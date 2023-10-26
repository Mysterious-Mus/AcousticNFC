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
        boolean[] encodedSeq = cfg.ECCOn? Ecc.ConvolutionEncode(seqForEncode) : seqForEncode;

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

    public float[] frame(ArrayList<Boolean> bitString) {
        // calculate how many frames are needed
        int numFrames = (int) Math.ceil((double) cfg.frameLength / cfg.packBitLen);

        // the final playBuffer
        ArrayList<Float> playBuffer = new ArrayList<Float>();

        // pack each pack
        for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
            // get the bit string to pack
            ArrayList<Boolean> bitStringToPack = new ArrayList<Boolean>();
            for (int bitIdx = 0; bitIdx < cfg.packBitLen; bitIdx++) {
                if (frameIdx * cfg.packBitLen + bitIdx < bitString.size()) {
                    bitStringToPack.add(bitString.get(frameIdx * cfg.packBitLen + bitIdx));
                } else {
                    bitStringToPack.add(false);
                }
            }

            // pack the bit string
            float[] samples = pack(bitStringToPack);

            // add to the playBuffer
            for (int sampleIdx = 0; sampleIdx < samples.length; sampleIdx++) {
                playBuffer.add(samples[sampleIdx]);
            }
        }

        // convert to float[] and return
        float[] playBufferFloat = new float[playBuffer.size()];
        for (int sampleIdx = 0; sampleIdx < playBuffer.size(); sampleIdx++) {
            playBufferFloat[sampleIdx] = playBuffer.get(sampleIdx);
        }
        return playBufferFloat;
    }
}
