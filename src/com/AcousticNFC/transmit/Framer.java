package com.AcousticNFC.transmit;

import com.AcousticNFC.Config;

/* Frame Protocol:
 * 1. SoF
 * 2. Length of bit string: 32 bits
 * 3. Bit string
 */
public class Framer {
    
    double sampleRate;

    SoF sof;
    OFDM ofdm;

    Config cfg;

    public Framer(Config cfg_src) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        ofdm = new OFDM(cfg);
    }

    /* Pack a dataframe containing FRAMELENGTH bits
     * padding 0s if the bit string is shorter than FRAMELENGTH
     * truncate the bit string if it is longer than FRAMELENGTH
     */
    public float[] pack(int[] bitString) {
        // fix the length of the bit string
        if (bitString.length > cfg.frameLength) {
            int[] newBitString = new int[cfg.frameLength];
            System.arraycopy(bitString, 0, newBitString, 0, cfg.frameLength);
            bitString = newBitString;
        } else if (bitString.length < cfg.frameLength) {
            int[] newBitString = new int[cfg.frameLength];
            System.arraycopy(bitString, 0, newBitString, 0, bitString.length);
            bitString = newBitString;
        }

        // get SoF and symbols
        float[] sofSamples = sof.generateSoF();
        float[] symbolSamples = ofdm.modulate(bitString);

        // concatenate SoF and symbols
        float[] samples = new float[sofSamples.length + symbolSamples.length];
        System.arraycopy(sofSamples, 0, samples, 0, sofSamples.length);
        System.arraycopy(symbolSamples, 0, samples, sofSamples.length, symbolSamples.length);

        return samples;
    }
}
