package com.AcousticNFC.receive;

import java.util.ArrayList;

import com.AcousticNFC.receive.SoFDetector;
import com.AcousticNFC.Config;
import com.AcousticNFC.receive.Demodulator;
import com.AcousticNFC.utils.FileOp;

public class Receiver {
    
    ArrayList<Float> samples;

    SoFDetector sofDetector;

    public boolean unpacking;
    public int tickDone;
    Demodulator demodulator;

    public ArrayList<Boolean> receiveBuffer;

    Config cfg;

    public Receiver(Config cfg_src) {
        cfg = cfg_src;
        samples = new ArrayList<Float>();
        sofDetector = new SoFDetector(cfg, this);
        unpacking = false;
        tickDone = 0;
        demodulator = new Demodulator(this, cfg);
        receiveBuffer = new ArrayList<Boolean>();
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

        // demodulation
        demodulator.demodulate();
    }
    
    public void dumpResults() {
        // update the correlations
        sofDetector.updateCorrelations();
        FileOp.outputFloatSeq(samples, "samples.csv");
        FileOp.outputDoubleArray(sofDetector.getCorrelations(), "correlations.csv");
    }
}
