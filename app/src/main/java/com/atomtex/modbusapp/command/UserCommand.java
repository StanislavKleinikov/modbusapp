package com.atomtex.modbusapp.command;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbusapp.activity.SettingsActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.DeviceService;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.ByteUtil;
import com.atomtex.modbusapp.util.CRC16;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.activity.MainActivity.APP_PREFERENCES;
import static com.atomtex.modbusapp.activity.MainActivity.TAG;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_CONNECTION_ACTIVE;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_RECONNECT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_UNABLE_CONNECT;
import static com.atomtex.modbusapp.util.BT_DU3Constant.*;

/**
 * The implementation of the {@link Command} interface.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class UserCommand implements Command {
    private static final int TIMEOUT = 1000;
    private Modbus mModbus;
    private ModbusMessage mRequest;
    private LocalService mService;
    private ScheduledExecutorService mExecutor;
    private Bundle mBundle;
    private Intent mIntent;
    private int mMode;
    private boolean isActive;

    private UserCommand() {

    }

    private static class UserCommandHolder {
        static final UserCommand instance = new UserCommand();
    }

    static UserCommand getInstance() {
        return UserCommandHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service, int mode) {
        isActive = true;
        mModbus = modbus;
        mService = service;
        mMode = mode;
        mBundle = new Bundle();
        mIntent = new Intent();
        SharedPreferences mPreferences = ((Context) mService).getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean bigEndian = mPreferences.getBoolean(SettingsActivity.PREFERENCES_CRC_ORDER, true);

        byte[] messageBytes = CRC16.getMessageWithCRC16(data, bigEndian);
        mRequest = new ModbusMessage(messageBytes);
        mBundle.putString(KEY_REQUEST_TEXT, ByteUtil.getHexString(messageBytes));

        if (mode == DeviceService.MODE_AUTO) {
            executeAuto();
        } else {
            executeSingle();
        }
    }

    @Override
    public void clear() {
        isActive = false;
    }

    @Override
    public void stop() {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
    }

    private void executeSingle() {
        if (mModbus.sendMessage(mRequest)) {
            ModbusMessage mResponse = mModbus.receiveMessage();

            if (mResponse.getBuffer().length == 0) {
                mBundle.putString(KEY_RESPONSE_TEXT, "No response or timeout failed");
            } else if (mResponse.isException()) {
                mBundle.putByte(KEY_EXCEPTION, mResponse.getBuffer()[2]);
            } else {
                if (!mResponse.isIntegrity()) {
                    mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match\n" + ByteUtil.getHexString(mResponse.getBuffer()));
                } else if (mResponse.getBuffer()[1] == READ_ACCUMULATED_SPECTRUM
                        || mResponse.getBuffer()[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED
                        || mResponse.getBuffer()[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT
                        || mResponse.getBuffer()[1] == READ_SPECTRUM_ACCUMULATED_SAMPLE) {
                    mBundle.putString(KEY_RESPONSE_TEXT, "The data has received " + mResponse.getBuffer().length + " bytes");
                } else {
                    mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mResponse.getBuffer()));
                }
            }

            mService.getBoundedActivity().updateUI(mBundle);
        } else {
            mIntent.setAction(ACTION_DISCONNECT);
            mService.sendBroadcast(mIntent);
            stop();
            restartConnection();
        }
    }

    private void executeAuto() {
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(this::executeSingle, 0, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private void restartConnection() {
        Log.e(TAG, "Restart connection " + Thread.currentThread().getId());
        new Thread(() -> {
            boolean isConnected = false;
            while (!isConnected) {
                if (!isActive) {
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
            if (mMode == DeviceService.MODE_AUTO) {
                executeAuto();
            }
        }).start();
    }

}
