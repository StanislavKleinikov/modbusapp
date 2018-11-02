package com.atomtex.modbusapp.transport;

/**
 * This interface represents basic operation for interaction with devices
 * through the various interface.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public interface ModbusTransport {

    boolean sendMessage(byte[] message);

    byte[] receiveMessage();

    boolean connect();

    void close();

}
