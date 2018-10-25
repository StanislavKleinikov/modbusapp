package com.atomtex.modbusapp.transport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import com.atomtex.modbusapp.activity.MainActivity;
import com.atomtex.modbusapp.util.ByteUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * The implementation of the {@link ModbusTransport}.
 * Implements methods for interaction with devices through the RFCOMM interface
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusRFCOMMTransport implements ModbusTransport, Closeable {

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private static final int TIMEOUT = 300;
    private static final int NUMBER_OF_BYTES_WITHOUT_DATA = 5;

    private ModbusRFCOMMTransport() {
    }

    private static class ModbusRFCOMMTransportHolder {
        private static final ModbusRFCOMMTransport instance = new ModbusRFCOMMTransport();
    }

    public static ModbusRFCOMMTransport getInstance(BluetoothDevice device) {
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

        byte[] buffer = new byte[0];
        int currentPosition = 0;
        long startTime = System.currentTimeMillis();
        int totalByte = NUMBER_OF_BYTES_WITHOUT_DATA;

        while (System.currentTimeMillis() - startTime < TIMEOUT) {

            if (buffer.length == totalByte) {
                return buffer;
            }
            try {
                if (inputStream.available() > 0) {
                    buffer = Arrays.copyOf(buffer, buffer.length + 1);
                    int x = inputStream.read();
                    buffer[currentPosition] = (byte) x;
                    currentPosition++;
                    if (buffer.length == 3) {
                        totalByte = buffer[2] + NUMBER_OF_BYTES_WITHOUT_DATA;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        //TODO remove this code after the test above one
       /* while ((System.currentTimeMillis() - startTime) < 150) {
            try {
                int bytesAvailable = inputStream.available();
                if (bytesAvailable > 0) {

                    byte[] packetBytes = new byte[bytesAvailable];
                    buffer = Arrays.copyOf(buffer, buffer.length + packetBytes.length);
                    inputStream.read(packetBytes);
                    for (int i = 0; i < bytesAvailable; i++, currentPosition++) {
                        byte b = packetBytes[i];
                        buffer[currentPosition] = b;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }*/
        return buffer;
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
