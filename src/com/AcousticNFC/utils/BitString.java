package com.AcousticNFC.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BitString {
    public String filename;
    public int[] bitString;

    public BitString(String filename) {
        this.filename = filename;

        // read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                bitString = new int[line.length()];
                for (int i = 0; i < line.length(); i++) { // each bit is a separate character
                    bitString[i] = Character.getNumericValue(line.charAt(i)); // parse each bit separately
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // print first bits
        System.out.println("First 10 bits of " + filename + ":");
        for (int i = 0; i < 10; i++) {
            System.out.print(bitString[i]);
        }

        // print length
        System.out.println("\nLength of " + filename + ": " + bitString.length);
    }
}
