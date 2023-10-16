package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;

import java.util.ArrayList;

import com.AcousticNFC.receive.Receiver;

public class SoFDetector {
    
    double sampleRate;
    SoF sof;
    float[] sofSamples;
    int sofNSamples;

    int lastSoFIdx = -1000;

    float corrThreshold = 0.06f;

    Receiver receiver;
    /* The correlation between the samples and the SoF
     * correlations[i] is the correlation between the samples[i:i+L-1] and the SoF */
    ArrayList<Double> correlations;

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

    public void updateCorrelations() {
        // if nothing yet
        if (receiver.getLength() < sofNSamples) {
            return;
        }

        // calculate the new correlations
        for (int startingIdx = correlations.size(); 
            startingIdx < receiver.getLength() - sofNSamples + 1; startingIdx++) {
            correlations.add(correlation(receiver.getSamples(), startingIdx));
            if (!receiver.unpacking) {
                if (correlations.get(startingIdx) > corrThreshold
                    && startingIdx - lastSoFIdx > sofNSamples) {
                    int endIdx = startingIdx + sofNSamples;
                    System.out.println("SoF end detected at " + endIdx);
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
