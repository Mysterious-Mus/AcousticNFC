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
    float[] sofSamples;

    Receiver receiver; // where to take the samples

    /* The correlation between the samples and the SoF
     * correlations[i] is the correlation between the samples[i-L+1:i] and the SoF */
    /* the small index at the start would be L-1 */
    CyclicBuffer<Double> correlations;
    /* How many points should it save?
     * We know the points before receiver.tickDone are checked
     * We don't check more than sofNSamples + sofSilentNSamples points at a time,
     *  otherwise we might miss the start of a frame
     * To confirm that a point is exact end the start of a frame, we need to check all points
     *  whose distance to the point is smaller than sofNSamples
     * So the size of the buffer, serving also as a limit of the number of points to check,
     *  should be (sofNSamples - 1) + (sofNSamples + sofSilentNSamples) + (sofNSamples - 1)
     *  = 3 * sofNSamples + sofSilentNSamples - 2,
     * At each call, the buffer should contain(both end inclusive):
     *  from (receiver.tickDone + 1) - (sofNSamples - 1) = receiver.tickDone - sofNSamples + 2(or 0)
     *  to (receiver.tickDone + 1) + (sofNSamples + sofSilentNSamples) - 1 + (sofNSamples - 1)
     *      = receiver.tickDone + 2 * sofNSamples + sofSilentNSamples - 1(or receiveBufferSize - sofNSamples)
     */

    public SoFDetector(Config cfg_src, Receiver receiver) {
        cfg = cfg_src;
        sof = new SoF(cfg);
        sofSamples = sof.generateSoFNoSilence();
        correlations = new CyclicBuffer<Double>(3 * cfg.sofNSamples + cfg.sofSilentNSamples - 2);
        // the initial FIW:
        correlations.setFIW(cfg.sofNSamples - 1);
        this.receiver = receiver;
    }

    /* Calculating Correlations with SoF and see if we can mark the start of a frame */
    public void detect() {
        if (receiver.unpacking) return;
        // Which slice of Corrs we need in this call?
        // the first index we should check is receiver.tickDone + 1
        // if it is the end of a SoF, it should be the largest among the neighbouring 2*sofNSamples-1 points,
        //  and greater than the threshold.
        // So the first index of Corrs we should preserve is receiver.tickDone + 1 - sofNSamples + 1
        //  = receiver.tickDone - sofNSamples + 2
        correlations.setFIW(receiver.tickDone - cfg.sofNSamples + 2);

        // print correlations.feedIdx() and receiver.getLength()
        // fill in the buffer until full or no more data
        for (
            int idx = correlations.feedIdx();
            idx < receiver.getLength() && !correlations.full();
            idx++
        ) {
            // the interval calculated now is [idx - sofNSamples + 1, idx]
            double newCorr = 0;
            for (int sofIdx = 0; sofIdx < cfg.sofNSamples; sofIdx++) {
                newCorr += receiver.getSample(idx-cfg.sofNSamples+1+sofIdx) * sofSamples[sofIdx];
            }
            newCorr /= cfg.sofNSamples;
            correlations.push(newCorr);
            // update the cfg panel
            cfg.UpdCorrdetect(newCorr);
        }

        // now we scan all our candidates
        for (
            int candidateIdx = Math.max(receiver.tickDone + 1, correlations.FIW);
            candidateIdx <= correlations.feedIdx() - cfg.sofNSamples;
            candidateIdx++
        ) {
            // check threshold first
            if (correlations.get(candidateIdx) > cfg.SofDetectThreshld) {
                // check if it is greatest among the neighbouring 2*sofNSamples-1 points
                int greatestIdx = candidateIdx;
                for (
                    int i = Math.max(correlations.feedIdx() - cfg.sofNSamples, candidateIdx + 1);
                    i <= candidateIdx + cfg.sofNSamples - 1;
                    i++
                ) {
                    if (correlations.get(i) > correlations.get(greatestIdx)) {
                        greatestIdx = i;
                    }
                }

                // if it is the greatest among neighbours
                if (greatestIdx == candidateIdx) {
                    // we have found a SoF
                    receiver.unpacking = true;
                    // point to the end of SoF slience
                    receiver.tickDone = candidateIdx + cfg.sofSilentNSamples;
                    // print log
                    System.out.println("Found a SoF ending at " + receiver.tickDone);
                    return;
                }
                else {
                    // the candidates between candidateIdx and greatestIdx - 1 have been ruled out
                    // we can skip them
                    candidateIdx = Math.max(greatestIdx - 1, candidateIdx);
                }
            }
        }

        // no SoF found, go to the last index we have checked
        receiver.tickDone = correlations.feedIdx() - cfg.sofNSamples;
    }
}
