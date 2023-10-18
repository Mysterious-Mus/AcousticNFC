package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;

import java.util.ArrayList;

import com.AcousticNFC.Config;
import com.AcousticNFC.receive.Receiver;

public class SoFDetector {

    Config cfg;
    
    SoF sof;
    float[] sofSamples;

    int lastSoFIdx = 0;

    Receiver receiver;
    /* The correlation between the samples and the SoF
     * correlations[i] is the correlation between the samples[i:i+L-1] and the SoF */
    ArrayList<Double> correlations;

    public SoFDetector(Config cfg_src, Receiver receiver) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        correlations = new ArrayList<Double>();
        this.receiver = receiver;
    }

    /* Calculate the correlation between the samples and the SoF
     * If the samples are shorter than SoF, 0s are padded to the end of the samples
     */
    public double correlation(ArrayList<Float> samples, int startIdx) {
        double sum = 0;
        for (int i = startIdx; i < Math.min(startIdx + cfg.sofNSamples, samples.size()); i++) {
            sum += samples.get(i) * sofSamples[i - startIdx];
        }

        return sum / cfg.sofNSamples;
    }

    /* Calculating Correlations with SoF and see if we can mark the start of a frame */
    public void updateCorrelations() {
        // if nothing yet
        if (receiver.getLength() < cfg.sofNSamples) {
            return;
        }

        // record where we were last time
        int startingIdx = correlations.size();
        // this means we've calculated all the correlations of the slices whose
        // starting index is smaller than startingIdx

        // this function is designed to mark 1 SoF at a time
        // we can't go too far in a single run because we might miss the start of a frame
        // when we can calculate more than SofNSample + SofSilentNSamples correlations this time,
        // it means we have the risk of missing the start of a frame, so we should wait.
        // This could happen because the processing speed is not enough for the sampling speed.
        int endIdx = Math.min(receiver.getLength() - cfg.sofNSamples + 1, 
            startingIdx + cfg.sofNSamples + cfg.sofSilentNSamples);

        // calculate the new correlations
        for (int idx = correlations.size(); 
            idx < receiver.getLength() - cfg.sofNSamples + 1; idx++) {
            correlations.add(correlation(receiver.getSamples(), idx));
        }

        // marking the start of a frame
        // we shouldn't be demodulating
        if (!receiver.unpacking) {
            // The task is to locate a zone that contains the exact matching point
            // then find the maximun correlation, done

            // The feature of the matching zone, emperically, is that the correlation
            // would take a sharp rise and decline at once, much sharper than even a sudden loud noise

            // we can't safely mark the starting point of a SoF until we have some correlations
            // behind the point, because when the transmission starts

            if (startingIdx >= waitNSamples) {
                if (correlations.get(startingIdx - waitNSamples) > corrThreshold
                    && startingIdx - lastSoFIdx > sofNSamples + sof.silentNSamples + waitNSamples) {
                    int bestStartingIdx = startingIdx - waitNSamples;
                    for (int i = startingIdx - waitNSamples + 1; i < startingIdx; i++) {
                        if (correlations.get(i) > correlations.get(bestStartingIdx)) {
                            bestStartingIdx = i;
                        }
                    }
                    int endIdx = bestStartingIdx + sofNSamples + sof.silentNSamples;
                    System.out.println("SoF end detected at " + endIdx + ", Starting at " + bestStartingIdx);
                    // send message to start demodulation
                    receiver.unpacking = true;
                    receiver.tickDone = endIdx;
                    lastSoFIdx = endIdx;
                }
            }
        }
    }

    public ArrayList<Double> getCorrelations() {
        return correlations;
    }
}
