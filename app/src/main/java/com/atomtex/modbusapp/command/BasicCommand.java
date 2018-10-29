package com.atomtex.modbusapp.command;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbusapp.activity.MainActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BT_DU3Constant;
import com.atomtex.modbusapp.util.ByteUtil;

import java.nio.ByteBuffer;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_ACCUMULATED_SPECTER;
import static com.atomtex.modbusapp.util.BT_DU3Constant.READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT;

/**
 * The implementation of the {@link Command} interface.
 */
public class BasicCommand implements Command {

    private ModbusMessage mMessage;
    private LocalService mService;
    private Bundle mBundle;
    private Intent mIntent;

    private static class ReadStateCommandHolder {
        static final BasicCommand instance = new BasicCommand();
    }

    static BasicCommand getInstance() {
        return ReadStateCommandHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service) {
        Log.i(MainActivity.TAG, "Execute");
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
        if (mMessage.getBuffer().length == 0) {
            mBundle.putString(KEY_RESPONSE_TEXT, "No response or timeout failed");


            //TODO need to test
        } else if (mMessage.getBuffer().length >= 2 &&
                (mMessage.getBuffer()[1] == READ_ACCUMULATED_SPECTER || mMessage.getBuffer()[1] == READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT)) {
            StringBuilder builder = new StringBuilder();
            mBundle.putString(KEY_RESPONSE_TEXT, "The Specter has received " + mMessage.getBuffer().length + " bytes" + "\n"
                    + (mMessage.isIntegrity() ? "" : "CRC is not match"));
            for (int i = 0; i < mMessage.getBuffer().length; i++) {
                if (i % 80 == 0) {
                    builder.append("\n");
                }
                builder.append(mMessage.getBuffer()[i]).append(" ");
            }
            Log.i(MainActivity.TAG, builder.toString());


        } else if (mMessage.isException()) {
            mBundle.putByte(KEY_EXCEPTION, mMessage.getBuffer()[2]);
        } else {
            if (!mMessage.isIntegrity()) {
                mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match\n" + ByteUtil.getHexString(mMessage.getBuffer()));
            } else {
                mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mMessage.getBuffer()));
            }
        }
        mService.getBoundedActivity().

                updateUI(mBundle);
        mService.stop();
    }

    @Override
    public void clear() {

    }

    @Override
    public void stop() {

    }
}
