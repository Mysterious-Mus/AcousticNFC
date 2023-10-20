package com.AcousticNFC.receive;

import java.util.ArrayList;

import com.AcousticNFC.utils.FFT;
import com.AcousticNFC.utils.Complex;

import java.io.File;

import com.AcousticNFC.Config;
import com.AcousticNFC.transmit.OFDM;
import com.AcousticNFC.utils.FileOp;

public class Demodulator {
    
    Receiver receiver;
    Config cfg;

    private ArrayList<Boolean> frameBuffer;

    public Demodulator(Receiver receiver, Config cfg_src) {
        this.receiver = receiver;
        frameBuffer = new ArrayList<Boolean>();
        cfg = cfg_src;
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

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public ArrayList<Boolean> demodulateSymbol(float[] samples) {
        ArrayList<Boolean> resultBuffer = new ArrayList<Boolean>();
        // do the FFT
        Complex[] fftResult = FFT.fft(samples);

        // log the first symbol phases
        // if the frameBuffer is empty
        if (frameBuffer.size() == 0) {
            String panelInfo = "";
            for (int i = 0; i < cfg.numSubCarriers; i++) {
                panelInfo += String.format("%.2f ", fftResult[
                    (int) Math.round((cfg.bandWidthLow + i * cfg.subCarrierWidth) / 
                    cfg.sampleRate * cfg.symbolLength)].phase());
            }
            cfg.UpdFirstSymbolPhases(panelInfo);
        }

        // calculate the keys of the subcarriers
        for (int i = 0; i < cfg.numSubCarriers; i++) {
            // see notes.ipynb for the derivation
            double thisCarrierPhase = fftResult[
                    (int) Math.round((cfg.bandWidthLow + i * cfg.subCarrierWidth) / 
                    cfg.sampleRate * cfg.symbolLength)].phase();

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
        int lastSampleIdx = scanTestCallPoint + scanWindow + cfg.allSymbolLength + 100;
        if (lastSampleIdx < receiver.getLength()) {
            int bestDoneIdx = scanTestCallPoint; double bestBER = 1;
            for (int doneIdx = scanTestCallPoint - scanWindow; doneIdx <= scanTestCallPoint + scanWindow; doneIdx++) {
                ArrayList<Boolean> testReceiveBuffer = new ArrayList<Boolean>();
                int testReceiverPtr = doneIdx;
                while (testReceiveBuffer.size() < cfg.frameLength) {
                    testReceiverPtr += cfg.cyclicPrefixNSamples;
                    float nxtSymbolSamples[] = new float[cfg.symbolLength];
                    for (int i = 0; i < cfg.symbolLength; i++) {
                        nxtSymbolSamples[i] = receiver.samples.get(testReceiverPtr + i + 1);
                    }
                    testReceiverPtr += cfg.symbolLength;
                    ArrayList<Boolean> resultBuffer = demodulateSymbol(nxtSymbolSamples);
                    for (int i = 0; i < resultBuffer.size() && testReceiveBuffer.size() < cfg.frameLength;
                        i++) {
                        testReceiveBuffer.add(resultBuffer.get(i));
                    }
                }
                // calculate BER
                int numErrors = 0;
                for (int bitIdx = 0; bitIdx < testReceiveBuffer.size(); bitIdx ++) {
                    if (testReceiveBuffer.get(bitIdx) != cfg.transmitted.get(bitIdx)) {
                        numErrors ++;
                    }
                }
                double BER = (double)numErrors / cfg.frameLength;
                if (BER < bestBER) {
                    bestBER = BER;
                    bestDoneIdx = doneIdx;
                }
            }
            // print result
            System.out.println("Scan test result: best doneIdx = " + bestDoneIdx + 
                ", best BER = " + bestBER + ". SoF end at " + (bestDoneIdx - cfg.sofSilentNSamples));
            pendingScanTest = false;
        }
    }

    /* Demodulate all to demodulate */
    public void demodulate() {

        // see if we do the scan test
        if (pendingScanTest) {
            scanTest();
        }

        while ( receiver.unpacking &&
            receiver.tickDone + cfg.cyclicPrefixNSamples + cfg.symbolLength < receiver.getLength() &&
            frameBuffer.size() < cfg.frameLength) {
            
            if (frameBuffer.isEmpty()) {
                // rise a scan test
                pendingScanTest = true;
                scanTestCallPoint = receiver.tickDone;
            }
            
            ArrayList<Boolean> resultBuffer = demodulateSymbol(getNxtSample());
            for (int i = 0; i < resultBuffer.size(); i++) {
                frameBuffer.add(resultBuffer.get(i));
            }
        }
        if (frameBuffer.size() >= cfg.frameLength) {
            // print log
            System.out.println("Received a frame of length " + cfg.frameLength);
            receiver.unpacking = false;
            // pop back until the length is Config.frameLength
            while (frameBuffer.size() > cfg.frameLength) {
                frameBuffer.remove(frameBuffer.size() - 1);
            }
            FileOp.outputBitString(frameBuffer, "receiveBuffer.txt");
            // push the frame into the receiver's buffer
            for (int i = 0; i < cfg.frameLength; i++) {
                receiver.receiveBuffer.add(frameBuffer.get(i));
            }
            // calculate BER
            int numErrors = 0;
            for (int i = 0; i < cfg.frameLength; i++) {
                if (frameBuffer.get(i) != cfg.transmitted.get(i)) {
                    numErrors++;
                }
            }
            cfg.UpdBER((double)numErrors / cfg.frameLength);
            // print log
            System.out.println("Pushed the frame into the receiver's buffer");
            // clear the frameBuffer
            frameBuffer.clear();
        }
    }

    boolean pendingScanTest = false;
    int scanTestCallPoint;
    int scanWindow = 500;
}
