package com.atomtex.modbusapp.transport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import com.atomtex.modbusapp.util.BitConverter;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import static com.atomtex.modbusapp.util.BT_DU3Constant.DIAGNOSTICS;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_ACCUMULATED_SPECTER;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_STATUS_WORD;

/**
 * The implementation of the {@link ModbusTransport}.
 * Implements methods for interaction with devices through the RFCOMM interface
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusRFCOMMTransport implements ModbusTransport, Closeable {

    private static final int TIMEOUT_DEFAULT = 300;
    private static final int TIMEOUT_READ_SPECTER = 600;
    private static final int DEFAULT_MESSAGE_LENGTH = 5;
    private static final int COMMAND_40_MESSAGE = 6;
    private static final int DIAGNOSTICS_MESSAGE = 8;

    private final byte[] buffer;
    private static int responseTimeout = 300;

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private ModbusRFCOMMTransport() {
        buffer = new byte[3084];
    }

    private static class ModbusRFCOMMTransportHolder {
        private static final ModbusRFCOMMTransport instance = new ModbusRFCOMMTransport();
    }

    static ModbusRFCOMMTransport getInstance(BluetoothDevice device) {
        ModbusRFCOMMTransport instance = ModbusRFCOMMTransportHolder.instance;
        instance.device = device;
        return instance;
    }

    @Override
    public boolean connect() {
        try {
            ParcelUuid[] idArray = device.getUuids();
            UUID uuid = UUID.fromString(idArray[0].toString());
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean sendMessage(byte[] message) {

        if (message[1] == READ_ACCUMULATED_SPECTER || message[1] == READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT) {
            responseTimeout = TIMEOUT_READ_SPECTER;
        } else {
            responseTimeout = TIMEOUT_DEFAULT;
        }

        try {
            outputStream.write(message);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public byte[] receiveMessage() {

        byte[] buffer = this.buffer;
        int currentPosition = 0;
        long startTime = System.currentTimeMillis();
        int totalByte = DEFAULT_MESSAGE_LENGTH;

        while ((System.currentTimeMillis() - startTime < responseTimeout) & currentPosition != totalByte) {

            try {
                if (inputStream.available() > 0) {
                    int x = inputStream.read();
                    buffer[currentPosition] = (byte) x;
                    currentPosition++;

                    if (currentPosition == 3) {
                        if (buffer[1] == DIAGNOSTICS) {
                            totalByte = DIAGNOSTICS_MESSAGE;
                        } else if (buffer[1] == READ_STATUS_WORD) {
                            totalByte = DEFAULT_MESSAGE_LENGTH;
                        } else {
                            totalByte = (buffer[2] & 255) + DEFAULT_MESSAGE_LENGTH;
                        }
                    } else if (currentPosition == 4 && (buffer[1] == READ_ACCUMULATED_SPECTER
                            || buffer[1] == READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT)) {
                        int lengthData = BitConverter.toInt16(new byte[]{buffer[3], buffer[2]}, 0);
                        totalByte = lengthData + COMMAND_40_MESSAGE;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return Arrays.copyOf(buffer, totalByte);
    }

    @Override
    public boolean isConnected() {
        if (socket != null) {
            return socket.isConnected();
        } else
            return false;
    }


    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
        if (outputStream != null) {
            try {
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

}
