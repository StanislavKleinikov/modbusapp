package com.atomtex.modbusapp.util;

/**
 * This class contains the list of command for BT-DU3 device
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class BT_DU3Constant {

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
     * User enter a command manually
     */
    public static final byte USER_COMMAND = 0x00;

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
     * To consider the accumulated sample spectrum
     */
    public static final byte READ_SPECTRUM_ACCUMULATED_SAMPLE = 0x0B;

    /**
     * Change the state of control registers
     */
    public static final byte CHANGE_STATE_CONTROL_REGISTERS = 0x10;

    /**
     * Read device identification code
     */
    public static final byte READ_DEVICE_ID = 0x11;

    /**
     * Read calibration data sample
     */
    public static final byte READ_CALIBRATION_DATA_SAMPLE = 0x12;

    /**
     * Record calibration data sample
     */
    public static final byte WRITE_CALIBRATION_DATA_SAMPLE = 0x13;

    /**
     * Read the accumulated spectrum
     */
    public static final byte READ_ACCUMULATED_SPECTRUM = 0x40;

    /**
     * Read the accumulated spectrum in compressed form with the restart of set
     */
    public static final byte READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT = 0x41;

    /**
     * Read the accumulated spectrum in compressed form
     */
    public static final byte READ_ACCUMULATED_SPECTRUM_COMPRESSED = 0x42;

    /**
     * TODO: remove this constant after test
     * Read status word test
     */
    public static final byte READ_STATUS_WORD_TEST = 0x20;



    //The Exception codes

    /**
     * Indicates that the command is invalid
     */
    public static final byte EXCEPTION_INVALID_COMMAND = 0x01;

    /**
     * Indicates that the address data is invalid
     */
    public static final byte EXCEPTION_INVALID_DATA_ADDRESS = 0x02;

    /**
     * Indicates that the value of data is invalid
     */
    public static final byte EXCEPTION_DATA_VALUE = 0x03;

    /**
     * The failure of the detection unit.
     * Detailed information can be obtained using the diagnostic register read command
     */
    public static final byte EXCEPTION_DETECTION_UNIT_FAILURE = 0x04;

    /**
     * The detection unit is busy executing the previous command
     */
    public static final byte EXCEPTION_DETECTION_UNIT_BUSY = 0x06;


}


