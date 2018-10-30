package com.atomtex.modbusapp.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class BasicCommandFragment extends Fragment implements ServiceFragment, Callback {

    @BindView(R.id.layout_data_container)
    LinearLayout layoutDataContainer;
    @BindView(R.id.first_text)
    TextView first_text;
    @BindView(R.id.second_text)
    TextView second_text;
    @BindView(R.id.third_text)
    TextView third_text;
    @BindView(R.id.request_text)
    TextView requestText;
    @BindView(R.id.response_text)
    TextView responseText;
    @BindView(R.id.first_value)
    EditText first_field;
    @BindView(R.id.second_value)
    EditText second_field;
    @BindView(R.id.third_value)
    EditText third_field;
    @BindView(R.id.send_button)
    Button sendButton;
    @BindView(R.id.address)
    EditText addressView;

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
        View view = inflater.inflate(R.layout.fragment_basic_command, container, false);
        ButterKnife.bind(this, view);

        responseText.setMovementMethod(new ScrollingMovementMethod());

        switch (mCommand) {
            case USER_COMMAND:
                userCommand();
                break;
            case SEND_CONTROL_SIGNAL:
                sendControlSignal();
                break;
            case CHANGE_STATE_CONTROL_REGISTER:
                changeStateControlRegister();
                break;
            case READ_STATUS_WORD:
                commandWithoutData();
                break;
            case DIAGNOSTICS:
                diagnostics();
                break;
            case CHANGE_STATE_CONTROL_REGISTERS:
                changeMultiplyState();
                break;
            case READ_SPECTER_ACCUMULATED_SAMPLE:
                readSample();
                break;
            case READ_DEVICE_ID:
                commandWithoutData();
                break;
            case READ_CALIBRATION_DATA_SAMPLE:
                readSample();
                break;
            case WRITE_CALIBRATION_DATA_SAMPLE:
                changeMultiplyState();
                break;
            case READ_ACCUMULATED_SPECTER:
                commandWithoutData();
                break;
            case READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT:
                commandWithoutData();
                break;
            case READ_ACCUMULATED_SPECTER_COMPRESSED:
                commandWithoutData();
                break;
            default:
                defaultCommand();
                break;
        }
        return view;
    }

    private void userCommand() {
        first_text.setVisibility(View.GONE);
        first_field.setVisibility(View.GONE);
        second_text.setVisibility(View.GONE);
        second_field.setVisibility(View.GONE);
        addressView.setVisibility(View.GONE);
        layoutDataContainer.setVisibility(View.VISIBLE);
        third_text.setText(R.string.command_data);
        third_field.setInputType(InputType.TYPE_CLASS_TEXT);

        sendButton.setOnClickListener(v -> {
            ByteBuffer buffer;
            String dataString;
            try {
                dataString = third_field.getText().toString().trim();
                String[] stringArray = dataString.split(" ");
                buffer = ByteBuffer.allocate(stringArray.length);

                for (String number : stringArray) {
                    buffer.put((byte) Integer.parseInt(number, 16));
                }

                mService.start(buffer.array()[0], mCommand, buffer.array());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void commandWithoutData() {
        first_text.setVisibility(View.GONE);
        first_field.setVisibility(View.GONE);
        second_text.setVisibility(View.GONE);
        second_field.setVisibility(View.GONE);

        sendButton.setOnClickListener(v -> {
            try {
                byte address = Byte.parseByte(addressView.getText().toString().trim());
                mService.start(address, mCommand, null);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void readSample() {
        first_text.setText(R.string.first_byte_address);
        second_text.setText(R.string.number_bytes);
        defaultCommand();
    }

    private void sendControlSignal() {
        first_text.setText(R.string.control_signal_code);
        second_text.setText(R.string.value);
        first_field.setHint(R.string.hex_number);
        second_field.setHint(R.string.hex_number);

        sendButton.setOnClickListener(v -> {
            ByteBuffer buffer;
            byte[] firstValueBytes, secondValueBytes;
            String firstString, secondString;
            short firstValue, secondValue;
            try {
                buffer = ByteBuffer.allocate(4);
                byte address = Byte.parseByte(addressView.getText().toString());
                firstString = first_field.getText().toString().trim();
                secondString = second_field.getText().toString().trim();

                if (firstString.startsWith(getString(R.string.hex_prefix))) {
                    firstString = firstString.substring(2, firstString.length());
                }
                if (secondString.startsWith(getString(R.string.hex_prefix))) {
                    secondString = secondString.substring(2, secondString.length());
                }

                firstValue = (short) Integer.parseInt(firstString, 16);
                secondValue = (short) Integer.parseInt(secondString, 16);
                firstValue = ByteSwapper.swap(firstValue);
                secondValue = ByteSwapper.swap(secondValue);
                firstValueBytes = BitConverter.getBytes(firstValue);
                secondValueBytes = BitConverter.getBytes(secondValue);

                buffer.put(firstValueBytes).put(secondValueBytes);

                mService.start(address, mCommand, buffer.array());
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changeStateControlRegister() {
        first_text.setText(R.string.number_register);
        second_text.setText(R.string.value);
        defaultCommand();
    }

    private void changeMultiplyState() {
        layoutDataContainer.setVisibility(View.VISIBLE);
        first_text.setText(getString(R.string.first_register_number));
        second_text.setText(getString(R.string.number_registers));

        sendButton.setOnClickListener(v -> {
            ByteBuffer buffer;
            String dataString;
            byte[] firstValueBytes, secondValueBytes;
            short firstValue, secondValue;
            try {
                byte address = Byte.parseByte(addressView.getText().toString().trim());
                firstValue = (short) Integer.parseInt(first_field.getText().toString().trim());
                secondValue = (short) Integer.parseInt(second_field.getText().toString().trim());
                dataString = third_field.getText().toString().trim();
                String[] stringArray = dataString.split(" ");
                if (secondValue != dataString.length()) {
                    throw new NumberFormatException();
                }
                firstValue = ByteSwapper.swap(firstValue);
                secondValue = ByteSwapper.swap(secondValue);
                firstValueBytes = BitConverter.getBytes(firstValue);
                secondValueBytes = BitConverter.getBytes(secondValue);

                buffer = ByteBuffer.allocate(4 + stringArray.length * 2);
                buffer.put(firstValueBytes).put(secondValueBytes);

                for (String number : stringArray) {
                    short value = Short.parseShort(number);
                    value = ByteSwapper.swap(value);
                    byte[] xBytes = BitConverter.getBytes(value);
                    buffer.put(xBytes);
                }

                mService.start(address, mCommand, buffer.array());
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void diagnostics() {
        first_text.setText(R.string.sub_command);
        second_text.setText(R.string.data);
        defaultCommand();
    }

    private void defaultCommand() {
        sendButton.setOnClickListener(v -> {
            ByteBuffer buffer;
            byte[] firstValueBytes, secondValueBytes;
            short firstValue, secondValue;
            try {
                byte address = Byte.parseByte(addressView.getText().toString().trim());
                buffer = ByteBuffer.allocate(4);
                firstValue = (short) Integer.parseInt(first_field.getText().toString().trim());
                secondValue = (short) Integer.parseInt(second_field.getText().toString().trim());
                firstValue = ByteSwapper.swap(firstValue);
                secondValue = ByteSwapper.swap(secondValue);
                firstValueBytes = BitConverter.getBytes(firstValue);
                secondValueBytes = BitConverter.getBytes(secondValue);
                buffer.put(firstValueBytes).put(secondValueBytes);
                mService.start(address, mCommand, buffer.array());
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
            }
        });
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

