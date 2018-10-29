package com.atomtex.modbusapp.command;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbusapp.activity.MainActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.ByteUtil;
import com.atomtex.modbusapp.util.CRC16;

import java.nio.ByteBuffer;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;

/**
 * The implementation of the {@link Command} interface.
 */
public class SpecterCommand implements Command {

    private ModbusMessage mMessage;
    private LocalService mService;
    private Bundle mBundle;
    private Intent mIntent;

    private static class SpecterCommandHolder {
        static final SpecterCommand instance = new SpecterCommand();
    }

    static SpecterCommand getInstance() {
        return SpecterCommandHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service) {
        Log.i(MainActivity.TAG, "Execute");
        mService = service;
        mBundle = new Bundle();
        mIntent = new Intent();
        ByteBuffer buffer;

        if (data != null) {
            buffer = ByteBuffer.allocate(3 + data.length).put(address).put(command).put((byte) data.length).put(data);
        } else {
            buffer = ByteBuffer.allocate(2).put(address).put(command);
        }
        byte[] messageBytes = CRC16.getMessageWithCRC16(buffer.array());
        mMessage = new ModbusMessage(messageBytes);
        mBundle.putString(KEY_REQUEST_TEXT, ByteUtil.getHexString(messageBytes));
        if (modbus.sendMessage(mMessage)) {
            mMessage = modbus.receiveMessage();
        } else {
            mIntent.setAction(ACTION_DISCONNECT);
            mService.sendBroadcast(mIntent);
        }
        if (mMessage.getBuffer().length == 0) {
            mBundle.putString(KEY_RESPONSE_TEXT, "No response or timeout failed");
        } else if (mMessage.isException()) {
            mBundle.putByte(KEY_EXCEPTION, mMessage.getBuffer()[2]);
        } else {
            if (!mMessage.isIntegrity()) {
                mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match");
            } else {

                //TODO do something with the specter data
                mBundle.putString(KEY_RESPONSE_TEXT, "The Specter has received " + mMessage.getBuffer().length + " bytes");
            }
        }
        mService.getBoundedActivity().

                updateUI(mBundle);
    }

    @Override
    public void clear() {

    }

    @Override
    public void stop() {

    }
}