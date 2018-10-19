package com.atomtex.modbusapp.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.service.DeviceService;
import com.atomtex.modbusapp.service.LocalService;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.atomtex.modbusapp.activity.MainActivity.TAG;

/**
 * @author kleinikov.stanislav@gmail.com
 */
public class DeviceCommunicateActivity extends AppCompatActivity implements Callback, ServiceConnection {

    public static final String KEY_RESPONSE_TEXT = "responseText";
    public static final String KEY_MESSAGE_NUMBER = "messageNumber";
    public static final String KEY_ERROR_NUMBER = "errorNumber";
    public static final String KEY_ACTIVATED = "activated";
    public static final String KEY_SERVICE_INTENT = "serviceIntent";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_TOGGLE_CLICKABLE = "clickable";

    @BindView(R.id.device_name)
    TextView deviceNameText;
    @BindView(R.id.response_text)
    TextView responseText;
    @BindView(R.id.message_number)
    TextView messageNumberView;
    @BindView(R.id.error_number)
    TextView errorNumberView;
    @BindView(R.id.change_device)
    Button changeDeviceButton;
    @BindView(R.id.toggle_button)
    ToggleButton toggleButton;

    private BluetoothDevice mDevice;
    private BroadcastReceiver mReceiver;

    private LocalService mService;
    private Intent mServiceIntent;
    private ServiceConnection mConnection;
    private ProgressDialog mDialog;

    private int mMessageNumber;
    private int mErrorNumber;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);
        ButterKnife.bind(this);

        mDialog = new ProgressDialog(DeviceCommunicateActivity.this);
        mDialog.setTitle("Connecting to device");
        mDialog.setMessage("Please wait..");
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);

        changeDeviceButton.setOnClickListener(v ->
                cancel(getString(R.string.toast_change_device)));
        toggleButton.setOnClickListener((v) -> {
            if (toggleButton.isChecked()) {
                mService.start();
            } else {
                mService.stop();
            }
        });
        if (savedInstanceState != null) {
            responseText.setText(savedInstanceState.getCharSequence(KEY_RESPONSE_TEXT));
            mMessageNumber = savedInstanceState.getInt(KEY_MESSAGE_NUMBER);
            mErrorNumber = savedInstanceState.getInt(KEY_ERROR_NUMBER);
            toggleButton.setChecked(savedInstanceState.getBoolean(KEY_ACTIVATED));
            toggleButton.setClickable(savedInstanceState.getBoolean(KEY_TOGGLE_CLICKABLE));
            mServiceIntent = savedInstanceState.getParcelable(KEY_SERVICE_INTENT);
            mDevice = savedInstanceState.getParcelable(KEY_DEVICE);
        } else {
            mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            toggleButton.setChecked(getIntent().getBooleanExtra(KEY_ACTIVATED, false));
            mMessageNumber = getIntent().getIntExtra(KEY_MESSAGE_NUMBER, 0);
            mErrorNumber = getIntent().getIntExtra(KEY_ERROR_NUMBER, 0);
        }

        messageNumberView.setText(String.valueOf(mMessageNumber));
        errorNumberView.setText(String.valueOf(mErrorNumber));
        deviceNameText.setText(mDevice.getName());
        mConnection = this;
        IntentFilter filter = new IntentFilter(DeviceService.ACTION_UNABLE_CONNECT);
        filter.addAction(DeviceService.ACTION_CONNECTION_ACTIVE);
        filter.addAction(DeviceService.ACTION_RECONNECT);
        filter.addAction(DeviceService.ACTION_DISCONNECT);
        filter.addAction(DeviceService.ACTION_CANCEL);
        mReceiver = new ConnectionBroadCastReceiver();
        registerReceiver(mReceiver, filter);

        if (mServiceIntent == null) {
            mDialog.show();
            mServiceIntent = new Intent(this, DeviceService.class);
            mServiceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            startService(mServiceIntent);
        }
        bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_RESPONSE_TEXT, responseText.getText());
        outState.putInt(KEY_MESSAGE_NUMBER, mMessageNumber);
        outState.putInt(KEY_ERROR_NUMBER, mErrorNumber);
        outState.putBoolean(KEY_ACTIVATED, toggleButton.isChecked());
        outState.putBoolean(KEY_TOGGLE_CLICKABLE, toggleButton.isClickable());
        outState.putParcelable(KEY_SERVICE_INTENT, mServiceIntent);
        outState.putParcelable(KEY_DEVICE, mDevice);
    }

    private void cancel(String message) {
        Log.e(TAG, "cancel");
        setResult(RESULT_CANCELED);
        stopService(mServiceIntent);
        getIntent().putExtra(MainActivity.EXTRA_MESSAGE, message);
        finish();
    }

    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        mService.onDestroy();
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    @Override
    public void updateUI(Bundle bundle) {
        runOnUiThread(() -> {
            mMessageNumber = bundle.getInt(KEY_MESSAGE_NUMBER);
            mErrorNumber = bundle.getInt(KEY_ERROR_NUMBER);
            messageNumberView.setText(String.valueOf(mMessageNumber));
            errorNumberView.setText(String.valueOf(mErrorNumber));
        });
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        DeviceService.LocalBinder binder = (DeviceService.LocalBinder) service;
        mService = binder.getServiceInstance(); //Get instance of your service!
        mService.registerClient(DeviceCommunicateActivity.this); //Activity register in the service as client for callbacks!
        Log.i(TAG, "Service connected ");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "service disconnected ");
    }

    private class ConnectionBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DeviceService.ACTION_CONNECTION_ACTIVE.equals(action)) {
                responseText.setText(getString(R.string.status_connected));
                makeToast(getString(R.string.toast_connection_active));
                toggleButton.setClickable(true);
                if (mDialog.isShowing()) {
                    mDialog.cancel();
                }
            } else if (DeviceService.ACTION_UNABLE_CONNECT.equals(action)) {
                responseText.setText(getString(R.string.status_unable_connect));
                makeToast(getString(R.string.toast_connection_failed));
                toggleButton.setClickable(false);
            } else if (DeviceService.ACTION_RECONNECT.equals(action)) {
                responseText.setText(getString(R.string.status_reconnect));
                makeToast(getString(R.string.toast_reconnection));
                toggleButton.setClickable(false);
            } else if (DeviceService.ACTION_DISCONNECT.equals(action)) {
                responseText.setText(getString(R.string.status_disconnect));
                makeToast(getString(R.string.toast_connection_failed));
                toggleButton.setClickable(false);
            } else if (DeviceService.ACTION_CANCEL.equals(action)) {
                cancel(getString(R.string.status_unable_connect));
            }
        }
    }
}
