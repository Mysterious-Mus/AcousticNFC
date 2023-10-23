package com.AcousticNFC.transmit;

import com.AcousticNFC.Config;
import java.util.ArrayList;

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

    public float[] pack(ArrayList<Boolean> bitString) {
        // current implementation is for test. Just pack all bits into one frame,
        // truncate if longer, pad if shorter

        ArrayList<Boolean> frameData = new ArrayList<Boolean>();

        // generate aligning header
        int headerLen = cfg.alignNSymbol * cfg.keyingCapacity * cfg.numSubCarriers;
        for (int i = 0; i < headerLen; i++) {
            frameData.add(i % 2 == 0);
        }

        for (int i = headerLen; i < cfg.frameLength; i++) {
            frameData.add(i < bitString.size() ? bitString.get(i) : false);
        }

        // get SoF and symbols
        float[] sofSamples = sof.generateWarmupSoF();
        float[] symbolSamples = ofdm.modulate(frameData);

        // concatenate SoF and symbols
        float[] samples = new float[sofSamples.length + symbolSamples.length];
        System.arraycopy(sofSamples, 0, samples, 0, sofSamples.length);
        System.arraycopy(symbolSamples, 0, samples, sofSamples.length, symbolSamples.length);
        // System.out.println("Packed " + frameData.size() + " bits into " + samples.length + " samples");
        return samples;
    }
}
