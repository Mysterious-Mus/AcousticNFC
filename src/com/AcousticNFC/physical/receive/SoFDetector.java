package com.AcousticNFC.physical.receive;

import com.AcousticNFC.physical.transmit.SoF;

import java.util.ArrayList;
import java.util.Collections;

import com.AcousticNFC.Config;
import com.AcousticNFC.Host;
import com.AcousticNFC.utils.CyclicBuffer;

public class SoFDetector {

    SoF sof;
    public float[] sofSamples;



    public SoFDetector() {
        sof = new SoF();
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
            if(candidateIdx >= Config.sofNSamples - 1) {
                // now the correlation is valid
                double corr = 0;
                for (int i = 0; i < Config.sofNSamples; i++) {
                    corr += Host.receiver.samples.get(candidateIdx-Config.sofNSamples+1+i) * sofSamples[i];
                }
                corr /= Config.sofNSamples;
                Config.UpdCorrdetect(corr);
                if (corr > Config.SofDetectThreshld) {
                    // found a SoF
                    Host.receiver.scanAligning = true;
                    // find the greatest point in the window
                    for (int i = candidateIdx + 1; i < candidateIdx + window; i++) {
                        // calculate new corr
                        double newcorr = 0;
                        for (int j = 0; j < Config.sofNSamples; j++) {
                            newcorr += Host.receiver.samples.get(i-Config.sofNSamples+1+j) * sofSamples[j];
                        }
                        newcorr /= Config.sofNSamples;
                        if (newcorr > corr) {
                            corr = newcorr;
                            candidateIdx = i;
                        }
                    }
                    // print candidateIdx
                    Host.receiver.tickDone = candidateIdx + Config.sofSilentNSamples + Config.sofAlignCompensate;
                    return true;
                }
            }
        }

        Host.receiver.tickDone = Math.max(Host.receiver.getLength() - window, Host.receiver.tickDone);
        return false;
    }
}
