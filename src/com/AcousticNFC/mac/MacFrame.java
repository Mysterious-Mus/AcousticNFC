package com.AcousticNFC.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.print.DocFlavor.BYTE_ARRAY;

import java.io.ByteArrayOutputStream;

import com.AcousticNFC.Config;
import com.AcousticNFC.Host;
import com.AcousticNFC.utils.TypeConvertion;


/*
 * IEEE 802.3 based Ethernet Frame 
 * Destination Address: 1 bytes
 * Source Address: 1 bytes
 * Type: 1 bytes
 * Data: 100 bytes
 * Frame Check Sequence: 4 bytes
 */

public class MacFrame {


    public byte[] frame;
    public byte destinationAddress;
    public byte sourceAddress;
    public byte type;
    /**
     * Data field in the frame (100 bytes)
     */
    public byte[] data;
    /**
     * CRC field in the frame (4 bytes)
     */
    public Long CRC;
    /**
     * True if CRC is correct
     */
    public Boolean is_valid;
    /**
     * Construct for sender 
     * autogenerate CRC 
     * @param destinationAddress 1 byte
     * @param sourceAddress 1 byte
     * @param Type : 1 byte 0x00 for data, 0xFF for ack
     * @param data : payload
     */
    public MacFrame (byte destinationAddress, byte sourceAddress, byte type, byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream( );
        try {

            // Destination Address (8 bits)
            stream.write(destinationAddress);
            
            // Source Address (8 bits)
            stream.write(sourceAddress);

            // Type (8 bits)
            stream.write(type); 

            // Data (800 bits)
            stream.write(data);
            
            
            // CRC (32 bits)
            CRC32 crc = new CRC32();
            crc.update(data);
            stream.write(TypeConvertion.Long2ByteArray(crc.getValue()));
            
            this.frame = stream.toByteArray();
            this.destinationAddress = destinationAddress;
            this.sourceAddress = sourceAddress;
            this.type = type;
            this.data = data;
            this.CRC = crc.getValue();
            this.is_valid = true;
        } 
        catch (Exception e) {
            System.out.println("Create Frame Error: " + e);
        }
        
    }

    /** 
     * Construct for receiver. Auto check CRC value, and the result stored in {@code is_valid}  
     * @param array: byte array
     */
    public MacFrame (byte[] frameBuffer) {
        this.frame = frameBuffer;

        int preambleLen = 0;

        // Destination Address (8 bits)
        this.destinationAddress = frameBuffer[preambleLen++];

        // Source Address (8 bits)
        this.sourceAddress = frameBuffer[preambleLen++];

        // Type (8 bits)
        this.type = frameBuffer[preambleLen++];

        // Data (800 bits)
        int dataLen = Math.ceilDiv(Config.packBitLen, Byte.SIZE);
        this.data = Arrays.copyOfRange(frameBuffer, preambleLen, dataLen + preambleLen);

        // CRC (32 bits)
        this.CRC = TypeConvertion.byteArray2Long(
            Arrays.copyOfRange(frameBuffer, preambleLen + dataLen, preambleLen + dataLen + Long.BYTES / 2)
        );

        // check CRC
        this.is_valid = checkCRC();
    }

    public MacFrame (ArrayList<Boolean> frameBuffer) {
        this(TypeConvertion.booleanList2ByteArray(frameBuffer));
    }

    /**
     * The Length of a mac frame
     */
    public static int getFrameBitLen() {
        return Config.packBitLen + Byte.SIZE * 3 + Long.SIZE / 2;
    }
     
    /** check CRC
     * @return true if CRC is correct
     */
    public boolean checkCRC() {
        // CRC (32 bits)
        CRC32 crc = new CRC32();
        crc.update(this.data);
        return crc.getValue() == this.CRC;     
    }

}
