package com.AcousticNFC.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BitString {
    public String filename;
    public ArrayList<Boolean> bitString;

    public BitString(String filename) {
        this.filename = filename;
        this.bitString = new ArrayList<Boolean>();

        // read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                for (int i = 0; i < line.length(); i++) { // each bit is a separate character
                    bitString.add(line.charAt(i) == '1' ? true : false);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Boolean> getBitString() {
        return bitString;
    }
}
