package com.AcousticNFC.physical.transmit;

import com.AcousticNFC.Config;
import com.AcousticNFC.utils.CyclicBuffer;

import java.util.ArrayList;

/* Dataframe SoF generator
 * The SoF is a 1ms long sound that is used to indicate the start of a dataframe
 * The SoF is a chirp signal in Dhwani's implementation, the formulation
 * can be found in notes.ipynb
 */
public class SoF {

    // int warmUpL = 10;
    // public float[] generateWarmupSoF() {
    //     float[] withWarmup = new float[warmUpL + Config.sofNSamples + Config.sofSilentNSamples];

    //     // the warmUp section should be a sine wave, with lenght Config.sofNSamples
    //     for (int idx = 0; idx < warmUpL; idx ++) {
    //         withWarmup[idx] = 0.8f * (float) Math.sin(2 * Math.PI * 8000 * idx / Config.sampleRate);
    //     }

    //     float[] samplesNoSilence = generateSoFNoSilence();
    //     System.arraycopy(samplesNoSilence, 0, withWarmup, warmUpL, Config.sofNSamples);
    //     return withWarmup;
    // }

    public static float[] generateSoF() {
        float[] samples = new float[Config.sofNSamples + Config.sofSilentNSamples];
        
        float[] samplesNoSilence = generateSoFNoSilence();
        System.arraycopy(samplesNoSilence, 0, samples, 0, Config.sofNSamples);
        return samples;
    }

    public static float[] generateSoFNoSilence() {
        float[] samples = new float[Config.sofNSamples];
        float a = (float)((Config.SoF_fmax - Config.SoF_fmin) / Config.SoF_T);
        float phi0 = (float)(Math.PI * a * Config.SoF_T * Config.SoF_T);
        // stage 1
        for (int i = 0; i < Config.sofNSamples / 2; i++) {
            float time = (float) i / (float) Config.sofNSamples;
            float phase = (float) (Math.PI * a * time * time);
            samples[i] = Config.SoF_amplitude * (float) Math.cos(phase);
        }
        // stage 2
        for (int i = Config.sofNSamples / 2; i < Config.sofNSamples; i++) {
            float t = (float) i / (float) Config.sampleRate;
            float phase = (float) (phi0 + Config.SoF_fmax*(t-Config.SoF_T) - Math.PI * a * (t-Config.SoF_T) * (t-Config.SoF_T));
            samples[i] = Config.SoF_amplitude * (float) Math.cos(phase);
        }
        return samples;
    }

    public static int NSample() {
        return (int) (2 * Config.SoF_T * Config.SoF_amplitude);
    }

    public static ArrayList<Boolean> alignBits() {
        ArrayList<Boolean> bits = new ArrayList<Boolean>();
        for (int i = 0; i < Config.alignBitLen; i++) {
            bits.add(Config.alignBitFunc(i));
        }
        return bits;
    }

    public static double calcCorr(ArrayList<Float> buffer, int startIdx) {
        double corr = 0;
        for (int i = 0; i < Config.sofNSamples; i++) {
            corr += buffer.get(startIdx+i) * Config.SofNoSilence[i];
        }
        corr /= Config.sofNSamples;
        return corr;
    }

    public static double calcCorr(CyclicBuffer<Float> buffer, int startIdx) {
        double corr = 0;
        for (int i = 0; i < Config.sofNSamples; i++) {
            corr += buffer.get(startIdx+i) * Config.SofNoSilence[i];
        }
        corr /= Config.sofNSamples;
        Config.UpdCorrdetect(corr);
        return corr;
    }
}