package com.atomtex.modbusapp.command;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbusapp.activity.DeviceActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.ByteUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_ERROR_NUMBER;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_MESSAGE_NUMBER;
import static com.atomtex.modbusapp.activity.MainActivity.TAG;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_CONNECTION_ACTIVE;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_RECONNECT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_UNABLE_CONNECT;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_STATUS_WORD;

/**
 * The implementation of the {@link Command} interface.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ReadStatusWordTestCommand implements Command {

    private static final int TIMEOUT = 100;
    private Modbus modbus;
    private ScheduledExecutorService executor;
    private LocalService service;
    private Bundle bundle;
    private ModbusMessage message;
    private Intent intent;
    private int messageNumber;
    private int errorNumber;
    private long time;


    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service) {
        this.modbus = modbus;
        this.service = service;
        ByteBuffer buffer = ByteBuffer.allocate(2).put(address).put(READ_STATUS_WORD);
        byte[] messageBytes = ByteUtil.getMessageWithCRC16(buffer.array());
        message = new ModbusMessage(messageBytes);
        bundle = new Bundle();
        intent = new Intent();
        start();
    }

    @Override
    public void clear() {
        messageNumber = 0;
        errorNumber = 0;
    }

    public void start() {
        time = System.currentTimeMillis();
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this::sendMessage, 0, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public Bundle getBundle() {
        bundle.putInt(KEY_MESSAGE_NUMBER, messageNumber);
        bundle.putInt(KEY_ERROR_NUMBER, errorNumber);
        return bundle;
    }

    private void sendMessage() {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - time) + Arrays.toString(message.getBuffer()));
        if (modbus.sendMessage(message)) {
            receiveMessage();
            messageNumber++;
            service.getBoundedActivity().updateUI(getBundle());
        } else {
            Log.e(TAG, "An error occurred while sending data");
            intent.setAction(ACTION_DISCONNECT);
            service.sendBroadcast(intent);
            stop();
            restartConnection();
        }
    }

    private void receiveMessage() {
        Log.i(TAG, "Start listening " + (System.currentTimeMillis() - time));
        ModbusMessage message = modbus.receiveMessage();
        if (message.getBuffer() == null) {
            Log.e(TAG, "Unable to read");
            errorNumber++;
            service.getBoundedActivity().updateUI(getBundle());
        } else if (!message.isIntegrity()) {
            Log.e(TAG, "Buffer" + Arrays.toString(message.getBuffer()));
            errorNumber++;
            service.getBoundedActivity().updateUI(getBundle());
        }
    }

    private void restartConnection() {
        Log.e(TAG, "Restart connection " + Thread.currentThread().getId());
        new Thread(() -> {
            boolean isConnected = false;
            while (!isConnected) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                modbus.disconnect();
                intent.setAction(ACTION_RECONNECT);
                service.sendBroadcast(intent);
                isConnected = modbus.connect();
                if (!isConnected) {
                    Log.w(TAG, "Unable to connect");
                    intent.setAction(ACTION_UNABLE_CONNECT);
                    service.sendBroadcast(intent);
                }
            }
            isConnected = false;
            while (!isConnected) {
                modbus.disconnect();
                isConnected = modbus.connect();
            }
            intent.setAction(ACTION_CONNECTION_ACTIVE);
            service.sendBroadcast(intent);
            start();
        }).start();
    }
}
