package com.AcousticNFC.mac;

import java.util.zip.CRC32;
import java.io.ByteArrayOutputStream;

import com.AcousticNFC.utils.TypeConvertion;


/*
 * IEEE 802.3 based Ethernet Frame 
 * Destination Address: 1 bytes
 * Source Address: 1 bytes
 * Data: 100 bytes
 * Frame Check Sequence: 4 bytes
 */

public class EthernetFrame {


    public static byte[] CreateFrame(byte[] destinationAddress, byte[] sourceAddress, byte[] data) {
        ByteArrayOutputStream frame = new ByteArrayOutputStream( );
        try {
            // Destination Address (8 bits)
            frame.write(destinationAddress);
            
            // Source Address (8 bits)
            frame.write(sourceAddress);
            
            // Data (800 bits)
            frame.write(data);
            
            // CRC (32 bits)
            CRC32 crc = new CRC32();
            crc.update(frame.toByteArray());
            frame.write(TypeConvertion.Long2ByteArray(crc.getValue()));

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
