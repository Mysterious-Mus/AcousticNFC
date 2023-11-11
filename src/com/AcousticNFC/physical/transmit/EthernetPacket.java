package com.AcousticNFC.physical.transmit;

import com.AcousticNFC.Config;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.stream.DoubleStream;

import com.AcousticNFC.utils.ECC;
import com.AcousticNFC.utils.TypeConvertion;

/* Frame Protocol:
 * 1. SoF
 * 2. Length of bit string: 32 bits
 * 3. Bit string
 */
public class EthernetPacket {
    
    double sampleRate;

    SoF sof;
    OFDM ofdm;

    public EthernetPacket() {
        sof = new SoF();
        ofdm = new OFDM();
    }
    /**
     * Get the packet to be sent to 
     * Recieve a mac frame
     * @param MacFrame
     * @return {@code float[]} packet samples to physical layer
     */
    public static float[] getPacket(byte[] MacFrame) {
        /* 
        * Input: frame from mac layer
        * Modulate the packet using OFDM (byte[] -> float[])
        * Add preamble and SOF filed 
        * Add interpacket gap field
        * Output: packet to physical layer
        */

        ArrayList<Boolean> MacFrameBits = TypeConvertion.byteArray2BooleanList(MacFrame);
        ArrayList<Boolean> alignBits = SoF.alignBits();
        ArrayList<Boolean> bitsTransmit =TypeConvertion.concatList(alignBits, MacFrameBits);

        // modulate the MacFrame 
        float[] MacFrameSamples = OFDM.modulate(bitsTransmit);

        // add preamble and SoF
        float[] SoFSamples = SoF.generateSoF();

        // add interpacket gap
        float[] interPacketGapSamples = new float[Config.interPacketGapNSamples];

        // concatenate all the samples
        float[] packetSamples = new float[SoFSamples.length + MacFrameSamples.length + interPacketGapSamples.length];
        System.arraycopy(SoFSamples, 0, packetSamples, 0, SoFSamples.length);
        System.arraycopy(MacFrameSamples, 0, packetSamples, SoFSamples.length, MacFrameSamples.length);
        System.arraycopy(interPacketGapSamples, 0, packetSamples, SoFSamples.length + MacFrameSamples.length, interPacketGapSamples.length);

        return packetSamples;

    } 

    // public float[] frame(ArrayList<Boolean> bitString) {
    //     // calculate how many frames are needed
    //     int numFrames = (int) Math.ceil((double) Config.transmitBitLen / Config.packBitLen);

    //     // the final playBuffer
    //     ArrayList<Float> playBuffer = new ArrayList<Float>();

    //     // pack each pack
    //     for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
    //         // get the bit string to pack
    //         ArrayList<Boolean> bitStringToPack = new ArrayList<Boolean>();
    //         for (int bitIdx = 0; bitIdx < Config.packBitLen; bitIdx++) {
    //             if (frameIdx * Config.packBitLen + bitIdx < Config.transmitBitLen) {
    //                 bitStringToPack.add(bitString.get(frameIdx * Config.packBitLen + bitIdx));
    //             } else {
    //                 bitStringToPack.add(false);
    //             }
    //         }

    //         // pack the bit string
    //         float[] samples = pack(bitStringToPack);

    //         // add to the playBuffer
    //         for (int sampleIdx = 0; sampleIdx < samples.length; sampleIdx++) {
    //             playBuffer.add(samples[sampleIdx]);
    //         }
    //     }

    //     // convert to float[] and return
    //     float[] playBufferFloat = new float[playBuffer.size()];
    //     for (int sampleIdx = 0; sampleIdx < playBuffer.size(); sampleIdx++) {
    //         playBufferFloat[sampleIdx] = playBuffer.get(sampleIdx);
    //     }
    //     return playBufferFloat;
    // }
}
