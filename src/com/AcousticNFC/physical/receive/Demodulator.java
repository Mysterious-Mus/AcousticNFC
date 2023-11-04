package com.AcousticNFC.physical.receive;

import java.util.ArrayList;

import com.AcousticNFC.utils.FFT;
import com.AcousticNFC.utils.Complex;
import com.AcousticNFC.utils.ECC;

import java.io.File;

import com.AcousticNFC.Config;
import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.transmit.OFDM;
import com.AcousticNFC.utils.FileOp;

public class Demodulator {
    
    Receiver receiver;
    Config cfg;

    public ArrayList<Boolean> frameBuffer;
    double timeCompensation = 0; // compensate the sampling offset

    ECC Ecc;

    public Demodulator(Receiver receiver, Config cfg_src) {
        this.receiver = receiver;
        frameBuffer = new ArrayList<Boolean>();
        cfg = cfg_src;
        Ecc = new ECC(cfg);
    }

    private float[] getNxtSample() {
        // skip the cyclic prefix
        receiver.tickDone += cfg.cyclicPrefixNSamples;


        // get the samples of the symbol
        float[] samples = new float[cfg.symbolLength];
        for (int i = 0; i < cfg.symbolLength; i++) {
            samples[i] = receiver.getSample(receiver.tickDone + i + 1);
        }
        receiver.tickDone += cfg.symbolLength;

        return samples;
    }

    public double[] subCarrPhases(float[] samples) {
        double[] result = new double[cfg.numSubCarriers];
        Complex[] fftResult = FFT.fft(samples);
        for (int i = 0; i < cfg.numSubCarriers; i++) {
            result[i] = fftResult[
                    (int) Math.round((cfg.bandWidthLow + i * cfg.subCarrierWidth) / 
                    cfg.sampleRate * cfg.symbolLength)].phase() + 
                    timeCompensation * 2 * Math.PI * (cfg.bandWidthLow + i * cfg.subCarrierWidth);
        }
        return result;
    }

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public ArrayList<Boolean> demodulateSymbol(float[] samples) {
        ArrayList<Boolean> resultBuffer = new ArrayList<Boolean>();
        // get Phases
        double phases[] = subCarrPhases(samples);

        // log the first symbol phases
        // if the frameBuffer is empty
        if (frameBuffer.size() == 0) {
            String panelInfo = "";
            for (int i = 0; i < cfg.numSubCarriers; i++) {
                panelInfo += String.format("%.2f ", phases[i]);
            }
            cfg.UpdFirstSymbolPhases(panelInfo);
        }

        // calculate the keys of the subcarriers
        for (int i = 0; i < cfg.numSubCarriers; i++) {
            // see notes.ipynb for the derivation
            double thisCarrierPhase = phases[i];

            int numKeys = (int) Math.round(Math.pow(2, cfg.keyingCapacity));
            double lastPhaseSegment = 2 * Math.PI / numKeys / 2;
            int thisCarrierIndex = (int)Math.floor((thisCarrierPhase + 2 * Math.PI + lastPhaseSegment) % (2 * Math.PI) / 
                (2 * Math.PI) * numKeys);
            
            // push the bits into the receiver's buffer
            for (int j = 0; j < cfg.keyingCapacity; j++) {
                resultBuffer.add((thisCarrierIndex & (1 << (cfg.keyingCapacity - j - 1))) != 0);
            }
        }
        return resultBuffer;
    }

