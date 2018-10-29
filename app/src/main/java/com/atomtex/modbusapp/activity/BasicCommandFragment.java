package com.atomtex.modbusapp.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteSwapper;

import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_CONNECTION_STATUS;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_COMMAND;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_DISCONNECTED;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_NONE;
import static com.atomtex.modbusapp.util.BT_DU3Constant.*;

public class BasicCommandFragment extends Fragment implements ServiceFragment, Callback {

    @BindView(R.id.first_text)
    TextView first_text;
    @BindView(R.id.second_text)
    TextView second_text;
    @BindView(R.id.request_text)
    TextView requestText;
    @BindView(R.id.response_text)
    TextView responseText;
    @BindView(R.id.first_signal)
    EditText first_field;
    @BindView(R.id.number_signal)
    EditText second_field;
    @BindView(R.id.send_button)
    Button sendButton;
    @BindView(R.id.address_checkbox)
    CheckBox addressCheckBox;
    @BindView(R.id.address)
    EditText addressView;

    private static final String KEY_CHECKED = "isChecked";

    private LocalService mService;
    private byte mCommand;

    public static BasicCommandFragment newInstance(byte command) {
        BasicCommandFragment fragment = new BasicCommandFragment();
        Bundle bundle = new Bundle();
        bundle.putByte(KEY_COMMAND, command);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mCommand = bundle.getByte(KEY_COMMAND);
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_read_state_binary_signal, container, false);
        ButterKnife.bind(this, view);

        responseText.setMovementMethod(new ScrollingMovementMethod());

        switch (mCommand) {
            case READ_STATUS_WORD:
                commandWithoutData();
                break;
            case SEND_CONTROL_SIGNAL:
                sendControlSignal();
                break;
            case CHANGE_STATE_CONTROL_REGISTER:
                changeStateControlRegister();
                break;
            case DIAGNOSTICS:
                diagnostics();
                break;
            case READ_DEVICE_ID:
                commandWithoutData();
                break;
            case READ_ACCUMULATED_SPECTER:
                commandWithoutData();
                break;
            case READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT:
                commandWithoutData();
                break;
            default:
                defaultCommand();
                break;
        }
        return view;
    }

    private void commandWithoutData() {
        first_text.setVisibility(View.GONE);
        first_field.setVisibility(View.GONE);
        second_text.setVisibility(View.GONE);
        second_field.setVisibility(View.GONE);

        sendButton.setOnClickListener(v -> {
            try {
                byte address = Byte.parseByte(addressView.getText().toString());
                mService.start(address, mCommand, null);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Enter a valid data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendControlSignal() {
        first_text.setText(R.string.control_signal_code);
        second_text.setText(R.string.value);
        defaultCommand();
    }

    private void changeStateControlRegister() {
        first_text.setText(R.string.number_register);
        second_text.setText(R.string.value);
        defaultCommand();
    }

    private void diagnostics() {
        first_text.setText(R.string.sub_command);
        second_text.setText(R.string.data);
        defaultCommand();
    }

    private void defaultCommand() {
        sendButton.setOnClickListener(v -> {
            ByteBuffer buffer;
            byte[] firstSignalBytes, numberSignalBytes;
            short firstSignal, numberSignal;
            try {
                byte address = Byte.parseByte(addressView.getText().toString());
                buffer = ByteBuffer.allocate(4);
                firstSignal = (short) Integer.parseInt(first_field.getText().toString());
                numberSignal = (short) Integer.parseInt(second_field.getText().toString());
                firstSignal = ByteSwapper.swap(firstSignal);
                numberSignal = ByteSwapper.swap(numberSignal);
                firstSignalBytes = BitConverter.getBytes(firstSignal);
                numberSignalBytes = BitConverter.getBytes(numberSignal);
                buffer.put(firstSignalBytes).put(numberSignalBytes);
                mService.start(address, mCommand, buffer.array());
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Enter a valid data", Toast.LENGTH_SHORT).show();
            }

        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            requestText.setText(savedInstanceState.getString(KEY_REQUEST_TEXT));
            responseText.setText(savedInstanceState.getString(KEY_RESPONSE_TEXT));
            addressCheckBox.setChecked(savedInstanceState.getBoolean(KEY_CHECKED));
        }

        if (addressCheckBox.isChecked()) {
            addressView.setEnabled(false);
        }

        addressCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isChecked()) {
                addressView.setText(R.string.default_device_address);
                addressView.setEnabled(false);
            } else {
                addressView.setEnabled(true);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_REQUEST_TEXT, requestText.getText().toString());
        outState.putString(KEY_RESPONSE_TEXT, responseText.getText().toString());
        outState.putBoolean(KEY_CHECKED, addressCheckBox.isChecked());
    }

    @Override
    public void boundService(LocalService service) {
        mService = service;
    }

    @Override
    public void updateUI(Bundle bundle) {
        int status = bundle.getInt(KEY_CONNECTION_STATUS, STATUS_NONE);
        byte exception = bundle.getByte(KEY_EXCEPTION);

        if (status != STATUS_NONE) {
            switch (status) {
                case STATUS_DISCONNECTED:
                    responseText.setText(getString(R.string.status_disconnect));
                    break;
            }
        } else if (exception != 0) {
            switch (exception) {
                case EXCEPTION_INVALID_COMMAND:
                    responseText.setText(getString(R.string.exception_invalid_command));
                    break;
                case EXCEPTION_INVALID_DATA_ADDRESS:
                    responseText.setText(getString(R.string.exception_invalid_data_address));
                    break;
                case EXCEPTION_DATA_VALUE:
                    responseText.setText(getString(R.string.exception_invalid_data_value));
                    break;
                case EXCEPTION_DETECTION_UNIT_FAILURE:
                    responseText.setText(getString(R.string.exception_detection_unit_failure));
                    break;
                case EXCEPTION_DETECTION_UNIT_BUSY:
                    responseText.setText(getString(R.string.exception_detection_unit_busy));
                    break;
            }
        } else {
            responseText.setText(bundle.getString(KEY_RESPONSE_TEXT, null));
        }
        requestText.setText(bundle.getString(KEY_REQUEST_TEXT, null));
    }
}

