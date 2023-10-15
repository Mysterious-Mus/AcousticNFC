package com.AcousticNFC.utils;

import java.io.FileWriter;
import java.io.IOException;

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

}
