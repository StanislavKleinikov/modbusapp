package com.atomtex.modbusapp.service;

import android.app.Service;

import com.atomtex.modbusapp.activity.Callback;

public abstract class LocalService extends Service {

    public abstract Callback getBoundedActivity();

    public abstract void registerClient(Callback activity);

    public abstract void start();

    public abstract void stop();

}
