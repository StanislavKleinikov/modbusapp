package com.atomtex.modbusapp.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.atomtex.modbusapp.R;
import com.atomtex.modbusapp.util.BTD3Constant;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReadStateBinarySignalFragment extends Fragment {

    @BindView(R.id.first_high)
    EditText firstHighView;
    @BindView(R.id.first_low)
    EditText firstLowView;
    @BindView(R.id.number_high)
    EditText numberHighView;
    @BindView(R.id.number_low)
    EditText numberLowView;
    @BindView(R.id.send_button)
    Button sendButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_read_state_binary_signal, container, false);
        ButterKnife.bind(this,view);

        sendButton.setOnClickListener(v -> {
            byte address = BTD3Constant.ADDRESS;
            byte command = BTD3Constant.READ_STATUS_BINARY_SIGNAL;
            byte firstHigh;
            byte firstLow;
            byte numberHigh;
            byte numberLow;
            byte [] request;
            try{ firstHigh = Byte.parseByte(firstHighView.getText().toString());
                 firstLow = Byte.parseByte(firstLowView.getText().toString());
                 numberHigh = Byte.parseByte(numberHighView.getText().toString());
                 numberLow = Byte.parseByte(numberLowView.getText().toString());
                 request = new byte[]{address,command,firstHigh,firstLow,numberHigh,numberLow};
            }catch (NumberFormatException e){
                Toast.makeText(getActivity(), "Enter a valid data", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}

