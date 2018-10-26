package com.atomtex.modbusapp.transport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import com.atomtex.modbusapp.activity.MainActivity;
import com.atomtex.modbusapp.util.BT_DU3Constant;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import static com.atomtex.modbusapp.util.BT_DU3Constant.DIAGNOSTICS;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_ACCUMULATED_SPECTR;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_STATUS_WORD;

/**
 * The implementation of the {@link ModbusTransport}.
 * Implements methods for interaction with devices through the RFCOMM interface
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusRFCOMMTransport implements ModbusTransport, Closeable {

    private static final int TIMEOUT = 300;
    private static final int DEFAULT_MESSAGE_LENGTH = 5;
    private static final int COMMAND_40_MESSAGE = 6;
    private static final int DIAGNOSTICS_MESSAGE = 8;

    private final byte[] buffer;

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;


    private ModbusRFCOMMTransport() {
        buffer = new byte[255];
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

        while ((System.currentTimeMillis() - startTime < TIMEOUT) & currentPosition != totalByte) {

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

                            //TODO need to test this code
                        } else if (buffer[1] == READ_ACCUMULATED_SPECTR) {
                            buffer = new byte[3082];
                            buffer[0] = this.buffer[0];
                            buffer[1] = this.buffer[1];
                            buffer[2] = this.buffer[2];
                            totalByte = (BitConverter.toInt16(new byte[]{buffer[3],buffer[2]},0))
                                    + COMMAND_40_MESSAGE;

                        } else {
                            totalByte = (buffer[2] & 255) + DEFAULT_MESSAGE_LENGTH;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return Arrays.copyOf(buffer, totalByte);


        //TODO remove this code after the test above one
       /* while (System.currentTimeMillis() - startTime < TIMEOUT) {

            try {
                if (inputStream.available() > 0) {
                    int x = inputStream.read();
                    buffer[currentPosition] = (byte) x;
                    currentPosition++;
                    if (currentPosition == 3) {
                        if ((buffer[1] >= (byte) 0x80)) {
                            totalByte = buffer[2] + NUMBER_OF_BYTES_WITHOUT_DATA;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (totalByte == currentPosition) {
                return Arrays.copyOf(buffer, currentPosition);
            }
        }
        return Arrays.copyOf(buffer, currentPosition);*/


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
