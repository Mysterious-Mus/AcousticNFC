package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;

import java.util.ArrayList;

import com.AcousticNFC.receive.Receiver;

public class SoFDetector {
    
    double sampleRate;
    SoF sof;
    float[] sofSamples;
    int sofNSamples;

    int lastSoFIdx = 0;

    float corrThreshold = 0.0025f;
    // float corrThreshold = 0.15f;

    Receiver receiver;
    /* The correlation between the samples and the SoF
     * correlations[i] is the correlation between the samples[i:i+L-1] and the SoF */
    ArrayList<Double> correlations;

    /* Optimize: when the corr reach thresh, still wait a while and find
     * the max corr in the following samples
     */
    int waitNSamples = 1000;

    public SoFDetector(double sampleRate, Receiver receiver) {
        this.sampleRate = sampleRate;
        sof = new SoF(sampleRate);
        sofSamples = sof.generateSoF();
        sofNSamples = sof.NSample();
        correlations = new ArrayList<Double>();
        this.receiver = receiver;
    }

    public int getLength() {
        return sofNSamples;
    }

    /* Calculate the correlation between the samples and the SoF
     * If the samples are shorter than SoF, 0s are padded to the end of the samples
     */
    public double correlation(ArrayList<Float> samples, int startIdx) {
        double sum = 0;
        for (int i = startIdx; i < Math.min(startIdx + sofNSamples, samples.size()); i++) {
            sum += samples.get(i) * sofSamples[i - startIdx];
        }

        return sum / sofNSamples;
    }

    boolean testdone = false;

    public void updateCorrelations() {
        // if nothing yet
        if (receiver.getLength() < sofNSamples) {
            return;
        }

        // test: force detect
        // if (receiver.getLength() > 150000 && !testdone) {
        //     int endIdx = receiver.getLength();
        //     System.out.println("SoF end detected at " + endIdx);
        //     // send message to start demodulation
        //     receiver.unpacking = true;
        //     receiver.tickDone = endIdx;
        //     lastSoFIdx = endIdx;
        //     testdone = true;
        // }

        // calculate the new correlations
        for (int startingIdx = correlations.size(); 
            startingIdx < receiver.getLength() - sofNSamples + 1; startingIdx++) {
            correlations.add(correlation(receiver.getSamples(), startingIdx));
            if (!receiver.unpacking) {
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
    }

    public ArrayList<Double> getCorrelations() {
        return correlations;
    }
}
