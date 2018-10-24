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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.service.DeviceService;
import com.atomtex.modbusapp.service.LocalService;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.atomtex.modbusapp.activity.MainActivity.TAG;
import static com.atomtex.modbusapp.util.BTD3Constant.READ_STATE_CONTROL_REGISTERS;
import static com.atomtex.modbusapp.util.BTD3Constant.READ_STATE_DATA_REGISTERS;
import static com.atomtex.modbusapp.util.BTD3Constant.READ_STATUS_BINARY_SIGNAL;
import static com.atomtex.modbusapp.util.BTD3Constant.READ_STATUS_WORD;
import static com.atomtex.modbusapp.util.BTD3Constant.READ_STATUS_WORD_TEST;

public class DeviceActivity extends FragmentActivity implements ServiceConnection, Callback {

    public static final String KEY_RESPONSE_TEXT = "responseText";
    public static final String KEY_REQUEST_TEXT = "request";
    public static final String KEY_SERVICE_INTENT = "serviceIntent";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_MESSAGE_NUMBER = "messageNumber";
    public static final String KEY_ERROR_NUMBER = "errorNumber";
    public static final String KEY_ACTIVATED = "activated";
    public static final String KEY_TOGGLE_CLICKABLE = "clickable";
    public static final String KEY_EXCEPTION = "exception";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_CONNECTION_STATUS = "connectionStatus";
    public static final String KEY_DIALOG_IS_SHOWING = "dialogIsShowing";
    public static final int STATUS_NONE = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISCONNECTED = 2;
    public static final int STATUS_RECONNECT = 3;
    public static final int STATUS_UNABLE_CONNECT = 4;

    public static final String FRAGMENT_READ_STATE = "read_state_fragment";
    public static final String FRAGMENT_TEST = "test_fragment";

