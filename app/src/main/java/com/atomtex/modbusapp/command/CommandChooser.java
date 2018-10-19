package com.atomtex.modbusapp.command;

import android.util.SparseArray;

import static com.atomtex.modbusapp.util.BTD3Constant.*;

/**
 * This class was designed as a holder for a command's list of a modbus device.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class CommandChooser {

    private static final SparseArray<Command> btdu3commands;

    static {
        btdu3commands = new SparseArray<>();
        btdu3commands.put(READ_STATUS_BINARY_SIGNAL, new ReadStatusBinarySignalCommand());
        btdu3commands.put(READ_STATE_CONTROL_REGISTERS, new ReadStateControlRegistersCommand());
        btdu3commands.put(READ_STATE_DATA_REGISTERS, new ReadStateDataRegistersCommand());
        btdu3commands.put(SEND_CONTROL_SIGNAL, new SendControlSignalCommand());
        btdu3commands.put(CHANGE_STATE_CONTROL_REGISTER, new ChangeStateControlRegisterCommand());
        btdu3commands.put(READ_STATUS_WORD, new ReadStatusWordCommand());
        btdu3commands.put(DIAGNOSTICS, new DiagnosticsCommand());
        btdu3commands.put(READ_SPECTR_NV_MEMORY, new ReadSpectrumNonVolatileMemoryCommand());
        btdu3commands.put(WRITE_SPECTR_NV_MEMORY, new WriteSpectrumNonVolatileMemoryCommand());
        btdu3commands.put(READ_SPECTR_ACCUMULATED_SAMPLE, new ReadAccumulatedSpectrumCommand());
        btdu3commands.put(CHANGE_MULTIPLY_CONTROL_REGISTERS, new ChangeStateMultiplyControlRegistersCommand());
        btdu3commands.put(READ_DEVICE_ID, new ReadDeviceIdCommand());
        btdu3commands.put(READ_DEVICE_ID, new ReadDeviceIdCommand());
        btdu3commands.put(READ_CALIBRATION_DATA_SAMPLE, new ReadCallibrationDataSampleCommand());
        btdu3commands.put(WRITE_CALIBRATION_DATA_SAMPLE, new WriteCalibrationDataSampleCommand());
    }

    public static SparseArray<Command> getCommands() {
        return btdu3commands;
    }

    public static Command getCommand(byte command) {
        return btdu3commands.get(command);
    }


}
