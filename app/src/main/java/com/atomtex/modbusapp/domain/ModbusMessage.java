package com.atomtex.modbusapp.domain;

/**
 * The object to hold message and additional information about it.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusMessage {

    public static final byte[] MESSAGE_07 = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};

    /**
     * The byte array to be send.
     */
    private byte[] buffer;

    /**
     * The length of the buffer.
     */
    private int length;

    /**
     * Whether the message is not valid message. The CRC value do not match.
     */
    private boolean integrity;

    /**
     * Indicates whether the message contains an exception.
     */
    private boolean exception;

    public ModbusMessage() {

    }

    public ModbusMessage(byte[] buffer) {
        this.buffer = buffer;
        length = buffer.length;
        integrity = true;
        exception = false;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
        length = buffer.length;
    }

    public int getLength() {
        return length;
    }

    public boolean isIntegrity() {
        return integrity;
    }

    public void setIntegrity(boolean integrity) {
        this.integrity = integrity;
    }

    public boolean isException() {
        return exception;
    }

    public void setException(boolean exception) {
        this.exception = exception;
    }
}
