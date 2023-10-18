package com.AcousticNFC.transmit;

/* Dataframe SoF generator
 * The SoF is a 1ms long sound that is used to indicate the start of a dataframe
 * The SoF is a chirp signal in Dhwani's implementation, the formulation
 * can be found in notes.ipynb
 */
public class SoF {
    double sampleRate;
    double T = 0.002905; // duration of SoF, seconds
    float amplitude = 0.8f;
    // frequencies of SoF
    float fmax = 6000;
    float fmin = 12000;
    int numSamples;

    double silentDuration = 0.004; // seconds
    public int silentNSamples;

    public SoF(double sampleRate) {
        this.sampleRate = sampleRate;
        numSamples = (int)Math.round(2 * T * sampleRate);
        silentNSamples = (int)Math.round(silentDuration * sampleRate);
        // print NSample
        System.out.println("SoF NSample: " + numSamples);
    }

    public float[] generateSoF() {
        float[] samples = new float[numSamples + silentNSamples];
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