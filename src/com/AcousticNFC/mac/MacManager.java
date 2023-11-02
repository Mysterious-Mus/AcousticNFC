package com.AcousticNFC.mac;

import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat.Type;

import com.AcousticNFC.Config;
import com.AcousticNFC.physical.transmit.EthernetPacket;
import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.TypeConvertion;


public class MacManager {

    Config cfg;

    public MacManager(Config cfg_src) {
        cfg = cfg_src;
    }


    public byte[][] distribute( ArrayList<Boolean> bitString) {
        /* break the large data into frames */
        byte[] input = TypeConvertion.booleanListByteArrayTo(bitString);

        int payloadlen = (int) cfg.packBitLen / 8;
        int frameNum = (input.length + payloadlen - 1) / payloadlen;
         
        byte[][] frames = new byte[frameNum][payloadlen];

        byte[] destinationAddress = new byte[] {0x00};
        byte[] sourceAddress = new byte[] {0x00};

        for (int i = 0; i < frameNum; i++) {
            int start = i * payloadlen;
            int end = Math.min(start + payloadlen, input.length);
            // Copy the input bytes to the frame array
            System.arraycopy(input, start, frames[i], 0, end - start);
            
            // pad the data with 0s
            if (end - start < payloadlen) {
                Arrays.fill(frames[i], end - start, payloadlen, (byte) 0);
            }
            // Add mac header
            frames[i] = EthernetFrame.CreateFrame(destinationAddress, sourceAddress, frames[i]);
        }

        return frames;
    }

    public float[] send( ArrayList<Boolean> bitString) {
        /* Transmit a boolean bitstring 
         * break into few pieces and then palyback
        */
        byte[][] frames = distribute(bitString);



        EthernetPacket ethernetPacket = new EthernetPacket(cfg);
        ArrayList <Float> playContent = new ArrayList <Float> ();

        for (int frameID = 0; frameID < frames.length; frameID++) {
            // physical Layer
            float[] packet = ethernetPacket.getPacket(frames[frameID]);
            for (float sample : packet) {
                playContent.add(sample);
            }
        }
        return TypeConvertion.floatListtoFloatarray(playContent);
    }
    public static void main(String[] args) {
        System.out.println("Hello World!");
        
    }
}
