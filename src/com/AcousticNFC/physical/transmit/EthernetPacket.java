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

    Config cfg;

    public EthernetPacket(Config cfg_src) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        ofdm = new OFDM(cfg);
    }
    /**
     * Get the packet to be sent to 
     * Recieve a mac frame
     * @param MacFrame
     * @return {@code float[]} packet samples to physical layer
     */
    public float[] getPacket(byte[] MacFrame) {
        /* 
        * Input: frame from mac layer
        * Modulate the packet using OFDM (byte[] -> float[])
        * Add preamble and SOF filed 
        * Add interpacket gap field
        * Output: packet to physical layer
        */

        // modulate the MacFrame 
        float[] MacFrameSamples = ofdm.modulate(TypeConvertion.byteArrayToBooleanList(MacFrame));

        // add preamble and SoF
        float[] SoFSamples = sof.generateSoF();

        // add interpacket gap
        float[] interPacketGapSamples = new float[cfg.interPacketGapNSamples];

        // concatenate all the samples
        float[] packetSamples = new float[SoFSamples.length + MacFrameSamples.length + interPacketGapSamples.length];
        System.arraycopy(SoFSamples, 0, packetSamples, 0, SoFSamples.length);
        System.arraycopy(MacFrameSamples, 0, packetSamples, SoFSamples.length, MacFrameSamples.length);
        System.arraycopy(interPacketGapSamples, 0, packetSamples, SoFSamples.length + MacFrameSamples.length, interPacketGapSamples.length);

        return packetSamples;

    } 

    // public float[] frame(ArrayList<Boolean> bitString) {
    //     // calculate how many frames are needed
    //     int numFrames = (int) Math.ceil((double) cfg.transmitBitLen / cfg.packBitLen);

    //     // the final playBuffer
    //     ArrayList<Float> playBuffer = new ArrayList<Float>();

    //     // pack each pack
    //     for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
    //         // get the bit string to pack
    //         ArrayList<Boolean> bitStringToPack = new ArrayList<Boolean>();
    //         for (int bitIdx = 0; bitIdx < cfg.packBitLen; bitIdx++) {
    //             if (frameIdx * cfg.packBitLen + bitIdx < cfg.transmitBitLen) {
    //                 bitStringToPack.add(bitString.get(frameIdx * cfg.packBitLen + bitIdx));
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
