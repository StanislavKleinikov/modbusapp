package com.atomtex.modbusapp.service;

import android.content.Intent;

import com.atomtex.modbusapp.activity.Callback;

public interface LocalService {

     Callback getBoundedActivity();

     void registerClient(Callback activity);

     void start(byte[] commandData);

     void stop();

     void onDestroy();

     void sendBroadcast(Intent intent);

}
