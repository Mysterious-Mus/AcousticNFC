package com.AcousticNFC.receive;

import com.AcousticNFC.transmit.SoF;
import com.AcousticNFC.receive.Receiver;

public class SoFDetector {
    
    double sampleRate;
    SoF sof;
    float[] sofSamples;
    int sofNSamples;

    Receiver receiver;
    /* The correlation between the samples and the SoF
     * correlations[i] is the correlation between the samples[i-L+1:i+1] and the SoF
     */
    double[] correlations;

    public SoFDetector(double sampleRate, Receiver receiver) {
        this.sampleRate = sampleRate;
        sof = new SoF(sampleRate);
        sofSamples = sof.generateSoF();
        sofNSamples = sof.NSample();
        correlations = new double[sofNSamples];
        this.receiver = receiver;
    }

    public int getLength() {
        return sofNSamples;
    }

    /* Calculate the correlation between the samples and the SoF
     * If the samples are shorter than SoF, 0s are padded to the end of the samples
     */
    public double correlation(float[] samples) {
        double sum = 0;
        for (int i = 0; i < Math.min(samples.length, sofNSamples); i++) {
            sum += samples[i] * sofSamples[i];
        }

        return sum;
    }

    public void updateCorrelations() {
        double[] newCorrelations = new double[receiver.getLength() - sofNSamples + 1];

        // copy the old correlations
        System.arraycopy(correlations, 0, newCorrelations, 0, correlations.length);

        // calculate the new correlations
        for (int i = correlations.length; i < newCorrelations.length; i++) {
            newCorrelations[i] = correlation(receiver.getSamples(i, sofNSamples));
        }

        correlations = newCorrelations;
    }

    public double[] getCorrelations() {
        return correlations;
    }
}
