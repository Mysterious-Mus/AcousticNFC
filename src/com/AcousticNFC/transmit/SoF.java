package com.AcousticNFC.transmit;

/* Dataframe SoF generator
 * The SoF is a 1ms long sound that is used to indicate the start of a dataframe
 * The SoF is a chirp signal in Dhwani's implementation, the formulation
 * can be found in notes.ipynb
 */
public class SoF {
    double sampleRate;
    double T = 0.003; // duration of SoF, seconds
    float amplitude = 0.7f;
    // frequencies of SoF
    float fmax = 16000;
    float fmin = 6000;
    int numSamples;

    public SoF(double sampleRate) {
        this.sampleRate = sampleRate;
        numSamples = (int) (2 * T * sampleRate);
        // print NSample
        System.out.println("SoF NSample: " + numSamples);
    }

    public float[] generateSoF() {
        float[] samples = new float[numSamples];
        float a = (float)((fmax - fmin) / T);
        float phi0 = (float)(Math.PI * a * T * T);
        // stage 1
        for (int i = 0; i < numSamples / 2; i++) {
            float time = (float) i / (float) sampleRate;
            float phase = (float) (Math.PI * a * time * time);
            samples[i] = amplitude * (float) Math.cos(phase);
        }
        // stage 2
        for (int i = numSamples / 2; i < numSamples; i++) {
            float t = (float) i / (float) sampleRate;
            float phase = (float) (phi0 + fmax*(t-T) - Math.PI * a * (t-T) * (t-T));
            samples[i] = amplitude * (float) Math.cos(phase);
        }
        return samples;
    }

    public int NSample() {
        return (int) (2 * T * sampleRate);
    }
}