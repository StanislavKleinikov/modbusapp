package com.atomtex.modbusapp.domain;

import android.util.Log;

import com.atomtex.modbusapp.activity.MainActivity;
import com.atomtex.modbusapp.command.Command;
import com.atomtex.modbusapp.command.CommandChooser;
import com.atomtex.modbusapp.transport.ModbusTransport;
import com.atomtex.modbusapp.util.CRC16;

import java.util.Arrays;

/**
 * The 'slave' implementation of the the {@link Modbus} device
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusSlave extends Modbus {

    private ModbusMessage requestMessage;

    private ModbusSlave() {
    }

    private static class ModbusSlaveHolder {
        static final ModbusSlave instance = new ModbusSlave();
    }

    public static ModbusSlave getInstance() {
        return ModbusSlaveHolder.instance;
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
        return getTransport().sendMessage(message.getBuffer());
    }

    @Override
    public Command getCommand(Byte commandId) {
        return CommandChooser.getCommand(commandId);
    }

    @Override
    public ModbusMessage receiveMessage() {
        ModbusMessage responseMessage = new ModbusMessage(getTransport().receiveMessage());
        Log.e(MainActivity.TAG, "\nRequest " + Arrays.toString(requestMessage.getBuffer()) + "\n"
                + "Response " + Arrays.toString(responseMessage.getBuffer()));
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
        try {
            int x = responseMessage.getBuffer()[1] & 255;
            int y = requestMessage.getBuffer()[1] & 255;
            return (x ^ y) == 0x80;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

}
