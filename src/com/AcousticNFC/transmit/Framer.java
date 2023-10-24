package com.AcousticNFC.transmit;

import com.AcousticNFC.Config;
import java.util.ArrayList;
import com.AcousticNFC.utils.ECC;

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

    ECC Ecc;

    public Framer(Config cfg_src) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        ofdm = new OFDM(cfg);
        Ecc = new ECC(cfg);
    }

    public float[] pack(ArrayList<Boolean> bitString) {
        // current implementation is for test. Just pack all bits into one frame,
        // truncate if longer, pad if shorter

        ArrayList<Boolean> frameData = new ArrayList<Boolean>();

        // generate aligning header
        int headerLen = cfg.alignNSymbol * cfg.keyingCapacity * cfg.numSubCarriers;
        for (int i = 0; i < headerLen; i++) {
            frameData.add(cfg.alignBitFunc(i));
        }

        // get transmitted sequence and ECC encode
        boolean[] seqForEncode = new boolean[cfg.transmitBitLen];
        for (int bitIdx = 0; bitIdx < cfg.transmitBitLen; bitIdx++) {
            if (bitIdx < bitString.size()) {
                seqForEncode[bitIdx] = bitString.get(bitIdx);
            } else {
                seqForEncode[bitIdx] = false;
            }
        }
        boolean[] encodedSeq = Ecc.ConvolutionEncode(seqForEncode);

        // add into the transmission string
        for (int bitIdx = 0; bitIdx < encodedSeq.length; bitIdx++) {
            frameData.add(encodedSeq[bitIdx]);
        }

        // get SoF and symbols
        float[] sofSamples = sof.generateSoF();
        float[] symbolSamples = ofdm.modulate(frameData);

        // concatenate SoF and symbols
        float[] samples = new float[sofSamples.length + symbolSamples.length];
        System.arraycopy(sofSamples, 0, samples, 0, sofSamples.length);
        System.arraycopy(symbolSamples, 0, samples, sofSamples.length, symbolSamples.length);
        // System.out.println("Packed " + frameData.size() + " bits into " + samples.length + " samples");
        return samples;
    }
}
