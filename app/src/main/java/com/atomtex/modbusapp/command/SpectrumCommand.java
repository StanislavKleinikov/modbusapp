package com.atomtex.modbusapp.command;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbusapp.activity.SettingsActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.DeviceService;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteUtil;
import com.atomtex.modbusapp.util.CRC16;

import java.nio.ByteBuffer;
import java.util.Arrays;
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
public class SpectrumCommand implements Command {

    private static final int TIMEOUT = 1000;
    private static final int SERVICE_DATA_LENGTH = 6;
    private Modbus mModbus;
    private ModbusMessage mRequest;
    private LocalService mService;
    private ScheduledExecutorService mExecutor;
    private Bundle mBundle;
    private Intent mIntent;
    private int mMode;
    private boolean isActive;

    private SpectrumCommand() {

    }

    private static class SpecterCommandHolder {
        static final SpectrumCommand instance = new SpectrumCommand();
    }

    static SpectrumCommand getInstance() {
        return SpecterCommandHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service, int mode) {
        isActive = true;
        mModbus = modbus;
        mService = service;
        mMode = mode;
        mBundle = new Bundle();
        mIntent = new Intent();
        ByteBuffer buffer;
        SharedPreferences preferences = ((Context) mService).getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean bigEndian = preferences.getBoolean(SettingsActivity.PREFERENCES_CRC_ORDER, true);

        if (data != null) {
            buffer = ByteBuffer.allocate(3 + data.length).put(address).put(command).put((byte) data.length).put(data);
        } else {
            buffer = ByteBuffer.allocate(2).put(address).put(command);
        }
        byte[] messageBytes = CRC16.getMessageWithCRC16(buffer.array(), bigEndian);
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
                    mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match");
                } else if (mResponse.getBuffer()[1] == WRITE_CALIBRATION_DATA_SAMPLE
                        || mResponse.getBuffer()[1] == CHANGE_STATE_CONTROL_REGISTERS) {
                    mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mResponse.getBuffer()));
                } else {

                    //TODO need to implements this path
                    if (mResponse.getBuffer()[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED
                            || mResponse.getBuffer()[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT) {
                        getDecompressedSpectrum(mResponse.getBuffer(), 4, 1024);
                    } else {
                        if (getSpectrum(mResponse.getBuffer())) {
                            Log.i(TAG, "Time" + mModbus.getTimeAccumulatedSpectrum());
                        }
                    }

                    mBundle.putString(KEY_RESPONSE_TEXT, "The data has received " + mResponse.getBuffer().length + " bytes");

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

    private int[] getDecompressedSpectrum(byte[] bytes, int indexInBytes, int channel) {
        Log.i(TAG, "getSpecter");
        int[] spectrum = new int[channel];
        long val = 0;
        byte[] vv;
        long lv;

        try {
            for (int i = 0; i < channel; i++) {
                long valI;
                if (bytes[indexInBytes] == -128) {
                    vv = new byte[4];

                    vv[0] = bytes[indexInBytes + 2];
                    vv[1] = bytes[indexInBytes + 1];
                    vv[2] = 0;
                    vv[3] = 0;
                    lv = BitConverter.toInt32(vv, 0);
                    valI = val + lv;
                    indexInBytes += 3;
                } else if (bytes[indexInBytes] == 127) {
                    vv = new byte[4];
                    vv[0] = bytes[indexInBytes + 3];
                    vv[1] = bytes[indexInBytes + 2];
                    vv[2] = bytes[indexInBytes + 1];
                    vv[3] = 0;

                    lv = BitConverter.toInt32(vv, 0);
                    valI = val + lv;
                    indexInBytes += 4;
                } else {
                    valI = val + bytes[indexInBytes];
                    indexInBytes += 1;
                }
                spectrum[i] = (int) valI;
                val = valI;

            }

            Log.i(TAG, Arrays.toString(spectrum) + "\n" + spectrum.length);
            return spectrum;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method was designed for parsing response that contains spectrum data
     *
     * <p>
     *
     *
     * </p>
     *
     * @param bytes is the incoming array that represents valid response through the Modbus protocol
     * @return whether the spectrum data was parsed successfully
     */
    private boolean getSpectrum(byte[] bytes) {

        int[] spectrum;
        int timeAccumulatedSpectrum;
        try {
            int spectrumDataLength = BitConverter.toInt16(
                    new byte[]{bytes[3], bytes[2]}, 0) - SERVICE_DATA_LENGTH;

            timeAccumulatedSpectrum = BitConverter.toInt16(
                    new byte[]{bytes[4 + spectrumDataLength + 1], bytes[4 + spectrumDataLength]}, 0);

            spectrum = new int[spectrumDataLength / 3];

            byte[] tempArray = new byte[4];

            for (int i = 0; i < spectrum.length; i++) {
                for (int j = 4; j < spectrumDataLength; j += 3) {
                    tempArray[0] = bytes[j + 2];
                    tempArray[1] = bytes[j + 1];
                    tempArray[2] = bytes[j];
                    tempArray[3] = 0;
                    spectrum[i] = BitConverter.toInt32(tempArray, 0);
                }
            }
            mModbus.setSpectrum(spectrum);
            mModbus.setTimeAccumulatedSpectrum(timeAccumulatedSpectrum);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
