package com.atomtex.modbusapp.command;

import android.content.Intent;
import android.os.Bundle;

import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.ByteUtil;

import java.nio.ByteBuffer;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;

/**
 * The implementation of the {@link Command} interface.
 */
public class ReadState implements Command {

    private ModbusMessage mMessage;
    private LocalService mService;
    private Bundle mBundle;
    private Intent mIntent;

    private static class ReadStateHolder {
        static final ReadState instance = new ReadState();
    }

    public static ReadState getInstance() {
        return ReadStateHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service) {
        mService = service;
        mBundle = new Bundle();
        mIntent = new Intent();
        ByteBuffer buffer;
        if (data != null) {
            buffer = ByteBuffer.allocate(2 + data.length).put(address).put(command).put(data);
        } else {
            buffer = ByteBuffer.allocate(2).put(address).put(command);
        }
        byte[] messageBytes = ByteUtil.getMessageWithCRC16(buffer.array());
        mMessage = new ModbusMessage(messageBytes);
        mBundle.putString(KEY_REQUEST_TEXT, ByteUtil.getHexString(messageBytes));
        if (modbus.sendMessage(mMessage)) {
            mMessage = modbus.receiveMessage();
        } else {
            mIntent.setAction(ACTION_DISCONNECT);
            mService.sendBroadcast(mIntent);
        }
        if (mMessage.isException()) {
            mBundle.putByte(KEY_EXCEPTION, mMessage.getBuffer()[2]);
        } else {
            if (!mMessage.isIntegrity()) {
                mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match\n" + ByteUtil.getHexString(mMessage.getBuffer()));
            } else {
                mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mMessage.getBuffer()));
            }
        }
        mService.getBoundedActivity().updateUI(mBundle);
        mService.stop();
    }

    @Override
    public void clear() {

    }

    @Override
    public void stop() {

    }
}
