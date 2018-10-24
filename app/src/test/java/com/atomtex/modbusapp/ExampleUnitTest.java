package com.atomtex.modbusapp;

import android.content.SharedPreferences;

import com.atomtex.modbusapp.util.BTD3Constant;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteSwapper;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void bitTest() {

        ByteBuffer buffer = ByteBuffer.allocate(6);
        byte address = BTD3Constant.ADDRESS;
        byte command = BTD3Constant.READ_STATUS_BINARY_SIGNAL;
        byte[] request;

        short first = 0;
        String x = "8";
        short number = (short) Integer.parseInt(x);

        first = ByteSwapper.swap(first);
        number = ByteSwapper.swap(number);
        byte[] firstBytes = BitConverter.getBytes(first);
        byte[] numberBytes = BitConverter.getBytes(number);

        buffer.put(address).put(command).put(firstBytes).put(numberBytes);

        System.out.println(Arrays.toString(buffer.array()));
    }

}