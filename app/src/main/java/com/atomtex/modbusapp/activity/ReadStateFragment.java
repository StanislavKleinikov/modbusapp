package com.atomtex.modbusapp.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BTD3Constant;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteSwapper;
import com.atomtex.modbusapp.util.ByteUtil;

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
import static com.atomtex.modbusapp.util.BTD3Constant.*;

public class ReadStateFragment extends Fragment implements ServiceFragment, Callback {

    @BindView(R.id.first_signal_text)
    TextView firstSignalText;
    @BindView(R.id.number_signal_text)
    TextView numberSignalText;
    @BindView(R.id.request_text)
    TextView requestText;
    @BindView(R.id.response_text)
    TextView responseText;
    @BindView(R.id.first_signal)
    EditText firstSignalView;
    @BindView(R.id.number_signal)
    EditText numberSignalView;
    @BindView(R.id.send_button)
    Button sendButton;

    private LocalService mService;
    private byte mCommand;

    public static ReadStateFragment newInstance(byte command) {
        ReadStateFragment fragment = new ReadStateFragment();
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

        if (mCommand == READ_STATUS_WORD) {
            firstSignalView.setVisibility(View.INVISIBLE);
            numberSignalView.setVisibility(View.INVISIBLE);
            firstSignalText.setVisibility(View.INVISIBLE);
            numberSignalText.setVisibility(View.INVISIBLE);
        }

        sendButton.setOnClickListener(v -> {
            ByteBuffer buffer;
            byte[] firstSignalBytes, numberSignalBytes;
            short firstSignal, numberSignal;
            if (mCommand == READ_STATUS_WORD) {
                buffer = ByteBuffer.allocate(2);
                buffer.put(BTD3Constant.ADDRESS).put(mCommand);
                mService.stop();
                mService.start(buffer.array());
            } else {
                try {
                    buffer = ByteBuffer.allocate(6);
                    firstSignal = (short) Integer.parseInt(firstSignalView.getText().toString());
                    numberSignal = (short) Integer.parseInt(numberSignalView.getText().toString());
                    firstSignal = ByteSwapper.swap(firstSignal);
                    numberSignal = ByteSwapper.swap(numberSignal);
                    firstSignalBytes = BitConverter.getBytes(firstSignal);
                    numberSignalBytes = BitConverter.getBytes(numberSignal);
                    buffer.put(BTD3Constant.ADDRESS).put(mCommand).put(firstSignalBytes).put(numberSignalBytes);
                    mService.start(buffer.array());
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Enter a valid data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            requestText.setText(savedInstanceState.getString(KEY_REQUEST_TEXT));
            responseText.setText(savedInstanceState.getString(KEY_RESPONSE_TEXT));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_REQUEST_TEXT, requestText.getText().toString());
        outState.putString(KEY_RESPONSE_TEXT, responseText.getText().toString());
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

