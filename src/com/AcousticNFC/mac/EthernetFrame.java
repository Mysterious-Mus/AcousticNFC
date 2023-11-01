package com.AcousticNFC.mac;

import java.util.zip.CRC32;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Arrays;

import com.AcousticNFC.Config;
import com.AcousticNFC.physical.transmit.OFDM;


/*
 * IEEE 802.3 based Ethernet Frame 
 * Destination Address: 1 bytes
 * Source Address: 1 bytes
 * Data: 1000 bytes
 * Frame Check Sequence: 4 bytes
 */

public class EthernetFrame {
    // private static final byte[] preamble = HexFormat.of().parseHex("aa".repeat(7));
    // private static final byte[] startOfFrame = HexFormat.of().parseHex("ab");
    private byte[] destinationAddress;
    private byte[] sourceAddress;
    private byte[] data;


    public EthernetFrame(byte[] destinationAddress, byte[] sourceAddress, byte[] data, Config   cfg_src) {
        this.destinationAddress = destinationAddress;
        this.sourceAddress = sourceAddress;
        this.data = data;
    }

    public byte[] Long2ByteArray(long value) {
        byte[] res = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
        // remove the first 4 bytes (leading 0s)
        return Arrays.copyOfRange(res, 4, 8);
    }

    public ArrayList<Boolean> byteArrayToBooleanList(byte[] byteArray) {
        ArrayList<Boolean> booleanList = new ArrayList<>();
    
        for (byte b : byteArray) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> i) & 1) == 1;
                booleanList.add(bit);
            }
        }
    
        return booleanList;
    }

    public static String byteArrayToString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public byte[] getFrame() {
        ByteArrayOutputStream frame = new ByteArrayOutputStream( );
        try {

            
            // // Preamble (56 bits)
            // frame.write(preamble);

            // // Start of Frame (8 bits)
            // frame.write(startOfFrame);
            
            // Destination Address (8 bits)
            frame.write(destinationAddress);
            
            // Source Address (8 bits)
            frame.write(sourceAddress);
            
            // Data (8000 bits)
            frame.write(data);
            
            // CRC (32 bits)
            CRC32 crc = new CRC32();
            crc.update(frame.toByteArray());
            frame.write(Long2ByteArray(crc.getValue()));

        } 
        catch (Exception e) {
            System.out.println("Create Frame Error: " + e);
        }
        
        return frame.toByteArray();
    }

    // public static void main(String[] args) {
    //     byte[] destinationAddress = HexFormat.of().parseHex("AA") ;
    //     byte[] sourceAddress = HexFormat.of().parseHex("00");
    //     byte[] data = HexFormat.of().parseHex("010101010101010101010101010101010101010101010101");

    //     EthernetFrame ethernetFrame = new EthernetFrame(destinationAddress, sourceAddress, data);
    //     byte[] packedFrame = ethernetFrame.pack();

    //     System.out.println("Packed Ethernet Frame: " + byteArrayToString(packedFrame));
    // }
}
