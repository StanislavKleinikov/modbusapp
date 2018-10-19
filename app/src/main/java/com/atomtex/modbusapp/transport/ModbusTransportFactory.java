package com.atomtex.modbusapp.transport;

import android.bluetooth.BluetoothDevice;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusTransportFactory {

    public static ModbusTransport getTransport(BluetoothDevice device) {
        return ModbusRFCOMMTransport.getInstance(device);
    }
}
