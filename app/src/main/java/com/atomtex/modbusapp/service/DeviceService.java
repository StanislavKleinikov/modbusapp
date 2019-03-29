package com.atomtex.modbusapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.activity.Callback;
import com.atomtex.modbusapp.activity.DeviceActivity;
import com.atomtex.modbusapp.command.Command;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusSlave;
import com.atomtex.modbusapp.transport.ModbusTransportFactory;

import java.util.Date;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_ACTIVATED;
import static com.atomtex.modbusapp.activity.MainActivity.TAG;

/**
 * This class is the Service for communication with A device through the Bluetooth.
 * Was designed by using the local service pattern to communicate with the activity
 * which it bounded.
 * Contains methods to create a {@link BluetoothSocket} and to make reconnect with
 * a device in case the signal loss
 *
 * @author kleinikov.stanislav@gmail.com
 */
public class DeviceService extends Service implements LocalService {

    public static final String ACTION_UNABLE_CONNECT = "unableToConnect";
    public static final String ACTION_CONNECTION_ACTIVE = "connectionIsActive";
    public static final String ACTION_RECONNECT = "actionReconnect";
    public static final String ACTION_DISCONNECT = "actionDisconnect";
    public static final String ACTION_CANCEL = "actionCancel";
    public static final String ACTION_SPECTRUM_RECEIVED = "spectrumReceived";

    public static final int MODE_SINGLE_REQUEST = 1;
    public static final int MODE_AUTO = 2;


    private Callback mActivity;
    private BluetoothDevice mDevice;
    private Intent mIntent;
    private Modbus modbus;
    private Command command;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Start service");
        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mIntent = new Intent();
        new Thread(() -> {
            modbus = ModbusSlave.getInstance();
            modbus.setTransport(ModbusTransportFactory.getTransport(mDevice));
            if (!connect()) {
                mIntent.setAction(ACTION_CANCEL);
                sendBroadcast(mIntent);
            } else {
                Intent connectionActiveIntent = new Intent(ACTION_CONNECTION_ACTIVE);
                sendBroadcast(connectionActiveIntent);
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DeviceService.LocalBinder();
    }

    public class LocalBinder extends Binder {
        public DeviceService getServiceInstance() {
            return DeviceService.this;
        }
    }

    @Override
    public void registerClient(Callback activity) {
        this.mActivity = activity;
    }

    public boolean connect() {
        if (modbus.connect()) {
            return true;
        } else {
            Log.w(TAG, "Unable to connect");
            mIntent.setAction(ACTION_UNABLE_CONNECT);
            sendBroadcast(mIntent);
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public void start(byte address, byte commandByte, byte[] commandData, int mode) {
        Log.e(TAG, "Start");

        command = modbus.getCommand(commandByte);

        if (mode == MODE_AUTO) {
            Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
            intent.putExtra(KEY_ACTIVATED, true);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel =
                        new NotificationChannel("ID", "Notification", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(notificationChannel);
                builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId());
            } else {
                builder = new NotificationCompat.Builder(getApplicationContext());
            }

            builder.setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(mDevice.getName())
                    .setContentText("Executing...")
                    .setOngoing(true)
                    .setWhen(new Date().getTime())
                    .setUsesChronometer(true)
                    .setContentIntent(resultPendingIntent);

            Notification notification = builder.build();
            startForeground(1, notification);
        }
        if (command != null) {
            command.execute(modbus, address, commandByte, commandData, this, mode);
        } else {
            Log.e(TAG, "The command is not found");
        }
    }

    public void stop() {
        Log.e(TAG, "Stop");
        stopForeground(true);
        if (command != null) {
            command.stop();
        }
    }

    public Callback getBoundedActivity() {
        return mActivity;
    }


    @Override
    public void onDestroy() {
        stop();
        if (command != null) {
            command.clear();
        }
        modbus.disconnect();
        Log.i(TAG, "Destroy service");
        super.onDestroy();
    }
}
