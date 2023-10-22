package com.AcousticNFC.receive;

import java.util.ArrayList;

import com.AcousticNFC.Config;
import com.AcousticNFC.utils.FileOp;

public class Receiver {
    
    ArrayList<Float> samples;

    SoFDetector sofDetector;

    public boolean unpacking; // the receiver is either unpacking or waiting for SoF

    // when the receiver is waiting for SoF, tickDone means that we have checked
    // all the indices before and including tickDone can't be the exact end of a SoF
    // when the receiver is unpacking, tickDone means that we have unpacked all the
    // symbols before and including tickDone
    public int tickDone;

    Demodulator demodulator;

    public ArrayList<Boolean> receiveBuffer;

    Config cfg;

    public Receiver(Config cfg_src) {
        cfg = cfg_src;
        samples = new ArrayList<Float>();
        tickDone = cfg.sofNSamples - 1;  // can't be sure these points are SoF ends
        sofDetector = new SoFDetector(cfg, this);
        unpacking = false;
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

    public float getSample(int idx) {
        return samples.get(idx);
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
        sofDetector.detect();

        // demodulation
        demodulator.demodulate();
    }
    
    public void dumpResults() {
        FileOp.outputFloatSeq(samples, "samples.csv");
    }
}
