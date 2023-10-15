package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;

import java.util.ArrayList;

import com.AcousticNFC.receive.Receiver;

public class SoFDetector {
    
    double sampleRate;
    SoF sof;
    float[] sofSamples;
    int sofNSamples;

    /* By warmup, it means the SoF detector already has enough samples to calculate
     * The std of the correlations, thus can start to detect correlation peaks */
    boolean warmup;
    double corrStd;
    final int warmupLength = 2000;

    int lastSoFIdx = -1000;

    int stdFactor = 200;

    Receiver receiver;
    /* The correlation between the samples and the SoF
     * correlations[i] is the correlation between the samples[i-L+1:i+1] and the SoF */
    ArrayList<Double> correlations;

    public SoFDetector(double sampleRate, Receiver receiver) {
        this.sampleRate = sampleRate;
        sof = new SoF(sampleRate);
        sofSamples = sof.generateSoF();
        sofNSamples = sof.NSample();
        correlations = new ArrayList<Double>();
        this.receiver = receiver;
        warmup = false;
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
            if (startingIdx == warmupLength) {
                warmup = true;
                corrStd = 0;
                for (int j = 0; j < startingIdx; j++) {
                    corrStd += correlations.get(j) * correlations.get(j);
                }
                corrStd = Math.sqrt(corrStd / warmupLength);
                // log
                System.out.println("Correlation std: " + corrStd);
            }
            correlations.add(correlation(receiver.getSamples(), startingIdx));
            if (warmup) {
                if (correlations.get(startingIdx) > stdFactor * corrStd
                    && startingIdx - lastSoFIdx > sofNSamples) {
                    System.out.println("SoF end detected at " + startingIdx);
                    lastSoFIdx = startingIdx;
                }
            }
        }
    }

    public ArrayList<Double> getCorrelations() {
        return correlations;
    }
}
