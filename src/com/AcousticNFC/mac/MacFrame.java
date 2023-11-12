package com.AcousticNFC.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.print.DocFlavor.BYTE_ARRAY;

import java.io.ByteArrayOutputStream;

import com.AcousticNFC.Config;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.CRC8;


/*
 * IEEE 802.3 based Ethernet Frame 
 * Destination Address: 1 bytes
 * Source Address: 1 bytes
 * Type: 1 bytes
 * Data: 100 bytes
 * Frame Check Sequence: 4 bytes
 */

public class MacFrame {

    public static class Configs {
        public static int addrLb = 0;
        public static int addrUb = 255;

        public static enum HeaderFields {
            DEST_ADDR,
            SRC_ADDR,
            TYPE,
            CRC8,
            COUNT
        }

        public static int payloadNumBytes = 80;

        public static enum Types {
            DATA((byte) 0x00),
            ACK((byte) 0xFF);

            private byte value;

            private Types(byte value) {
                this.value = value;
            }

            public byte getValue() {
                return value;
            }

            public static Types fromByte(byte b) {
                for (Types type : Types.values()) {
                    if (type.getValue() == b) {
                        return type;
                    }
                }
                // print error
                System.out.println("Error: type not found");
                return null;
            }
        }
    }

    public static class Header {

        private byte[] contents = new byte[Configs.HeaderFields.COUNT.ordinal()];

        public Header() {
        }

        public void SetField(Configs.HeaderFields field, Byte value) {
            contents[field.ordinal()] = value;
        }

        public byte[] stream() {
            // calc CRC8
            SetField(Configs.HeaderFields.CRC8, 
                CRC8.compute(Arrays.copyOfRange(contents, 0, Configs.HeaderFields.COUNT.ordinal() - 1)));
            return contents;
        }

        public boolean check() {
            return CRC8.compute(Arrays.copyOfRange(contents, 0, Configs.HeaderFields.COUNT.ordinal() - 1)) 
                == contents[Configs.HeaderFields.CRC8.ordinal()];
        }
    }

    private byte[] wholeContents; // everything including header/crc
    private Header header;

    /**
     * Create the frame to be sent
     * @param header
     * @param data
     */
    public MacFrame (Header header, byte[] data) {
        this.header = header;
        ByteArrayOutputStream stream = new ByteArrayOutputStream( );
        try {
            stream.write(header.stream());
            CRC32 crc = new CRC32();
            crc.update(data);
            stream.write(data);
            stream.write(TypeConvertion.Long2ByteArray(crc.getValue()));
            
            this.wholeContents = stream.toByteArray();
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
        // sanity: length check
        if (frameBuffer.length != getFrameBitLen() / 8) {
            System.out.println("Error: frame length not match");
            return;
        }
        this.wholeContents = frameBuffer;
        this.header = new Header();
        for (Configs.HeaderFields field : Configs.HeaderFields.values()) {
            if (field == Configs.HeaderFields.COUNT) {
                continue;
            }
            this.header.SetField(field, frameBuffer[field.ordinal()]);
        }
    }

    public MacFrame (ArrayList<Boolean> frameBuffer) {
        this(TypeConvertion.booleanList2ByteArray(frameBuffer));
    }

    /**
     * The Length of a mac frame
     */
    public static int getFrameBitLen() {
        return Configs.HeaderFields.COUNT.ordinal() * 8 + Configs.payloadNumBytes * 8 + 32;
    }
     
    /** check CRC
     * @return true if CRC is correct
     */
    public boolean verify() {
        // CRC (32 bits)
        CRC32 crc = new CRC32();
        crc.update(Arrays.copyOfRange(this.wholeContents, Configs.HeaderFields.COUNT.ordinal(), this.wholeContents.length - 4));
        return crc.getValue() == TypeConvertion.byteArray2Long(
            Arrays.copyOfRange(this.wholeContents, this.wholeContents.length - 4, this.wholeContents.length))
            && this.header.check();
    }

    public byte[] getWhole() {
        return wholeContents;
    }

    public byte[] getData() {
        return Arrays.copyOfRange(this.wholeContents, Configs.HeaderFields.COUNT.ordinal(), this.wholeContents.length - 4);
    }
}
