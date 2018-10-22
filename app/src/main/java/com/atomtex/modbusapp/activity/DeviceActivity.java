package com.atomtex.modbusapp.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.atomtex.modbusapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.atomtex.modbusapp.activity.MainActivity.TAG;

public class DeviceActivity extends AppCompatActivity  {

    @BindView(R.id.spinner)
    Spinner mSpinner;
    @BindView(R.id.change_device)
    Button changeDeviceButton;
    private Fragment mFragment;
    private ProgressDialog mDialog;
    private Intent mServiceIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        init();
    }


    private void init(){

        mDialog = new ProgressDialog(DeviceActivity.this);
        mDialog.setTitle("Connecting to device");
        mDialog.setMessage("Please wait..");
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);

        changeDeviceButton.setOnClickListener(v ->
                cancel(getString(R.string.toast_change_device)));


        ArrayAdapter<?> adapter =
                ArrayAdapter.createFromResource(this, R.array.command, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(adapter);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        mFragment = new ReadStateBinarySignalFragment();
                        fragmentTransaction.add(R.id.fragment_container,mFragment);
                        fragmentTransaction.disallowAddToBackStack();
                        fragmentTransaction.commit();
                        break;
                    case 1:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void cancel(String message) {
        Log.e(TAG, "cancel");
        setResult(RESULT_CANCELED);
        stopService(mServiceIntent);
        getIntent().putExtra(MainActivity.EXTRA_MESSAGE, message);
        finish();
    }
}
