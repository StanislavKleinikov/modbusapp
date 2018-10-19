package com.atomtex.modbusapp.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.atomtex.modbusapp.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author kleinikov.stanislav@gmail.com
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "myTag";

    public static final String EXTRA_MESSAGE = "message";

    private static final String ACTION_CHANGE_DEVICE = "changeDevice";

    private static final String FILE_NAME = "BoundedDeviceInfo";

    private static final String KEY_PIN = "0000";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_NAME = "name";

    private static final int REQUEST_CODE_BLUETOOTH_ON = 1;
    private static final int REQUEST_CODE_DEVICE_COMMUNICATE = 2;

    @BindView(R.id.switch_button)
    Button switcherButton;
    @BindView(R.id.find_devices)
    Button searchButton;
    @BindView(R.id.list_devices)
    ListView listViewDevices;

    private static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private SimpleAdapter mSimpleAdapter;
    private BroadcastReceiver mBroadcastReceiver;
    private BluetoothDevice mDevice;

    private final static ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /*
            Reads from the file the address of a device which was connect
         */
        String macAddress = readDeviceInfo();
        if (!ACTION_CHANGE_DEVICE.equals(getIntent().getAction())
                && BluetoothAdapter.checkBluetoothAddress(macAddress) && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getBondedDevices().contains(mBluetoothAdapter.getRemoteDevice(macAddress))) {
            connect(mBluetoothAdapter.getRemoteDevice(macAddress));
        }

        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            Map map = (Map) mSimpleAdapter.getItem(position);
            String address = (String) map.get(KEY_ADDRESS);
            mDevice = mBluetoothAdapter.getRemoteDevice(address);
            map.put(KEY_ADDRESS, "connection...");
            mSimpleAdapter.notifyDataSetChanged();
            if (mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                mDevice.createBond();
            } else {
                connect(mDevice);
            }
        });

        switcherButton.setOnClickListener(v -> switchState());
        searchButton.setOnClickListener(v -> {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOn, REQUEST_CODE_BLUETOOTH_ON);
            } else {
                findDevices();
            }

        });

        mSimpleAdapter = new SimpleAdapter(this, arrayList, android.R.layout.simple_list_item_2, new String[]{KEY_NAME, KEY_ADDRESS},
                new int[]{android.R.id.text1, android.R.id.text2});
        listViewDevices.setAdapter(mSimpleAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mBroadcastReceiver = new SingleBroadCastReceiver();
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void switchState() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            makeToast(getString(R.string.toast_turned_off));
        } else {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, REQUEST_CODE_BLUETOOTH_ON);
        }
    }

    private void connect(BluetoothDevice device) {
        mDevice = device;
        mBluetoothAdapter.cancelDiscovery();
        Intent intent = new Intent(MainActivity.this, DeviceCommunicateActivity.class);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        startActivityForResult(intent, REQUEST_CODE_DEVICE_COMMUNICATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (REQUEST_CODE_BLUETOOTH_ON == requestCode) {
            makeToast(getString(R.string.toast_turned_on));
            return;
        }
        switch (resultCode) {
            case RESULT_OK:
                saveDeviceInfo(mDevice);
                finish();
                break;
            case RESULT_CANCELED:
                if (data != null) {
                    String message = data.getStringExtra(EXTRA_MESSAGE);
                    makeToast(message);
                }
                getIntent().setAction(ACTION_CHANGE_DEVICE);
                findDevices();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void makeToast(String message) {
        if (message != null) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void findDevices() {
        arrayList.clear();
        mSimpleAdapter.notifyDataSetChanged();

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);

                    break;
                case PackageManager.PERMISSION_GRANTED:
                    mBluetoothAdapter.startDiscovery();
                    break;
            }
        }
    }

    @Override
    protected void onStop() {
        arrayList.clear();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void saveDeviceInfo(BluetoothDevice device) {
        Log.i(TAG, "save device info");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                openFileOutput(FILE_NAME, MODE_PRIVATE)))) {
            writer.write(device.getAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readDeviceInfo() {
        Log.i(TAG, "read device info");
        StringBuilder address = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    openFileInput(FILE_NAME)));
            address.append(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address.toString();
    }

    private class SingleBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                HashMap<String, String> map = new HashMap<>();
                map.put(KEY_NAME, device.getName());
                map.put(KEY_ADDRESS, device.getAddress());
                if (!arrayList.contains(map) && device.getName() != null) {
                    arrayList.add(map);
                }
                mSimpleAdapter.notifyDataSetChanged();
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                bluetoothDevice.setPin(KEY_PIN.getBytes());
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (bluetoothDevice.getBondState()) {
                    case BluetoothDevice.BOND_BONDED:
                        connect(bluetoothDevice);
                }
            }

        }
    }
}
