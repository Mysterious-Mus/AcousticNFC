package com.AcousticNFC.mac;

import java.util.ArrayList;
import java.util.zip.CRC32;
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

public class EthernetFrame {

    /**
     * The Length of a mac frame
     */
    public static int getFrameBitLen() {
        return Host.cfg.packBitLen + 56 + 
                 Host.cfg.alignNSymbol * Host.cfg.keyingCapacity * Host.cfg.numSubCarriers;
    }

    /**
     * return a mac frame in byte array
     * @param destinationAddress
     * @param sourceAddress
     * @param Type : 0x00 for data, 0xFF for ack
     * @param data : payload
     * @return byte array of the frame
     */
    public static byte[] CreateFrame(byte[] destinationAddress, byte[] sourceAddress, byte[] Type, byte[] data) {
        ByteArrayOutputStream frame = new ByteArrayOutputStream( );
        try {

            /// generate aligning header
            // ArrayList<Boolean> AlignData = new ArrayList<Boolean>();
            // int headerLen = Host.cfg.alignNSymbol * Host.cfg.keyingCapacity * Host.cfg.numSubCarriers;
            // for (int i = 0; i < headerLen; i++) {
            //     AlignData.add(Host.cfg.alignBitFunc(i));
            // }
            
            // frame.write(TypeConvertion.booleanListByteArrayTo(AlignData));

            // Destination Address (8 bits)
            frame.write(destinationAddress);
            
            // Source Address (8 bits)
            frame.write(sourceAddress);

            // Type (8 bits)
            frame.write(Type); 

            // Data (800 bits)
            frame.write(data);
            
            // CRC (32 bits)
            CRC32 crc = new CRC32();
            crc.update(data);
            frame.write(TypeConvertion.Long2ByteArray(crc.getValue()));

        } 
        catch (Exception e) {
            System.out.println("Create Frame Error: " + e);
        }
        
        return frame.toByteArray();
    }
     
    /** check CRC
     * @param frame
     */
    public static boolean checkCRC(byte[] frame) {
        // sanity check 
        assert frame.length * 8== getFrameBitLen();

        // get the CRC
        byte[] crc = getCRC(frame);

        // get the data
        byte[] data = getData(frame);

        // calculate the CRC
        CRC32 crc32 = new CRC32();
        crc32.update(data);

        // compare the CRC
        return TypeConvertion.Long2ByteArray(crc32.getValue()) == crc;
    }

    /**
     * get the destinationAddress of a mac frame
     */
    public static byte[] getDestinationAddress(byte[] frame) {
        // sanity check 
        assert frame.length * 8== getFrameBitLen();

        return new byte[] {frame[0]};
    }
    /**
     * get the sourceAddress of a mac frame
     */
    public static byte[] getSourceAddress(byte[] frame) {
        // sanity check 
        assert frame.length * 8== getFrameBitLen();

        return new byte[] {frame[1]};
    }

    /**
     * get the type of a mac frame
    */
    public static byte[] getType(byte[] frame) {
        // sanity check 
        assert frame.length * 8== getFrameBitLen();

        return new byte[] {frame[2]};
    }

    /**
     * get the data of a mac frame
     */
    public static byte[] getData(byte[] frame) {
        // sanity check 
        assert frame.length * 8== getFrameBitLen();
        int dataLen = Math.ceilDiv(Host.cfg.packBitLen, 8);
        byte[] data = new byte[dataLen];
        System.arraycopy(frame, 3, data, 0, dataLen);
        return data;
    }

    /**
     * get the CRC of a mac frame
     */
    public static byte[] getCRC(byte[] frame) {
        // sanity check 
        assert frame.length * 8== getFrameBitLen();
        int dataLen = Math.ceilDiv(Host.cfg.packBitLen, 8);
        byte[] crc = new byte[4];
        System.arraycopy(frame, 3 + dataLen, crc, 0, 4);
        return crc;
    }


}
