package com.AcousticNFC.physical.receive;

import com.AcousticNFC.physical.transmit.SoF;

import java.util.ArrayList;
import java.util.Collections;

import com.AcousticNFC.Config;
import com.AcousticNFC.Host;
import com.AcousticNFC.utils.CyclicBuffer;

public class SoFDetector {

    Config cfg;
    
    SoF sof;
    public float[] sofSamples;



    public SoFDetector(Config cfg_src) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        sofSamples = sof.generateSoFNoSilence();
    }

    int window = 20;

    /**
     *  Calculating Correlations with SoF and see if we can mark the start of a frame 
     *  @return {@code boolean} whether we have found a SoF
     */
    public boolean detect() {
        if (Host.receiver.unpacking || Host.receiver.scanAligning) return false;
        for (int candidateIdx = Host.receiver.tickDone + 1; 
            candidateIdx <= Host.receiver.getLength() - window; candidateIdx++) {
            if(candidateIdx >= cfg.sofNSamples - 1) {
                // now the correlation is valid
                double corr = 0;
                for (int i = 0; i < cfg.sofNSamples; i++) {
                    corr += Host.receiver.samples.get(candidateIdx-cfg.sofNSamples+1+i) * sofSamples[i];
                }
                corr /= cfg.sofNSamples;
                cfg.UpdCorrdetect(corr);
                if (corr > cfg.SofDetectThreshld) {
                    // found a SoF
                    Host.receiver.scanAligning = true;
                    // find the greatest point in the window
                    for (int i = candidateIdx + 1; i < candidateIdx + window; i++) {
                        // calculate new corr
                        double newcorr = 0;
                        for (int j = 0; j < cfg.sofNSamples; j++) {
                            newcorr += Host.receiver.samples.get(i-cfg.sofNSamples+1+j) * sofSamples[j];
                        }
                        newcorr /= cfg.sofNSamples;
                        if (newcorr > corr) {
                            corr = newcorr;
                            candidateIdx = i;
                        }
                    }
                    // print candidateIdx
                    Host.receiver.tickDone = candidateIdx + cfg.sofSilentNSamples + cfg.sofAlignCompensate;
                    return true;
                }
            }
        }

        Host.receiver.tickDone = Math.max(Host.receiver.getLength() - window, Host.receiver.tickDone);
        return false;
    }
}
