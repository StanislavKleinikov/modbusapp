package com.atomtex.modbusapp.util;

import java.nio.ByteBuffer;

/**
 * Utility class for doing various operation with bytes.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ByteUtil {

    /**
     * Makes a hex string from byte which given by adding additional "0" if it has just a one digit.
     *
     * @param number byte to make a hex string
     * @return completed string
     */
    public static String getHexString(byte number) {
        String x = Integer.toHexString(number & 255);
        if (x.length() < 2) {
            x = "0" + x;
        }
        return x;
    }

    public static String getHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte singleByte : bytes) {
            builder.append(getHexString(singleByte)).append(" ");
        }
        builder.trimToSize();
        return builder.toString();
    }

    public static byte[] getMessageWithCRC16(byte[] bytes) {
        short crc = (short) CRC16.calcCRC(bytes);
        crc = ByteSwapper.swap(crc);
        byte[] bytesCRC = BitConverter.getBytes(crc);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + bytesCRC.length);
        buffer.put(bytes);
        buffer.put(bytesCRC);
        return buffer.array();
    }
}
