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
    private static final int[] SPECTRUM = new int[3072];

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
            byte command = mResponse.getBuffer()[1];

            if (mResponse.getBuffer().length == 0) {
                mBundle.putString(KEY_RESPONSE_TEXT, "No response or timeout failed");

            } else if (mResponse.isException()) {
                mBundle.putByte(KEY_EXCEPTION, mResponse.getBuffer()[2]);

            } else if (!mResponse.isIntegrity()) {
                mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match");

            } else if (command == WRITE_CALIBRATION_DATA_SAMPLE
                    || command == CHANGE_STATE_CONTROL_REGISTERS
                    || command == READ_SPECTRUM_ACCUMULATED_SAMPLE) {
                mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mResponse.getBuffer()));

            } else {

                boolean result;

                if (command == READ_ACCUMULATED_SPECTRUM_COMPRESSED
                        || command == READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT) {
                    result = getDecompressedSpectrum(mResponse.getBuffer());

                } else {
                    result = getSpectrum(mResponse.getBuffer());
                }

                if (result) {
                    Log.i(TAG, "Time " + mModbus.getTimeAccumulatedSpectrum()
                            + " Spectrum length " + mModbus.getSpectrum().length);
                    mBundle.putString(KEY_RESPONSE_TEXT, "The data has received " + mResponse.getBuffer().length + " bytes");
                } else {
                    mBundle.putString(KEY_RESPONSE_TEXT, "An error occurred while parsing response");
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

    /**
     * This method was designed for parsing response that contains spectrum and service data
     * in compressed form. Gets the source bytes array, then parses spectrum data and "live" spectrum
     * acquisition time and writes data into the {@link Modbus} class fields.
     * <p>
     * The method counts spectrum data length according the modbus response data where the 3 and 4
     * bytes are the length of spectrum data with service data.
     * <p>
     * The spectrum is encoded as follows. Each channel is allocated from one to four bytes.
     * If the value of the first byte is in the range from -127 to 126, then this byte is the only
     * one and contains the difference between the values of the current and previous channels.
     * If the value of the first byte is -128, the next two bytes contain the difference between
     * the values of the current and previous channels.
     * If the value of the first byte is 127, the next three bytes contain the absolute value
     * of the current channel.
     * Of the six bytes of overhead, the first 2 bytes contain the "live" spectrum acquisition time,
     * and the remaining 4 bytes contain the high – energy pulse counter.\
     * </p>
     *
     * @param bytes is the incoming array that represents valid response through the Modbus protocol
     *              that contains the spectrum and service data in compressed form
     * @return whether the spectrum data was parsed successfully
     * @see Modbus
     */
    private boolean getDecompressedSpectrum(byte[] bytes) {
        int startBytePosition = 4;
        int timeAccumulatedSpectrum;
        int previousValue = 0;
        byte[] tempArray;
        int difference;
        int counter = 0;

        try {

            int spectrumDataLength = BitConverter.toInt16(
                    new byte[]{bytes[3], bytes[2]}, 0) - SERVICE_DATA_LENGTH;

            timeAccumulatedSpectrum = BitConverter.toInt16(
                    new byte[]{bytes[startBytePosition + spectrumDataLength + 1],
                            bytes[startBytePosition + spectrumDataLength]}, 0);

            for (int i = startBytePosition; i < spectrumDataLength + startBytePosition; ) {

                int currentValue;

                if (bytes[i] == -128) {
                    tempArray = new byte[4];
                    tempArray[0] = bytes[i + 2];
                    tempArray[1] = bytes[i + 1];
                    tempArray[2] = 0;
                    tempArray[3] = 0;
                    difference = BitConverter.toInt32(tempArray, 0);
                    currentValue = previousValue + difference;
                    i += 3;
                } else if (bytes[i] == 127) {
                    tempArray = new byte[4];
                    tempArray[0] = bytes[i + 3];
                    tempArray[1] = bytes[i + 2];
                    tempArray[2] = bytes[i + 1];
                    tempArray[3] = 0;

                    difference = BitConverter.toInt32(tempArray, 0);
                    currentValue = previousValue + difference;
                    i += 4;
                } else {
                    currentValue = previousValue + bytes[i];
                    i += 1;
                }
                SPECTRUM[counter] = currentValue;
                previousValue = currentValue;
                counter++;
            }

            mModbus.setSpectrum(Arrays.copyOf(SPECTRUM, counter));
            mModbus.setTimeAccumulatedSpectrum(timeAccumulatedSpectrum);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * This method was designed for parsing response that contains spectrum and service data
     * Gets the source bytes array, then parses spectrum data and "live" spectrum
     * acquisition time and writes data into the {@link Modbus} class fields.
     *
     * <p>
     * The method counts spectrum data length according the modbus response data where the 3 and 4
     * bytes are the length of spectrum data with service data if length consist of two bytes or
     * 3 byte if one.
     * <p>
     * The entire spectrum is 3078 bytes in size. Of these, 3072 bytes - the actual spectrum
     * and 6 bytes-service data. The first 2 bytes of service data contain the "live" spectrum
     * acquisition time, and the remaining 4 bytes contain the high – energy pulse counter.
     * </p>
     *
     * @param bytes is the incoming array that represents valid response through the Modbus protocol
     *              that contains the spectrum and service data
     * @return whether the spectrum data was parsed successfully
     */
    private boolean getSpectrum(byte[] bytes) {
        int timeAccumulatedSpectrum;
        int spectrumDataLength;
        int startBytePosition;
        int counter = 0;
        try {
            if (bytes[1] == READ_SPECTRUM_ACCUMULATED_SAMPLE) {
                spectrumDataLength = bytes[2];
                startBytePosition = 3;
            } else {
                spectrumDataLength = BitConverter.toInt16(
                        new byte[]{bytes[3], bytes[2]}, 0) - SERVICE_DATA_LENGTH;
                startBytePosition = 4;
            }

            timeAccumulatedSpectrum = BitConverter.toInt16(
                    new byte[]{bytes[startBytePosition + spectrumDataLength + 1],
                            bytes[startBytePosition + spectrumDataLength]}, 0);

            byte[] tempArray = new byte[4];

            for (int i = startBytePosition; i < spectrumDataLength + startBytePosition; i += 3) {
                tempArray[0] = bytes[i + 2];
                tempArray[1] = bytes[i + 1];
                tempArray[2] = bytes[i];
                tempArray[3] = 0;
                SPECTRUM[counter] = BitConverter.toInt32(tempArray, 0);
                counter++;
            }
            mModbus.setSpectrum(Arrays.copyOf(SPECTRUM, counter));
            mModbus.setTimeAccumulatedSpectrum(timeAccumulatedSpectrum);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
