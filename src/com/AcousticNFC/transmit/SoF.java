package com.AcousticNFC.transmit;

import com.AcousticNFC.Config;

/* Dataframe SoF generator
 * The SoF is a 1ms long sound that is used to indicate the start of a dataframe
 * The SoF is a chirp signal in Dhwani's implementation, the formulation
 * can be found in notes.ipynb
 */
public class SoF {

    Config cfg;

    public SoF(Config cfg_src) {
        cfg = cfg_src;
    }

    public float[] generateSoF() {
        float[] samples = new float[cfg.sofNSamples + cfg.sofSilentNSamples];
        
        float[] samplesNoSilence = generateSoFNoSilence();
        System.arraycopy(samplesNoSilence, 0, samples, cfg.sofSilentNSamples, cfg.sofNSamples);
        return samples;
    }

    public float[] generateSoFNoSilence() {
        float[] samples = new float[cfg.sofNSamples];
        float a = (float)((cfg.SoF_fmax - cfg.SoF_fmin) / cfg.SoF_T);
        float phi0 = (float)(Math.PI * a * cfg.SoF_T * cfg.SoF_T);
        // stage 1
        for (int i = 0; i < cfg.sofNSamples / 2; i++) {
            float time = (float) i / (float) cfg.sofNSamples;
            float phase = (float) (Math.PI * a * time * time);
            samples[i] = cfg.SoF_amplitude * (float) Math.cos(phase);
        }
        // stage 2
        for (int i = cfg.sofNSamples / 2; i < cfg.sofNSamples; i++) {
            float t = (float) i / (float) cfg.sampleRate;
            float phase = (float) (phi0 + cfg.SoF_fmax*(t-cfg.SoF_T) - Math.PI * a * (t-cfg.SoF_T) * (t-cfg.SoF_T));
            samples[i] = cfg.SoF_amplitude * (float) Math.cos(phase);
        }
        return samples;
    }

    public int NSample() {
        return (int) (2 * cfg.SoF_T * cfg.SoF_amplitude);
    }
}