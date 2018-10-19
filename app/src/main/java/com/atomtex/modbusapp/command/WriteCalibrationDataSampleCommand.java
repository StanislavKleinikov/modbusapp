package com.atomtex.modbusapp.command;

import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.service.LocalService;

/**
 * The implementation of the {@link Command} interface.
 *
 *
 */
public class WriteCalibrationDataSampleCommand implements Command {
    @Override
    public void execute(Modbus modbus, byte[] data, LocalService service) {

    }

    @Override
    public void clear() {

    }

    @Override
    public void stop() {

    }
}
