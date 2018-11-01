package com.atomtex.modbusapp.service;

import android.content.Intent;

import com.atomtex.modbusapp.activity.Callback;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public interface LocalService {

    Callback getBoundedActivity();

    void registerClient(Callback activity);

    void start(byte address, byte commandByte, byte[] commandData,int mode);

    void stop();

    void onDestroy();

    void sendBroadcast(Intent intent);

}
