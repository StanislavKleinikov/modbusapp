package com.atomtex.modbusapp.util;

/**
 * This class contains the list of command for BT-DU3 device
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class BTD3Constant {

    /**
     * The address of a modbus device by default
     */
    public static final byte ADDRESS = 0x01;

    /**
     * The address of the first detection unit
     */
    public static final byte ADDRESS_DU_1 = (byte) 0x81;

    /**
     * The address of the second detection unit
     */
    public static final byte ADDRESS_DU_2 = (byte) 0x82;

    /**
     * The address of the third detection unit
     */
    public static final byte ADDRESS_DU_3 = (byte) 0x83;


    //Basic commands


    /**
     * To consider the status of the binary signals
     */
    public static final byte READ_STATUS_BINARY_SIGNAL = 0x02;

    /**
     * Consider the state of the control registers
     */
    public static final byte READ_STATE_CONTROL_REGISTERS = 0x03;

    /**
     * Read state of data registers
     */
    public static final byte READ_STATE_DATA_REGISTERS = 0x04;

    /**
     * Send control signal
     */
    public static final byte SEND_CONTROL_SIGNAL = 0x05;

    /**
     * Change the state of the control register
     */
    public static final byte CHANGE_STATE_CONTROL_REGISTER = 0x06;

    /**
     * Read status word
     */
    public static final byte READ_STATUS_WORD = 0x07;

    /**
     * Diagnostics
     */
    public static final byte DIAGNOSTICS = 0x08;

    /**
     * Consider a sample spectrum from the non-volatile memory
     */
    public static final byte READ_SPECTR_NV_MEMORY = 0x09;

    /**
     * Record the sample spectrum in the non-volatile memory
     */
    public static final byte WRITE_SPECTR_NV_MEMORY = 0x10;

    /**
     * To consider the accumulated sample spectrum
     */
    public static final byte READ_SPECTR_ACCUMULATED_SAMPLE = 0x11;

    /**
     * Change the state of control registers
     */
    public static final byte CHANGE_MULTIPLY_CONTROL_REGISTERS = 0x16;

    /**
     * Read device identification code
     */
    public static final byte READ_DEVICE_ID = 0x17;

    /**
     * Read calibration data sample
     */
    public static final byte READ_CALIBRATION_DATA_SAMPLE = 0x18;

    /**
     * Record calibration data sample
     */
    public static final byte WRITE_CALIBRATION_DATA_SAMPLE = 0x19;


    //The subcommands

//    /**
//     * Return a request data back
//     */
//    public static final byte RETURN_REQUEST_DATA = 0x00;
//
//    /**
//     * Record calibration data sample
//     */
//    public static final byte WRITE_CALIBRATION_DATA_SAMPLE = 0x01;
//
//    /**
//     * Record calibration data sample
//     */
//    public static final byte WRITE_CALIBRATION_DATA_SAMPLE = 0x02;


}

