package com.atomtex.modbusapp.activity;

import android.graphics.Color;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.domain.ModbusSlave;
import com.atomtex.modbusapp.service.DeviceService;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteSwapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_ACTIVATED;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_EXCEPTION;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_COMMAND;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_STATUS;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_TOGGLE_CLICKABLE;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_ACTIVE;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_DISCONNECTED;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_NONE;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_RECONNECT;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_SPECTRUM_RECEIVED;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_UNABLE_CONNECT;
import static com.atomtex.modbusapp.util.BT_DU3Constant.*;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class BasicCommandFragment extends Fragment
        implements ServiceFragment, Callback, OnChartValueSelectedListener {

    private static final String KEY_BUTTON_TEXT = "buttonText";
    private static final String KEY_SPECTER_DATA_SET = "specterDataSet";
    @BindView(R.id.layout_data_container)
    LinearLayout layoutDataContainer;
    @BindView(R.id.first_text)
    TextView first_text;
    @BindView(R.id.second_text)
    TextView second_text;
    @BindView(R.id.third_text)
    TextView third_text;
    @BindView(R.id.request_mode)
    CheckBox requestMode;
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
    @BindView(R.id.container)
    FrameLayout frameLayout;

    private LocalService mService;
    private byte mCommand;
    private Modbus mModbus;

    private LineChart graph;
    private ArrayList<Entry> entries;

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

        if (savedInstanceState != null) {
            requestText.setText(savedInstanceState.getString(KEY_REQUEST_TEXT));
            responseText.setText(savedInstanceState.getString(KEY_RESPONSE_TEXT));
            sendButton.setActivated(savedInstanceState.getBoolean(KEY_ACTIVATED, false));
            sendButton.setText(savedInstanceState.getString(KEY_BUTTON_TEXT));
            sendButton.setClickable(savedInstanceState.getBoolean(KEY_TOGGLE_CLICKABLE, true));
            entries = savedInstanceState.getParcelableArrayList(KEY_SPECTER_DATA_SET);
        }

        responseText.setMovementMethod(new ScrollingMovementMethod());
        mModbus = ModbusSlave.getInstance();
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
            case READ_SPECTRUM_ACCUMULATED_SAMPLE:
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
            case READ_ACCUMULATED_SPECTRUM:
                initGraph();
                commandWithoutData();
                break;
            case READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT:
                initGraph();
                commandWithoutData();
                break;
            case READ_ACCUMULATED_SPECTRUM_COMPRESSED:
                responseText.setVisibility(View.GONE);
                initGraph();
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
            if (sendButton.isActivated()) {
                mService.stop();
                sendButton.setActivated(false);
                sendButton.setText(R.string.send_button);
            } else {
                ByteBuffer buffer;
                String dataString;
                int counter = 0;
                try {
                    dataString = third_field.getText().toString().trim();
                    String[] stringArray = dataString.split(" ");
                    buffer = ByteBuffer.allocate(stringArray.length);

                    for (String number : stringArray) {
                        if (!number.equals("")) {
                            buffer.put((byte) Integer.parseInt(number, 16));
                            counter++;
                        }
                    }

                    executeCommand(buffer.array()[0], mCommand, Arrays.copyOf(buffer.array(), counter));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void commandWithoutData() {
        first_text.setVisibility(View.GONE);
        first_field.setVisibility(View.GONE);
        second_text.setVisibility(View.GONE);
        second_field.setVisibility(View.GONE);

        sendButton.setOnClickListener(v -> {
            if (sendButton.isActivated()) {
                mService.stop();
                sendButton.setActivated(false);
                sendButton.setText(R.string.send_button);
            } else {
                try {
                    byte address = Byte.parseByte(addressView.getText().toString().trim());
                    executeCommand(address, mCommand, null);
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
                }
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
            if (sendButton.isActivated()) {
                mService.stop();
                sendButton.setActivated(false);
                sendButton.setText(R.string.send_button);
            } else {
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

                    executeCommand(address, mCommand, buffer.array());
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
                }
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
            if (sendButton.isActivated()) {
                mService.stop();
                sendButton.setActivated(false);
                sendButton.setText(R.string.send_button);
            } else {
                ByteBuffer buffer;
                String dataString;
                int counter = 0;
                byte[] firstValueBytes, secondValueBytes;
                short firstValue, secondValue;
                try {
                    byte address = Byte.parseByte(addressView.getText().toString().trim());
                    firstValue = (short) Integer.parseInt(first_field.getText().toString().trim());
                    secondValue = (short) Integer.parseInt(second_field.getText().toString().trim());
                    dataString = third_field.getText().toString().trim();
                    String[] stringArray = dataString.split(" ");

                    firstValue = ByteSwapper.swap(firstValue);
                    secondValue = ByteSwapper.swap(secondValue);
                    firstValueBytes = BitConverter.getBytes(firstValue);
                    secondValueBytes = BitConverter.getBytes(secondValue);

                    buffer = ByteBuffer.allocate(4 + stringArray.length * 2);
                    buffer.put(firstValueBytes).put(secondValueBytes);

                    for (String number : stringArray) {
                        if (!number.equals("")) {
                            short value = Short.parseShort(number);
                            value = ByteSwapper.swap(value);
                            byte[] xBytes = BitConverter.getBytes(value);
                            buffer.put(xBytes);
                            counter++;
                        }
                    }

                    if (secondValue != counter) {
                        throw new NumberFormatException();
                    }

                    executeCommand(address, mCommand, Arrays.copyOf(buffer.array(), counter + 4));
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
                }
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
            if (sendButton.isActivated()) {
                mService.stop();
                sendButton.setActivated(false);
                sendButton.setText(R.string.send_button);
            } else {
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
                    executeCommand(address, mCommand, buffer.array());
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), R.string.toast_invalid_data, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void executeCommand(byte address, byte commandByte, byte[] commandData) {
        if (requestMode.isChecked()) {
            sendButton.setActivated(true);
            sendButton.setText(R.string.toggle_on);
            mService.start(address, commandByte, commandData, DeviceService.MODE_AUTO);
        } else {
            mService.start(address, commandByte, commandData, DeviceService.MODE_SINGLE_REQUEST);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_REQUEST_TEXT, requestText.getText().toString());
        outState.putString(KEY_RESPONSE_TEXT, responseText.getText().toString());
        outState.putBoolean(KEY_ACTIVATED, sendButton.isActivated());
        outState.putString(KEY_BUTTON_TEXT, sendButton.getText().toString());
        outState.putBoolean(KEY_TOGGLE_CLICKABLE, sendButton.isClickable());
        outState.putParcelableArrayList(KEY_SPECTER_DATA_SET, entries);
    }

    @Override
    public void boundService(LocalService service) {
        mService = service;
    }

    @Override
    public void updateUI(Bundle bundle) {
        int status = bundle.getInt(KEY_STATUS, STATUS_NONE);
        byte exception = bundle.getByte(KEY_EXCEPTION);

        if (status != STATUS_NONE) {
            switch (status) {
                case STATUS_ACTIVE:
                    requestText.setText(getString(R.string.status_connected));
                    sendButton.setClickable(true);
                    break;
                case STATUS_DISCONNECTED:
                    requestText.setText(getString(R.string.status_disconnect));
                    sendButton.setClickable(false);
                    break;
                case STATUS_RECONNECT:
                    requestText.setText(R.string.status_reconnect);
                    sendButton.setClickable(false);
                    break;
                case STATUS_UNABLE_CONNECT:
                    requestText.setText(R.string.status_unable_connect);
                    sendButton.setClickable(false);
                    break;
                case STATUS_SPECTRUM_RECEIVED:
                    updateGraph();
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
            requestText.setText(bundle.getString(KEY_REQUEST_TEXT, null));
        }
    }

    private void updateGraph() {
        graph.clear();
        int[] spectrum = mModbus.getSpectrum();

        entries = new ArrayList<>(spectrum.length);

        for (int i = 0; i < spectrum.length; i++) {
            entries.add(new Entry(i, spectrum[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "specter");
        dataSet.setColor(Color.BLACK);
        dataSet.setLineWidth(.5f);
        dataSet.setDrawCircles(false);
        dataSet.setHighLightColor(Color.RED);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        graph.setData(lineData);
        graph.invalidate();

    }


    private void initGraph() {
        graph = new LineChart(getContext());
        frameLayout.addView(graph);

        if (entries != null) {
            LineDataSet dataSet = new LineDataSet(entries, "specter");
            dataSet.setColor(Color.BLACK);
            dataSet.setLineWidth(0);
            dataSet.setDrawCircles(false);
            dataSet.setHighLightColor(Color.RED);
            LineData lineData = new LineData(dataSet);
            graph.setData(lineData);
        }
        Description description = new Description();
        description.setText("Specter graph");
        graph.setDescription(description);
        graph.setOnChartValueSelectedListener(this);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Toast.makeText(getContext(), "X " + e.getX() + " Y " + e.getY(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {

    }
}

