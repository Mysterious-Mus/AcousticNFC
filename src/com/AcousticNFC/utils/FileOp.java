package com.AcousticNFC.utils;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileOp {
    
    public static void outputFloatSeq(double[] Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            for (int i = 0; i < Data.length; i++) {
                writer.append(Double.toString(Data[i]));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputFloatSeq(float[] Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            for (int i = 0; i < Data.length; i++) {
                writer.append(Float.toString(Data[i]));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputDoubleArray(ArrayList<Double> Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            for (Double d: Data) {
                writer.append(Double.toString(d));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputFloatSeq(ArrayList<Float> Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            for (Float d: Data) {
                writer.append(Float.toString(d));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputBitString(ArrayList<Boolean> Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            for (Boolean d: Data) {
                writer.append(d? "1": "0");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * append the byte array to the .bin file  
     * @param Data : Byte[]
     * @param fileName : String
     * @param clearfile : whether to clear the file before writing
     */
    public static void outputBin(byte[] Data, String fileName, boolean clearfile) {
        try {
            FileOutputStream writer = new FileOutputStream(fileName,!clearfile);
            for (Byte d: Data) {
                writer.write(d);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