    @BindView(R.id.device_name)
    TextView mDeviceName;
    @BindView(R.id.spinner)
    Spinner mSpinner;
    @BindView(R.id.change_device)
    Button mChangeDeviceButton;
    private Fragment mFragment;
    private ProgressDialog mDialog;
    private Intent mServiceIntent;
    private BluetoothDevice mDevice;
    private ServiceConnection mConnection;
    private LocalService mService;
    private BroadcastReceiver mReceiver;
    private FragmentManager mFragmentManager;
    private boolean dialogIsShowing = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            mServiceIntent = savedInstanceState.getParcelable(KEY_SERVICE_INTENT);
            mDevice = savedInstanceState.getParcelable(KEY_DEVICE);
            dialogIsShowing = savedInstanceState.getBoolean(KEY_DIALOG_IS_SHOWING);
        } else {
            mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        }

        init();
    }


    private void init() {

        mDialog = new ProgressDialog(DeviceActivity.this);
        mDialog.setTitle("Connecting to device");
        mDialog.setMessage("Please wait..");
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);

        if (dialogIsShowing) {
            mDialog.show();
        }

        mChangeDeviceButton.setOnClickListener(v ->
                cancel(getString(R.string.toast_change_device)));


        ArrayAdapter<?> adapter =
                ArrayAdapter.createFromResource(this, R.array.command, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(adapter);

        mFragmentManager = getSupportFragmentManager();

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1: {
                        setFragment(FRAGMENT_READ_STATE, READ_STATUS_BINARY_SIGNAL);
                        break;
                    }
                    case 2: {
                        setFragment(FRAGMENT_READ_STATE, READ_STATE_CONTROL_REGISTERS);
                        break;
                    }
                    case 3: {
                        setFragment(FRAGMENT_READ_STATE, READ_STATE_DATA_REGISTERS);
                        break;
                    }
                    case 6: {
                        setFragment(FRAGMENT_READ_STATE, READ_STATUS_WORD);
                        break;
                    }
                    case 8: {
                        setFragment(FRAGMENT_TEST, READ_STATUS_WORD_TEST);
                        break;
                    }
                    default: {
                        if (mFragment != null) {
                            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                            fragmentTransaction.remove(mFragment);
                            fragmentTransaction.commit();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mDeviceName.setText(mDevice.getName());
        mConnection = this;

        IntentFilter filter = new IntentFilter(DeviceService.ACTION_UNABLE_CONNECT);
        filter.addAction(DeviceService.ACTION_CONNECTION_ACTIVE);
        filter.addAction(DeviceService.ACTION_RECONNECT);
        filter.addAction(DeviceService.ACTION_DISCONNECT);
        filter.addAction(DeviceService.ACTION_CANCEL);
        mReceiver = new DeviceActivity.ConnectionBroadCastReceiver();

        registerReceiver(mReceiver, filter);

        if (mServiceIntent == null)

        {
            mServiceIntent = new Intent(this, DeviceService.class);
            mServiceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            startService(mServiceIntent);
        }

        bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!

    }

    private void setFragment(String fragmentType, byte readStatusBinarySignal) {
        mService.stop();
        switch (fragmentType) {
            case FRAGMENT_READ_STATE:
                mFragment = ReadStateFragment.newInstance(readStatusBinarySignal);
                ((ServiceFragment) mFragment).boundService(mService);
                break;
            case FRAGMENT_TEST:
                mFragment = new ReadStatusWordFragment();
                ((ServiceFragment) mFragment).boundService(mService);
        }

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, mFragment, fragmentType);
        fragmentTransaction.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SERVICE_INTENT, mServiceIntent);
        outState.putParcelable(KEY_DEVICE, mDevice);
        outState.putBoolean(KEY_DIALOG_IS_SHOWING, dialogIsShowing);
    }

    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void cancel(String message) {
        Log.e(TAG, "cancel");
        setResult(RESULT_CANCELED);
        stopService(mServiceIntent);
        getIntent().putExtra(MainActivity.EXTRA_MESSAGE, message);
        finish();
    }

    @Override
    public void onBackPressed() {
        mService.onDestroy();
        setResult(RESULT_OK);
        super.onBackPressed();
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
        mService.registerClient(DeviceActivity.this); //Activity register in the service as client for callbacks!
        if (mFragment != null) {
            ((ServiceFragment) mFragment).boundService(mService);
        }
        Log.i(TAG, "Service connected ");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "service disconnected ");
    }

    @Override
    public void updateUI(Bundle bundle) {
        runOnUiThread(() -> {
            if (mFragment != null) {
                ((Callback) mFragment).updateUI(bundle);
            }
        });
    }

    private class ConnectionBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DeviceService.ACTION_CONNECTION_ACTIVE.equals(action)) {
                makeToast(getString(R.string.toast_connection_active));
                if (mDialog.isShowing()) {
                    mDialog.cancel();
                    dialogIsShowing = false;
                }
                Bundle bundle = new Bundle();
                bundle.putInt(KEY_CONNECTION_STATUS, STATUS_ACTIVE);
                updateUI(bundle);
                Log.i(TAG, "connected ");
            } else if (DeviceService.ACTION_UNABLE_CONNECT.equals(action)) {
                makeToast(getString(R.string.toast_connection_failed));
                Bundle bundle = new Bundle();
                bundle.putInt(KEY_CONNECTION_STATUS, STATUS_UNABLE_CONNECT);
                updateUI(bundle);
                Log.i(TAG, "unable connect ");
            } else if (DeviceService.ACTION_RECONNECT.equals(action)) {
                makeToast(getString(R.string.toast_reconnection));
                Bundle bundle = new Bundle();
                bundle.putInt(KEY_CONNECTION_STATUS, STATUS_RECONNECT);
                updateUI(bundle);
                Log.i(TAG, "Service reconnect ");
            } else if (DeviceService.ACTION_DISCONNECT.equals(action)) {
                makeToast(getString(R.string.toast_connection_failed));
                Bundle bundle = new Bundle();
                bundle.putInt(KEY_CONNECTION_STATUS, STATUS_DISCONNECTED);
                updateUI(bundle);
                Log.i(TAG, "Service disconnected ");
            } else if (DeviceService.ACTION_CANCEL.equals(action)) {
                cancel(getString(R.string.status_unable_connect));
            }
        }
    }
}