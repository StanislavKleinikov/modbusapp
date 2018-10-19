package com.atomtex.modbusapp.domain;

import com.atomtex.modbusapp.command.Command;
import com.atomtex.modbusapp.command.CommandChooser;
import com.atomtex.modbusapp.transport.ModbusTransport;
import com.atomtex.modbusapp.util.CRC16;

/**
 * The 'slave' implementation of the the {@link Modbus} device
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusSlave extends Modbus {

    private ModbusMessage requestMessage;

    public ModbusSlave(ModbusTransport transport) {
        super(transport);
    }

    public ModbusTransport getTransport() {
        return super.getTransport();
    }

    public void setTransport(ModbusTransport transport) {
        super.setTransport(transport);
    }

    public boolean connect() {
        return getTransport().connect();
    }

    @Override
    public boolean sendMessage(ModbusMessage message) {
        requestMessage = message;
        return getTransport().sendMessage(requestMessage.getBuffer());
    }

    @Override
    public Command getCommand(Byte commandId) {
        return CommandChooser.getCommand(commandId);
    }

    @Override
    public ModbusMessage receiveMessage() {
        ModbusMessage responseMessage = new ModbusMessage(getTransport().receiveMessage());

        if (checkException(requestMessage, responseMessage)) {
            responseMessage.setException(true);
        } else {
            responseMessage.setIntegrity(CRC16.checkCRC(responseMessage.getBuffer()));
        }
        return responseMessage;
    }

    @Override
    public void disconnect() {
        getTransport().close();
    }

    private boolean checkException(ModbusMessage requestMessage, ModbusMessage responseMessage) {
        return (responseMessage.getBuffer()[1] ^ requestMessage.getBuffer()[1]) == 0x80;
    }

}
