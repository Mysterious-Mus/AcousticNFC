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

    public static Complex[] subCarrCoeffs(float[] samples) {
        Complex[] result = new Complex[Config.numSubCarriers];
        Complex[] fftResult = FFT.fft(samples);
        for (int i = 0; i < Config.numSubCarriers; i++) {
            result[i] = fftResult[
                    (int) Math.round((Config.bandWidthLow + i * Config.subCarrierWidth) / 
                    Config.sampleRate * Config.symbolLength)];
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
        Complex coeffs[] = subCarrCoeffs(samples);

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
        double unitAmp = 0;
        for (int i = 0; i < Config.numSubCarriers; i++) {
            // see proj1.ipynb for the derivation
            double thisCarrierPhase = coeffs[i].phase();

            int numPhaseKeys = 1 << Config.PSkeyingCapacity;
            double lastPhaseSegment = 2 * Math.PI / numPhaseKeys / 2;
            int thisCarrierPhaseIndex = (int)Math.floor((thisCarrierPhase + 2 * Math.PI + lastPhaseSegment) 
                % (2 * Math.PI) /  (2 * Math.PI) * numPhaseKeys);
            
            // push the bits into the receiver's buffer
            for (int j = 0; j < Config.PSkeyingCapacity; j++) {
                resultBuffer.add((thisCarrierPhaseIndex & (1 << (Config.PSkeyingCapacity - j - 1))) != 0);
            }

            if (i == 0) {
                unitAmp = coeffs[i].abs();
            }
            else {
                int thisCarrierAmpIdx = (int) Math.round(coeffs[i].abs() / unitAmp) - 1;
                // control with max
                thisCarrierAmpIdx = thisCarrierAmpIdx > (1 << OFDM.Configs.ASK_CAPACITY) - 1 ? 
                    (1 << OFDM.Configs.ASK_CAPACITY) - 1 : thisCarrierAmpIdx;
                // push bits
                for (int j = 0; j < OFDM.Configs.ASK_CAPACITY; j++) {
                    resultBuffer.add((thisCarrierAmpIdx & (1 << (OFDM.Configs.ASK_CAPACITY - j - 1))) != 0);
                }
            }
        }
        return resultBuffer;
    }

    // private void scanTest() {
    //     int alignNSample = Config.alignNSymbol * (Config.cyclicPrefixNSamples + Config.symbolLength);
    //     int alignBitLen = Config.alignNSymbol * Config.PSkeyingCapacity * Config.numSubCarriers;
    //     int lastSampleIdx = receiver.tickDone + Config.scanWindow + alignNSample;
    //     while (lastSampleIdx >= receiver.getLength()) {
    //         // busy waiting
    //         Thread.yield();
    //     }
    //     if (lastSampleIdx < receiver.getLength()) {
    //         int bestDoneIdx = receiver.tickDone;
    //         double bestDistortion = 1000;
    //         double bestBER = 1;
    //         for (int doneIdx = receiver.tickDone - Config.scanWindow; 
    //         doneIdx <= receiver.tickDone + Config.scanWindow; doneIdx++) {
    //             int testReceiverPtr = doneIdx;
    //             double avgAbsDistortion = 0;
    //             double avgDistortion = 0;
    //             double BER = 0;
    //             for (int testSymId = 0; testSymId < Config.alignNSymbol; testSymId++) {
    //                 testReceiverPtr += Config.cyclicPrefixNSamples;
    //                 float nxtSymbolSamples[] = new float[Config.symbolLength];
    //                 for (int i = 0; i < Config.symbolLength; i++) {
    //                     nxtSymbolSamples[i] = receiver.samples.get(testReceiverPtr + i + 1);
    //                 }
    //                 testReceiverPtr += Config.symbolLength;
    //                 // calculate average time distortion
    //                 double[] phases = subCarrCoeffs(nxtSymbolSamples);
    //                 for (int subCId = 0; subCId < Config.numSubCarriers; subCId ++) {
    //                     double thisCarrierPhase = phases[subCId];
    //                     int numKeys = (int) Math.round(Math.pow(2, Config.PSkeyingCapacity));
    //                     int thisCarrierIdx = 0;
    //                     for (int bitId = 0; bitId < Config.PSkeyingCapacity; bitId ++) {
    //                         thisCarrierIdx += (Config.alignBitFunc(testSymId * Config.PSkeyingCapacity * Config.numSubCarriers + subCId * Config.PSkeyingCapacity + bitId) ? 1 : 0)
    //                          << (Config.PSkeyingCapacity - bitId - 1);
    //                     }
    //                     double requiredPhase = 2 * Math.PI / numKeys * thisCarrierIdx;
    //                     double distortion = ((thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) < 
    //                         (2 * Math.PI) - (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) ? 
    //                         (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) : 
    //                         (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) - 2 * Math.PI);
    //                     avgAbsDistortion += Math.abs(distortion)
    //                                         / (2 * Math.PI * (Config.bandWidthLow + subCId * Config.subCarrierWidth));
    //                     avgDistortion += distortion
    //                                         / (2 * Math.PI * (Config.bandWidthLow + subCId * Config.subCarrierWidth));
    //                 }
    //                 // add to BER
    //                 ArrayList<Boolean> demodulated = demodulateSymbol(nxtSymbolSamples);
    //                 for (int symBitId = 0; symBitId < Config.PSkeyingCapacity * Config.numSubCarriers; symBitId ++) {
    //                     BER += (demodulated.get(symBitId) != Config.alignBitFunc(testSymId * Config.PSkeyingCapacity * Config.numSubCarriers + symBitId) ? 1 : 0);
    //                 }
    //             }
    //             avgAbsDistortion /= alignBitLen;
    //             avgDistortion /= alignBitLen;
    //             BER /= alignBitLen;
    //             if (Math.abs(avgAbsDistortion) < Math.abs(bestDistortion)) {
    //                 bestDistortion = avgAbsDistortion;
    //                 bestDoneIdx = doneIdx;
    //                 // timeCompensation = -avgDistortion;
    //                 bestBER = BER;
    //             }
    //         }
    //         // print compensation: bestdone - tickdone
    //         System.out.println("Compensation: " + (bestDoneIdx - receiver.tickDone));
    //         // timeCompensation = -bestDistortion;
    //         // print avg distort samples
    //         System.out.println("Avg Distort: " + bestDistortion * Config.sampleRate);
    //         // print BER
    //         System.out.println("BER: " + bestBER);

    //         receiver.scanAligning = false;
    //         receiver.tickDone = bestDoneIdx + alignNSample;
    //         receiver.unpacking = true;
    //     }
    // }
}
