package com.AcousticNFC.transmit;

/* Frame Protocol:
 * 1. SoF
 * 2. Length of bit string: 32 bits
 * 3. Bit string
 */
public class Framer {
    
    double sampleRate;

    SoF sof;
    OFDM ofdm;

    public Framer(double sampleRate) {
        this.sampleRate = sampleRate;

        sof = new SoF(sampleRate);
        ofdm = new OFDM(sampleRate);
    }

    public float[] pack(int[] bitString) {
        // concatenate the length of the bit string
        int[] length = new int[32];
        int[] bitStringWithLength = new int[bitString.length + length.length];
        for(int bitIdx = 0; bitIdx < length.length; bitIdx++) {
            length[bitIdx] = (bitString.length >> bitIdx) & 1;
        }
        System.arraycopy(length, 0, bitStringWithLength, 0, length.length);
        System.arraycopy(bitString, 0, bitStringWithLength, length.length, bitString.length);

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
