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

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public void demodulateSymbol() {
        // skip the cyclic prefix
        receiver.tickDone += cfg.cyclicPrefixNSamples;

        // get the samples of the symbol
        float[] samples = new float[cfg.symbolLength];
        for (int i = 0; i < cfg.symbolLength; i++) {
            samples[i] = receiver.samples.get(receiver.tickDone + i);
        }
        receiver.tickDone += cfg.symbolLength;

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
                frameBuffer.add((thisCarrierIndex & (1 << (cfg.keyingCapacity - j - 1))) != 0);
            }
        }
    }

    /* Demodulate all to demodulate */
    public void demodulate() {
        while ( receiver.unpacking &&
            receiver.tickDone + cfg.cyclicPrefixNSamples + cfg.symbolLength <= 
            receiver.getLength() &&
            frameBuffer.size() < cfg.frameLength) {
            demodulateSymbol();
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
            // print log
            System.out.println("Pushed the frame into the receiver's buffer");
            // clear the frameBuffer
            frameBuffer.clear();
        }
    }

}
