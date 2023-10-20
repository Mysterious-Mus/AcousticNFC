package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;

import java.util.ArrayList;
import java.util.Collections;

import com.AcousticNFC.Config;
import com.AcousticNFC.receive.Receiver;
import com.AcousticNFC.utils.CyclicBuffer;

public class SoFDetector {

    Config cfg;
    
    SoF sof;
    public float[] sofSamples;

    Receiver receiver; // where to take the samples

    public SoFDetector(Config cfg_src, Receiver receiver) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        sofSamples = sof.generateSoFNoSilence();
        // the initial FIW:
        this.receiver = receiver;
    }

    int window = 20;

    /* Calculating Correlations with SoF and see if we can mark the start of a frame */
    public void detect() {
        if (receiver.unpacking) return;

        for (int candidateIdx = receiver.tickDone + 1; 
            candidateIdx <= receiver.getLength() - window; candidateIdx++) {
            if(candidateIdx >= cfg.sofNSamples - 1) {
                // now the correlation is valid
                double corr = 0;
                for (int i = 0; i < cfg.sofNSamples; i++) {
                    corr += receiver.samples.get(candidateIdx-cfg.sofNSamples+1+i) * sofSamples[i];
                }
                corr /= cfg.sofNSamples;
                cfg.UpdCorrdetect(corr);
                if (corr > cfg.SofDetectThreshld) {
                    // found a SoF
                    receiver.unpacking = true;
                    // find the greatest point in the window
                    for (int i = candidateIdx + 1; i < candidateIdx + window; i++) {
                        // calculate new corr
                        double newcorr = 0;
                        for (int j = 0; j < cfg.sofNSamples; j++) {
                            newcorr += receiver.samples.get(i-cfg.sofNSamples+1+j) * sofSamples[j];
                        }
                        newcorr /= cfg.sofNSamples;
                        if (newcorr > corr) {
                            corr = newcorr;
                            candidateIdx = i;
                        }
                    }
                    // print candidateIdx
                    receiver.tickDone = candidateIdx + cfg.sofSilentNSamples;
                    System.out.println("decode start at " + receiver.tickDone + ", SoF signal end at " + candidateIdx);
                    // correction
                    // receiver.tickDone += -90;
                    break;
                }
            }
        }

        receiver.tickDone = Math.max(receiver.getLength() - window, receiver.tickDone);
    }
}