    private void scanTest() {
        int alignNSample = cfg.alignNSymbol * (cfg.cyclicPrefixNSamples + cfg.symbolLength);
        int alignBitLen = cfg.alignNSymbol * cfg.keyingCapacity * cfg.numSubCarriers;
        int lastSampleIdx = receiver.tickDone + cfg.scanWindow + alignNSample;
        while (lastSampleIdx >= receiver.getLength()) {
            // busy waiting
            Thread.yield();
        }
        if (lastSampleIdx < receiver.getLength()) {
            int bestDoneIdx = receiver.tickDone;
            double bestDistortion = 1000;
            for (int doneIdx = receiver.tickDone - cfg.scanWindow; 
            doneIdx <= receiver.tickDone + cfg.scanWindow; doneIdx++) {
                int testReceiverPtr = doneIdx;
                double avgAbsDistortion = 0;
                double avgDistortion = 0;
                for (int testSymId = 0; testSymId < cfg.alignNSymbol; testSymId++) {
                    testReceiverPtr += cfg.cyclicPrefixNSamples;
                    float nxtSymbolSamples[] = new float[cfg.symbolLength];
                    for (int i = 0; i < cfg.symbolLength; i++) {
                        nxtSymbolSamples[i] = receiver.samples.get(testReceiverPtr + i + 1);
                    }
                    testReceiverPtr += cfg.symbolLength;
                    // calculate average time distortion
                    double[] phases = subCarrPhases(nxtSymbolSamples);
                    for (int subCId = 0; subCId < cfg.numSubCarriers; subCId ++) {
                        double thisCarrierPhase = phases[subCId];
                        int numKeys = (int) Math.round(Math.pow(2, cfg.keyingCapacity));
                        int thisCarrierIdx = 0;
                        for (int bitId = 0; bitId < cfg.keyingCapacity; bitId ++) {
                            thisCarrierIdx += (cfg.alignBitFunc(testSymId * cfg.keyingCapacity * cfg.numSubCarriers + subCId * cfg.keyingCapacity + bitId) ? 1 : 0)
                             << (cfg.keyingCapacity - bitId - 1);
                        }
                        double requiredPhase = 2 * Math.PI / numKeys * thisCarrierIdx;
                        double distortion = ((thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) < 
                            (2 * Math.PI) - (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) ? 
                            (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) : 
                            (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) - 2 * Math.PI);
                        avgAbsDistortion += Math.abs(distortion)
                                            / (2 * Math.PI * (cfg.bandWidthLow + subCId * cfg.subCarrierWidth));
                        avgDistortion += distortion
                                            / (2 * Math.PI * (cfg.bandWidthLow + subCId * cfg.subCarrierWidth));
                    }
                }
                avgAbsDistortion /= alignBitLen;
                avgDistortion /= alignBitLen;
                if (Math.abs(avgAbsDistortion) < Math.abs(bestDistortion)) {
                    bestDistortion = avgAbsDistortion;
                    bestDoneIdx = doneIdx;
                    timeCompensation = -avgDistortion;
                }
            }
            // print compensation: bestdone - tickdone
            System.out.println("Compensation: " + (bestDoneIdx - receiver.tickDone));
            // timeCompensation = -bestDistortion;
            // print avg distort samples
            System.out.println("Avg Distort: " + bestDistortion * cfg.sampleRate);

            receiver.scanAligning = false;
            receiver.tickDone = bestDoneIdx + alignNSample;
            receiver.unpacking = true;
        }
    }

    /* Demodulate all to demodulate */
    public void demodulate() {

        // see if we do the scan test
        if (receiver.scanAligning) {
            scanTest();
        }

        while ( receiver.unpacking && frameBuffer.size() < MacFrame.getFrameBitLen()) {
            while (receiver.tickDone + cfg.cyclicPrefixNSamples + cfg.symbolLength >= receiver.getLength()) {
                // waiting for the next symbol
                Thread.yield();
            }
            ArrayList<Boolean> resultBuffer = demodulateSymbol(getNxtSample());
            for (int i = 0; i < resultBuffer.size(); i++) {
                frameBuffer.add(resultBuffer.get(i));
            }
        }
        if (frameBuffer.size() >= MacFrame.getFrameBitLen()) {
            // print log
            System.out.println("Received a frame of length " + MacFrame.getFrameBitLen());
            receiver.unpacking = false;
            // pop back until the length is Config.frameLength
            while (frameBuffer.size() > MacFrame.getFrameBitLen()) {
                frameBuffer.remove(frameBuffer.size() - 1);
            }
            // get the string for decoding
            boolean[] receivedCodewords = new boolean[MacFrame.getFrameBitLen()];
            for (int i = 0; i < MacFrame.getFrameBitLen(); i++) {
                receivedCodewords[i] = frameBuffer.get(i);
            }
            // decode
            boolean[] decoded = cfg.ECCOn? Ecc.viterbiDecode(receivedCodewords) : receivedCodewords;
            // push the frame into the receiver's buffer
            for (int i = 0; i < decoded.length; i++) {
                receiver.receiveBuffer.add(decoded[i]);
            }

        }
    }

}
