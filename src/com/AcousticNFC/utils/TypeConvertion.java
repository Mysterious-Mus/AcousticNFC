package com.AcousticNFC.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class TypeConvertion {


    public static byte[] booleanListByteArrayTo(ArrayList<Boolean> booleanList) {
        byte[] byteArray = new byte[(booleanList.size() + 7) / 8];
        for (int i = 0; i < booleanList.size(); i++) {
            if (booleanList.get(i)) {
                byteArray[i / 8] |= (1 << (7 - i % 8));
            }
        }
        return byteArray;
    }

    public static String byteArrayToString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] Long2ByteArray(long value) {
        byte[] res = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
        // remove the first 4 bytes (leading 0s)
        return Arrays.copyOfRange(res, 4, 8);
    }

    public static ArrayList<Boolean> byteArrayToBooleanList(byte[] byteArray) {
        ArrayList<Boolean> booleanList = new ArrayList<>();
        for (byte b : byteArray) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> i) & 1) == 1;
                booleanList.add(bit);
            }
        }
        return booleanList;
    }

    public static float[] floatListtoFloatarray(ArrayList<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            floatArray[i] = floatList.get(i);
        }
        return floatArray;
    }

}
