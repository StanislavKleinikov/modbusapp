package com.atomtex.modbusapp.command;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbusapp.activity.SettingsActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.CRC16;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_ERROR_NUMBER;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_MESSAGE_NUMBER;
import static com.atomtex.modbusapp.activity.MainActivity.APP_PREFERENCES;
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
    private Modbus mModbus;
    private ScheduledExecutorService executor;
    private LocalService mService;
    private Bundle mBundle;
    private ModbusMessage mMessage;
    private Intent mIntent;
    private int messageNumber;
    private int errorNumber;
    private long time;
    private boolean isActive;

    private ReadStatusWordTestCommand() {

    }

    private static class ReadStatusWordTestHolder {
        static final ReadStatusWordTestCommand instance = new ReadStatusWordTestCommand();
    }

    static ReadStatusWordTestCommand getInstance() {
        return ReadStatusWordTestHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service, int mode) {
        isActive = true;
        mModbus = modbus;
        mService = service;
        SharedPreferences preferences = ((Context) mService).getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean bigEndian = preferences.getBoolean(SettingsActivity.PREFERENCES_CRC_ORDER, true);

        ByteBuffer buffer = ByteBuffer.allocate(2).put(address).put(READ_STATUS_WORD);
        byte[] messageBytes = CRC16.getMessageWithCRC16(buffer.array(), bigEndian);
        mMessage = new ModbusMessage(messageBytes);
        mBundle = new Bundle();
        mIntent = new Intent();
        start();
    }

    @Override
    public void clear() {
        isActive = false;
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

    private Bundle getBundle() {
        mBundle.putInt(KEY_MESSAGE_NUMBER, messageNumber);
        mBundle.putInt(KEY_ERROR_NUMBER, errorNumber);
        return mBundle;
    }

    private void sendMessage() {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - time) + Arrays.toString(mMessage.getBuffer()));
        if (mModbus.sendMessage(mMessage)) {
            receiveMessage();
            messageNumber++;
            mService.getBoundedActivity().updateUI(getBundle());
        } else {
            Log.e(TAG, "An error occurred while sending data");
            mIntent.setAction(ACTION_DISCONNECT);
            mService.sendBroadcast(mIntent);
            stop();
            restartConnection();
        }
    }

    private void receiveMessage() {
        Log.i(TAG, "Start listening " + (System.currentTimeMillis() - time));
        ModbusMessage message = mModbus.receiveMessage();
        if (message.getBuffer() == null) {
            Log.e(TAG, "Unable to read");
            errorNumber++;
            mService.getBoundedActivity().updateUI(getBundle());
        } else if (!message.isIntegrity()) {
            Log.e(TAG, "Buffer" + Arrays.toString(message.getBuffer()));
            errorNumber++;
            mService.getBoundedActivity().updateUI(getBundle());
        }
    }

    private void restartConnection() {
        Log.e(TAG, "Restart connection " + Thread.currentThread().getId());
        new Thread(() -> {
            boolean isConnected = false;
            while (!isConnected) {
                if (!isActive){
                    return;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mModbus.disconnect();
                mIntent.setAction(ACTION_RECONNECT);
                mService.sendBroadcast(mIntent);
                isConnected = mModbus.connect();
                if (!isConnected) {
                    Log.w(TAG, "Unable to connect");
                    mIntent.setAction(ACTION_UNABLE_CONNECT);
                    mService.sendBroadcast(mIntent);
                }
            }
            isConnected = false;
            while (!isConnected) {
                mModbus.disconnect();
                isConnected = mModbus.connect();
            }
            mIntent.setAction(ACTION_CONNECTION_ACTIVE);
            mService.sendBroadcast(mIntent);
            start();
        }).start();
    }
}
