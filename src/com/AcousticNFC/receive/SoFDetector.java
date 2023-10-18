package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;

import java.util.ArrayList;
import java.util.Collections;

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
        sofSamples = sof.generateSoF();
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
        int startingIdx = correlations.size(); // inclusive
        // this means we've calculated all the correlations of the slices whose
        // starting index is smaller than startingIdx

        // this function is designed to mark 1 SoF at a time
        // we can't go too far in a single run because we might miss the start of a frame
        // when we can calculate more than SofNSample + SofSilentNSamples correlations this time,
        // it means we have the risk of missing the start of a frame, so we should wait.
        // This could happen because the processing speed is not enough for the sampling speed.
        int endIdx = Math.min(receiver.getLength() - cfg.sofNSamples + 1, 
            startingIdx + cfg.sofNSamples + cfg.sofSilentNSamples); // exclusive

        // calculate the new correlations
        for (int idx = correlations.size(); 
            idx < endIdx; idx++) {
            correlations.add(correlation(receiver.getSamples(), idx));
        }

        // marking the start of a frame
        // we shouldn't be demodulating
        if (!receiver.unpacking) {
            // The task is to locate a zone that contains the exact matching point
            // then find the maximun correlation, done

            // The feature of the matching zone, emperically, is that the correlation
            // would take a sharp rise and decline at once, much sharper than even a sudden loud noise

            // We should judge such a sharp peak by its quick rise

            // Due to the jittering nature of correlation (positively or negatively correlated),
            // we smooth the correlation with a window taking maximum. A great change between
            // neighbouring windows indicates the presence of a SoF.

            // Where should we start the detection?
            // Last time, the last corr calculated was at startingIdx - 1
            // So, every window before startingIdx - 1 - sofDetectWindowLen is already detected for SoF
            // The last window is not detected because the peak may appear after the window

            if (startingIdx - 1 - 3 * cfg.sofDetectWindowLen< 2000) {
                // we don't have enough data to detect SoF
                return;
            }

            int formerWindowEnd = startingIdx - 2 * cfg.sofDetectWindowLen; // exclusive
            double formerWindowMax = Collections.max(correlations.subList(
                formerWindowEnd - cfg.sofDetectWindowLen, formerWindowEnd));
            // See the following avaliable windows
            while (formerWindowEnd + cfg.sofDetectWindowLen <= endIdx - cfg.sofDetectWindowLen) {
                double newWindowMax = Collections.max(correlations.subList(
                    formerWindowEnd, formerWindowEnd + cfg.sofDetectWindowLen));
                if (newWindowMax / formerWindowMax> cfg.sofDetectWindowSensitivity) {
                    // We sense a sharp decline, find the exact peak
                    // the starting point of the peak scan
                    int peakIdx = formerWindowEnd;
                    // the ending point of the peak scan is the end of this window
                    for (int i = peakIdx + 1; i < formerWindowEnd + 2 * cfg.sofDetectWindowLen; i++) {
                        if (correlations.get(i) > correlations.get(peakIdx)) {
                            peakIdx = i;
                        }
                    }
                    // the corr peak found is the starting point of the SoF, calculate the end
                    int sofEndIdx = peakIdx + cfg.sofNSamples + cfg.sofSilentNSamples;
                    // report SoF found
                    System.out.println("SoF end detected at " + sofEndIdx + ", Starting at " + peakIdx);
                    // update receiver
                    receiver.unpacking = true;
                    receiver.tickDone = sofEndIdx;
                    break;
                }

                // move on
                formerWindowMax = newWindowMax;
                formerWindowEnd += cfg.sofDetectWindowLen;
            }
        }
    }

    public ArrayList<Double> getCorrelations() {
        return correlations;
    }
}
