package com.atomtex.modbusapp.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.service.LocalService;
import com.atomtex.modbusapp.util.BT_DU3Constant;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_ACTIVATED;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_REQUEST_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_RESPONSE_TEXT;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_MESSAGE_NUMBER;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_ERROR_NUMBER;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_TOGGLE_CLICKABLE;
import static com.atomtex.modbusapp.activity.DeviceActivity.KEY_CONNECTION_STATUS;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_ACTIVE;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_DISCONNECTED;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_RECONNECT;
import static com.atomtex.modbusapp.activity.DeviceActivity.STATUS_UNABLE_CONNECT;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class ReadStatusWordTestFragment extends Fragment implements ServiceFragment, Callback {

    @BindView(R.id.request_text)
    TextView requestText;
    @BindView(R.id.response_text)
    TextView responseText;
    @BindView(R.id.message_number)
    TextView messageNumberView;
    @BindView(R.id.error_number)
    TextView errorNumberView;
    @BindView(R.id.toggle_button)
    ToggleButton toggleButton;

    private LocalService mService;

    private int mMessageNumber;
    private int mErrorNumber;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_read_status_word_test, container, false);
        ButterKnife.bind(this, view);


        toggleButton.setOnClickListener((v) -> {
            if (toggleButton.isChecked()) {
                mService.start(BT_DU3Constant.ADDRESS, BT_DU3Constant.READ_STATUS_WORD_TEST, null);
            } else {
                mService.stop();
            }
        });

        if (savedInstanceState != null) {
            requestText.setText(savedInstanceState.getCharSequence(KEY_REQUEST_TEXT));
            responseText.setText(savedInstanceState.getCharSequence(KEY_RESPONSE_TEXT));
            mMessageNumber = savedInstanceState.getInt(KEY_MESSAGE_NUMBER);
            mErrorNumber = savedInstanceState.getInt(KEY_ERROR_NUMBER);
            toggleButton.setChecked(savedInstanceState.getBoolean(KEY_ACTIVATED));
            toggleButton.setClickable(savedInstanceState.getBoolean(KEY_TOGGLE_CLICKABLE));
        }

        messageNumberView.setText(String.valueOf(mMessageNumber));
        errorNumberView.setText(String.valueOf(mErrorNumber));

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_RESPONSE_TEXT, responseText.getText());
        outState.putCharSequence(KEY_REQUEST_TEXT, requestText.getText());
        outState.putInt(KEY_MESSAGE_NUMBER, mMessageNumber);
        outState.putInt(KEY_ERROR_NUMBER, mErrorNumber);
        outState.putBoolean(KEY_ACTIVATED, toggleButton.isChecked());
        outState.putBoolean(KEY_TOGGLE_CLICKABLE, toggleButton.isClickable());
    }

    @Override
    public void updateUI(Bundle bundle) {
        int status = bundle.getInt(KEY_CONNECTION_STATUS);
        switch (status) {
            case STATUS_ACTIVE:
                requestText.setText(getString(R.string.status_connected));
                toggleButton.setClickable(true);
                break;
            case STATUS_DISCONNECTED:
                requestText.setText(getString(R.string.status_disconnect));
                toggleButton.setClickable(false);
                break;
            case STATUS_RECONNECT:
                requestText.setText(R.string.status_reconnect);
                toggleButton.setClickable(false);
                break;
            case STATUS_UNABLE_CONNECT:
                requestText.setText(R.string.status_unable_connect);
                toggleButton.setClickable(false);
                break;
        }
        mMessageNumber = bundle.getInt(KEY_MESSAGE_NUMBER, mMessageNumber);
        mErrorNumber = bundle.getInt(KEY_ERROR_NUMBER, mErrorNumber);
        messageNumberView.setText(String.valueOf(mMessageNumber));
        errorNumberView.setText(String.valueOf(mErrorNumber));
    }

    @Override
    public void boundService(LocalService service) {
        mService = service;
    }
}
