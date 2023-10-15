package com.AcousticNFC.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileOp {
    
    public void outputFloatSeq(double[] Data, String fileName) {
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

    public void outputFloatSeq(float[] Data, String fileName) {
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

    public void outputDoubleArray(ArrayList<Double> Data, String fileName) {
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

    public void outputFloatSeq(ArrayList<Float> Data, String fileName) {
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
}
