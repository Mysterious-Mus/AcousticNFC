package com.AcousticNFC.receive;

import java.util.ArrayList;

import com.AcousticNFC.receive.SoFDetector;
import com.AcousticNFC.utils.FileOp;

public class Receiver {
    
    double sampleRate;
    ArrayList<Float> samples;

    SoFDetector sofDetector;

    public Receiver(double sampleRate) {
        this.sampleRate = sampleRate;
        samples = new ArrayList<Float>();
        sofDetector = new SoFDetector(sampleRate, this);
    }

    public int getLength() {
        return samples.size();
    }

    /* Get samples */
    public ArrayList<Float> getSamples() {
        return samples;
    }

    /* Add the samples to the receiver */
    public void feedSamples(float[] samples) {
        // add the new samples to back
        for (int i = 0; i < samples.length; i++) {
            this.samples.add(samples[i]);
        }
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
        fileOp.outputDoubleArray(sofDetector.getCorrelations(), "correlations.csv");
    }
}
