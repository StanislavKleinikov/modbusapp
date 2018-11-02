package com.atomtex.modbusapp.domain;

import com.atomtex.modbusapp.command.Command;
import com.atomtex.modbusapp.transport.ModbusTransport;

/**
 * This abstract class is the superclass of all classes representing
 * a device that uses the MODBUS protocol.
 *
 * @author stanislav.kleinikov@gmail.com
 * @see ModbusTransport
 * @see ModbusMessage
 * @see Command
 */
public abstract class Modbus {

    private ModbusTransport transport;

    private int[] spectrum;

    private int timeAccumulatedSpectrum;

    Modbus(ModbusTransport transport) {
        this.transport = transport;
    }

    public ModbusTransport getTransport() {
        return transport;
    }

    public void setTransport(ModbusTransport transport) {
        this.transport = transport;
    }

    /**
     * Allows to establish a connection between two devices.
     * <p>
     * In order to create a connection between two devices, classes that extends
     * this method must implement the server-side or client-side mechanisms
     * because one device must open a server socket, and the other one must
     * initiate the connection using the server device's MAC address.
     * </p>
     *
     * @return whether the connection is successful
     */
    public abstract boolean connect();

    /**
     * Allows to send a message using the MODBUS protocol to remote device
     *
     * @param message the message to send to device
     * @return indicates whether the sending is successful
     */
    public abstract boolean sendMessage(ModbusMessage message);

    public abstract Command getCommand(Byte commandId);

    public abstract ModbusMessage receiveMessage();

    public void disconnect() {
        transport.close();
    }

    public int[] getSpectrum() {
        return spectrum;
    }

    public void setSpectrum(int[] spectrum) {
        this.spectrum = spectrum;
    }

    public int getTimeAccumulatedSpectrum() {
        return timeAccumulatedSpectrum;
    }

    public void setTimeAccumulatedSpectrum(int timeAccumulatedSpectrum) {
        this.timeAccumulatedSpectrum = timeAccumulatedSpectrum;
    }
}