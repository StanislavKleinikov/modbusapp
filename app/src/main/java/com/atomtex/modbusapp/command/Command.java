package com.atomtex.modbusapp.command;

import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.service.LocalService;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public interface Command {

    void execute(Modbus modbus, byte[] data, LocalService service);

    void clear();

    void stop();
}