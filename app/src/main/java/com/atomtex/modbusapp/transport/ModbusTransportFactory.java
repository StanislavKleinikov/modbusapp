package com.atomtex.modbusapp.transport;

import android.bluetooth.BluetoothDevice;

/**
 * Contains methods which return the {@link ModbusTransport} implementations.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusTransportFactory {

    public static ModbusTransport getTransport(BluetoothDevice device) {
        return ModbusRFCOMMTransport.getInstance(device);
    }
}
