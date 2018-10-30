package com.atomtex.modbusapp.command;

import android.content.Intent;
import android.os.Bundle;

import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BT_DU3Constant;
import com.atomtex.modbusapp.util.ByteUtil;
import com.atomtex.modbusapp.util.CRC16;

import java.nio.ByteBuffer;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;

public class UserCommand implements Command {

    private ModbusMessage mMessage;
    private LocalService mService;
    private Bundle mBundle;
    private Intent mIntent;


    private static class UserCommandHolder {
        static final UserCommand instance = new UserCommand();
    }

    static UserCommand getInstance() {
        return UserCommandHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service) {
        mService = service;
        mBundle = new Bundle();
        mIntent = new Intent();
        byte[] messageBytes = CRC16.getMessageWithCRC16(data);
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
                mBundle.putString(KEY_RESPONSE_TEXT, "CRC is not match\n" + ByteUtil.getHexString(mMessage.getBuffer()));
            } else if (mMessage.getBuffer()[1] == BT_DU3Constant.READ_ACCUMULATED_SPECTER
                    || mMessage.getBuffer()[1] == BT_DU3Constant.READ_ACCUMULATED_SPECTER_COMPRESSED
                    || mMessage.getBuffer()[1] == BT_DU3Constant.READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT
                    || mMessage.getBuffer()[1] == BT_DU3Constant.READ_SPECTER_ACCUMULATED_SAMPLE) {
                mBundle.putString(KEY_RESPONSE_TEXT, "The data has received " + mMessage.getBuffer().length + " bytes");
            } else {
                mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mMessage.getBuffer()));
            }
        }

        mService.getBoundedActivity().updateUI(mBundle);
    }

    @Override
    public void clear() {

    }

    @Override
    public void stop() {

    }
}
