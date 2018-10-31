package com.atomtex.modbusapp.command;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.atomtex.modbusapp.activity.MainActivity;
import com.atomtex.modbusapp.activity.SettingsActivity;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusMessage;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.ByteUtil;
import com.atomtex.modbusapp.util.CRC16;

import java.nio.ByteBuffer;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.activity.MainActivity.APP_PREFERENCES;
import static com.atomtex.modbusapp.service.DeviceService.ACTION_DISCONNECT;
import static com.atomtex.modbusapp.util.BT_DU3Constant.*;

/**
 * The implementation of the {@link Command} interface.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class SpecterCommand implements Command {

    private ModbusMessage mMessage;
    private LocalService mService;
    private Bundle mBundle;
    private Intent mIntent;
    private SharedPreferences preferences;

    private SpecterCommand() {

    }

    private static class SpecterCommandHolder {
        static final SpecterCommand instance = new SpecterCommand();
    }

    static SpecterCommand getInstance() {
        return SpecterCommandHolder.instance;
    }

    @Override
    public void execute(Modbus modbus, byte address, byte command, byte[] data, LocalService service) {
        mService = service;
        mBundle = new Bundle();
        mIntent = new Intent();
        ByteBuffer buffer;
        preferences = ((Context) mService).getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean bigEndian = preferences.getBoolean(SettingsActivity.PREFERENCES_CRC_ORDER, true);

        if (data != null) {
            buffer = ByteBuffer.allocate(3 + data.length).put(address).put(command).put((byte) data.length).put(data);
        } else {
            buffer = ByteBuffer.allocate(2).put(address).put(command);
        }
        byte[] messageBytes = CRC16.getMessageWithCRC16(buffer.array(), bigEndian);
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
            } else if (mMessage.getBuffer()[1] == WRITE_CALIBRATION_DATA_SAMPLE
                    || mMessage.getBuffer()[1] == CHANGE_STATE_CONTROL_REGISTERS) {
                mBundle.putString(KEY_RESPONSE_TEXT, ByteUtil.getHexString(mMessage.getBuffer()));
            } else {
                //TODO do something with the specter data
                mBundle.putString(KEY_RESPONSE_TEXT, "The data has received " + mMessage.getBuffer().length + " bytes");
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
