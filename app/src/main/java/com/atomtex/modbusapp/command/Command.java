package com.atomtex.modbusapp.command;

import com.atomtex.modbusapp.domain.Modbus;
import com.atomtex.modbusapp.service.LocalService;

public interface Command {

    void execute(Modbus modbus, byte[] data, LocalService service);

    void stop();
}
