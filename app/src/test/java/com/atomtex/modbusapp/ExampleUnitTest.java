package com.atomtex.modbusapp;

import com.atomtex.modbusapp.util.BT_DU3Constant;
import com.atomtex.modbusapp.util.BitConverter;
import com.atomtex.modbusapp.util.ByteSwapper;
import com.atomtex.modbusapp.util.ByteUtil;
import com.atomtex.modbusapp.util.CRC16;

import org.junit.Test;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

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

        String str = "1235,34,54,12,2211,2";

        String[] stringArray = str.trim().split(",");
        if (stringArray.length % 2 != 0) {
            System.out.println("Please, enter a valid data");
        }

        ByteBuffer buffer = ByteBuffer.allocate(stringArray.length * 2);

        for (int i = 0; i < stringArray.length; i++) {
            try {
                short x = Short.parseShort(stringArray[i]);
                x = ByteSwapper.swap(x);
                byte[] xBytes = BitConverter.getBytes(x);
                buffer.put(xBytes);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                System.out.println("Please, enter a valid data");
            }
        }

        System.out.println(Arrays.toString(buffer.array()));
    }

    @Test
    public void bitConvertTest() {

        byte x = 0x01;
        byte y = 0x07;

        short value = (short)57921;

        value = ByteSwapper.swap(value);
        byte[] result = BitConverter.getBytes(value);

        System.out.println(CRC16.calcCRC(new byte[]{x,y}));
        System.out.println(Arrays.toString(result));
    }

    @Test
    public void whiteSpaceTest() {
        ByteBuffer buffer;
        String str = " 12 12   34 45   55 0 ";
        int counter = 0;
        String[] arrayString = str.trim().split(" ");
        buffer = ByteBuffer.allocate(arrayString.length);

        for (String number : arrayString) {
            if (!number.equals("")) {
                buffer.put(Byte.parseByte(number));
                counter++;
            }
        }
        System.out.println(Arrays.toString(Arrays.copyOf(buffer.array(), counter)));
    }
}