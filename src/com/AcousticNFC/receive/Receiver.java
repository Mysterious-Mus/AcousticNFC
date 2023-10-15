package com.AcousticNFC.receive;

import com.AcousticNFC.receive.SoFDetector;
import com.AcousticNFC.utils.FileOp;

public class Receiver {
    
    double sampleRate;
    float[] samples;

    SoFDetector sofDetector;

    public Receiver(double sampleRate) {
        this.sampleRate = sampleRate;
        samples = new float[0];
        sofDetector = new SoFDetector(sampleRate, this);
    }

    public int getLength() {
        return samples.length;
    }

    /* Get samples {i-L+1, ..., i} */
    public float[] getSamples(int i, int L) {
        // Sanity check
        if (i < L - 1) {
            throw new IllegalArgumentException("i must be greater than or equal to L - 1");
        }

        float[] result = new float[L];
        System.arraycopy(samples, i - L + 1, result, 0, L);
        return result;
    }

    /* Add the samples to the receiver */
    public void feedSamples(float[] samples) {
        // add the new samples to back
        float[] newSamples = new float[this.samples.length + samples.length];
        System.arraycopy(this.samples, 0, newSamples, 0, this.samples.length);
        System.arraycopy(samples, 0, newSamples, this.samples.length, samples.length);
        this.samples = newSamples;
    }
    
    /* Do the computation heavy operations */
    public void process() {
        // update the correlations
        sofDetector.updateCorrelations();
    }
    
    public void dumpResults() {
        // update the correlations
        sofDetector.updateCorrelations();
        FileOp fileOp = new FileOp();
        fileOp.outputFloatSeq(samples, "samples.csv");
        fileOp.outputFloatSeq(sofDetector.getCorrelations(), "correlations.csv");
    }
}
