package com.atomtex.modbusapp.command;

import android.util.SparseArray;

import static com.atomtex.modbusapp.util.BT_DU3Constant.*;

/**
 * This class was designed as a holder for a command's list of a modbus device.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class CommandChooser {

    private static final SparseArray<Command> btdu3commands;

    static {
        btdu3commands = new SparseArray<>();
        btdu3commands.put(READ_STATUS_BINARY_SIGNAL, BasicCommand.getInstance());
        btdu3commands.put(READ_STATE_CONTROL_REGISTERS, BasicCommand.getInstance());
        btdu3commands.put(READ_STATE_DATA_REGISTERS, BasicCommand.getInstance());
        btdu3commands.put(READ_STATUS_WORD, BasicCommand.getInstance());
        btdu3commands.put(SEND_CONTROL_SIGNAL, BasicCommand.getInstance());
        btdu3commands.put(CHANGE_STATE_CONTROL_REGISTER, BasicCommand.getInstance());
        btdu3commands.put(CHANGE_STATE_CONTROL_REGISTERS, BasicCommand.getInstance());
        btdu3commands.put(DIAGNOSTICS, BasicCommand.getInstance());
        btdu3commands.put(READ_DEVICE_ID, BasicCommand.getInstance());
        btdu3commands.put(READ_SPECTER_ACCUMULATED_SAMPLE, SpecterCommand.getInstance());
        btdu3commands.put(READ_ACCUMULATED_SPECTER, SpecterCommand.getInstance());
        btdu3commands.put(READ_ACCUMULATED_SPECTER_COMPRESSED_REBOOT, SpecterCommand.getInstance());
        btdu3commands.put(READ_ACCUMULATED_SPECTER_COMPRESSED, SpecterCommand.getInstance());
        btdu3commands.put(READ_STATUS_WORD_TEST, ReadStatusWordTestCommand.getInstance());
    }

    public static SparseArray<Command> getCommands() {
        return btdu3commands;
    }

    public static Command getCommand(byte command) {
        return btdu3commands.get(command);
    }


}
