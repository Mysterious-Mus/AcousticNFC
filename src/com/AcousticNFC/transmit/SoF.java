package com.AcousticNFC.transmit;

/* Dataframe SoF generator
 * The SoF is a 1ms long sound that is used to indicate the start of a dataframe
 * The SoF is a chirp signal that starts at 18kHz and ends at 22kHz
 */
public class SoF {
    double sampleRate;
    double duration = 0.001; // duration of SoF, seconds
    // frequencies of SoF
    float freq_start = 18000;
    float freq_end = 22000;

    public SoF(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public float[] generateSoF() {
        int numSamples = (int) (duration * sampleRate);
        float[] samples = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            float time = (float) i / (float) sampleRate;
            float phase = (float) (2 * Math.PI * 
                (freq_start * time + 
                (freq_end - freq_start) / (2 * duration) * time * time ));
            samples[i] = (float) Math.sin(phase);
        }
        return samples;
    }
}