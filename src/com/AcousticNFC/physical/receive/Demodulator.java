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

    public ArrayList<Boolean> frameBuffer;
    // double timeCompensation = 0; // compensate the sampling offset

    ECC Ecc;

    public Demodulator(Receiver receiver) {
        this.receiver = receiver;
        frameBuffer = new ArrayList<Boolean>();
        Ecc = new ECC();
    }

    private float[] getNxtSample() {
        // skip the cyclic prefix
        receiver.tickDone += Config.cyclicPrefixNSamples;


        // get the samples of the symbol
        float[] samples = new float[Config.symbolLength];
        for (int i = 0; i < Config.symbolLength; i++) {
            samples[i] = receiver.getSample(receiver.tickDone + i + 1);
        }
        receiver.tickDone += Config.symbolLength;

        return samples;
    }

    public static double[] subCarrPhases(float[] samples) {
        double[] result = new double[Config.numSubCarriers];
        Complex[] fftResult = FFT.fft(samples);
        for (int i = 0; i < Config.numSubCarriers; i++) {
            result[i] = fftResult[
                    (int) Math.round((Config.bandWidthLow + i * Config.subCarrierWidth) / 
                    Config.sampleRate * Config.symbolLength)].phase();
            // result[i] = fftResult[
            //         (int) Math.round((Config.bandWidthLow + i * Config.subCarrierWidth) / 
            //         Config.sampleRate * Config.symbolLength)].phase() + 
            //         timeCompensation * 2 * Math.PI * (Config.bandWidthLow + i * Config.subCarrierWidth);
        }
        return result;
    }

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public static ArrayList<Boolean> demodulateSymbol(float[] samples) {
        ArrayList<Boolean> resultBuffer = new ArrayList<Boolean>();
        // get Phases
        double phases[] = subCarrPhases(samples);

        // // log the first symbol phases
        // // if the frameBuffer is empty
        // if (frameBuffer.size() == 0) {
        //     String panelInfo = "";
        //     for (int i = 0; i < Config.numSubCarriers; i++) {
        //         panelInfo += String.format("%.2f ", phases[i]);
        //     }
        //     Config.UpdFirstSymbolPhases(panelInfo);
        // }

        // calculate the keys of the subcarriers
        for (int i = 0; i < Config.numSubCarriers; i++) {
            // see notes.ipynb for the derivation
            double thisCarrierPhase = phases[i];

            int numKeys = (int) Math.round(Math.pow(2, Config.keyingCapacity));
            double lastPhaseSegment = 2 * Math.PI / numKeys / 2;
            int thisCarrierIndex = (int)Math.floor((thisCarrierPhase + 2 * Math.PI + lastPhaseSegment) % (2 * Math.PI) / 
                (2 * Math.PI) * numKeys);
            
            // push the bits into the receiver's buffer
            for (int j = 0; j < Config.keyingCapacity; j++) {
                resultBuffer.add((thisCarrierIndex & (1 << (Config.keyingCapacity - j - 1))) != 0);
            }
        }
        return resultBuffer;
    }

    private void scanTest() {
        int alignNSample = Config.alignNSymbol * (Config.cyclicPrefixNSamples + Config.symbolLength);
        int alignBitLen = Config.alignNSymbol * Config.keyingCapacity * Config.numSubCarriers;
        int lastSampleIdx = receiver.tickDone + Config.scanWindow + alignNSample;
        while (lastSampleIdx >= receiver.getLength()) {
            // busy waiting
            Thread.yield();
        }
        if (lastSampleIdx < receiver.getLength()) {
            int bestDoneIdx = receiver.tickDone;
            double bestDistortion = 1000;
            double bestBER = 1;
            for (int doneIdx = receiver.tickDone - Config.scanWindow; 
            doneIdx <= receiver.tickDone + Config.scanWindow; doneIdx++) {
                int testReceiverPtr = doneIdx;
                double avgAbsDistortion = 0;
                double avgDistortion = 0;
                double BER = 0;
                for (int testSymId = 0; testSymId < Config.alignNSymbol; testSymId++) {
                    testReceiverPtr += Config.cyclicPrefixNSamples;
                    float nxtSymbolSamples[] = new float[Config.symbolLength];
                    for (int i = 0; i < Config.symbolLength; i++) {
                        nxtSymbolSamples[i] = receiver.samples.get(testReceiverPtr + i + 1);
                    }
                    testReceiverPtr += Config.symbolLength;
                    // calculate average time distortion
                    double[] phases = subCarrPhases(nxtSymbolSamples);
                    for (int subCId = 0; subCId < Config.numSubCarriers; subCId ++) {
                        double thisCarrierPhase = phases[subCId];
                        int numKeys = (int) Math.round(Math.pow(2, Config.keyingCapacity));
                        int thisCarrierIdx = 0;
                        for (int bitId = 0; bitId < Config.keyingCapacity; bitId ++) {
                            thisCarrierIdx += (Config.alignBitFunc(testSymId * Config.keyingCapacity * Config.numSubCarriers + subCId * Config.keyingCapacity + bitId) ? 1 : 0)
                             << (Config.keyingCapacity - bitId - 1);
                        }
                        double requiredPhase = 2 * Math.PI / numKeys * thisCarrierIdx;
                        double distortion = ((thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) < 
                            (2 * Math.PI) - (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) ? 
                            (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) : 
                            (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) - 2 * Math.PI);
                        avgAbsDistortion += Math.abs(distortion)
                                            / (2 * Math.PI * (Config.bandWidthLow + subCId * Config.subCarrierWidth));
                        avgDistortion += distortion
                                            / (2 * Math.PI * (Config.bandWidthLow + subCId * Config.subCarrierWidth));
                    }
                    // add to BER
                    ArrayList<Boolean> demodulated = demodulateSymbol(nxtSymbolSamples);
                    for (int symBitId = 0; symBitId < Config.keyingCapacity * Config.numSubCarriers; symBitId ++) {
                        BER += (demodulated.get(symBitId) != Config.alignBitFunc(testSymId * Config.keyingCapacity * Config.numSubCarriers + symBitId) ? 1 : 0);
                    }
                }
                avgAbsDistortion /= alignBitLen;
                avgDistortion /= alignBitLen;
                BER /= alignBitLen;
                if (Math.abs(avgAbsDistortion) < Math.abs(bestDistortion)) {
                    bestDistortion = avgAbsDistortion;
                    bestDoneIdx = doneIdx;
                    // timeCompensation = -avgDistortion;
                    bestBER = BER;
                }
            }
            // print compensation: bestdone - tickdone
            System.out.println("Compensation: " + (bestDoneIdx - receiver.tickDone));
            // timeCompensation = -bestDistortion;
            // print avg distort samples
            System.out.println("Avg Distort: " + bestDistortion * Config.sampleRate);
            // print BER
            System.out.println("BER: " + bestBER);

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
            while (receiver.tickDone + Config.cyclicPrefixNSamples + Config.symbolLength >= receiver.getLength()) {
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
            boolean[] decoded = Config.ECCOn? Ecc.viterbiDecode(receivedCodewords) : receivedCodewords;
            // push the frame into the receiver's buffer
            for (int i = 0; i < decoded.length; i++) {
                receiver.receiveBuffer.add(decoded[i]);
            }

        }
    }

}
