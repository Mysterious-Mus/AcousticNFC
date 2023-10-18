package com.AcousticNFC.receive;

import com.AcousticNFC.utils.FFT;
import com.AcousticNFC.utils.Complex;

import java.io.File;

import com.AcousticNFC.Config;
import com.AcousticNFC.transmit.OFDM;
import com.AcousticNFC.utils.FileOp;
import com.AcousticNFC.utils.Highpass;

public class Demodulator {
    
    Receiver receiver;
    OFDM ofdm_info;
    int symbolLength;
    Highpass highpass;

    public Demodulator(Receiver receiver) {
        this.receiver = receiver;

        // init a OFDM for demodulation info
        ofdm_info = new OFDM(receiver.sampleRate);

        symbolLength = ofdm_info.symbolNSamples;
    }

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public void demodulateSymbol_test() {
        // skip the cyclic prefix
        receiver.tickDone += ofdm_info.cyclicPrefixNSamples;

        highpass = new Highpass((float) receiver.sampleRate);
        // get the samples of the symbol
        float[] samples = new float[symbolLength];
        for (int i = 0; i < symbolLength; i++) {
            // samples[i] = highpass.filter(receiver.samples.get(receiver.tickDone + i));
            samples[i] = receiver.samples.get(receiver.tickDone + i);
        }
        receiver.tickDone += symbolLength;

        // do the FFT
        Complex[] fftResult = FFT.fft(samples);

        // calculate all amplitudes
        float[] amplitudes = new float[symbolLength];
        for (int i = 0; i < symbolLength; i++) {
            amplitudes[i] = (float) fftResult[i].abs();
        }

        // calcualte the phases
        float[] phases = new float[symbolLength];
        for (int i = 0; i < symbolLength; i++) {
            phases[i] = (float) fftResult[i].phase();
        }

        // calculate the phases of the subcarriers
        float[] subCarrierPhases = new float[ofdm_info.numSubCarriers];
        for (int i = 0; i < ofdm_info.numSubCarriers; i++) {
            subCarrierPhases[i] = phases[
                (int) Math.round((ofdm_info.bandWidthLow + i * ofdm_info.subCarrierWidth) / 
                    ofdm_info.sampleRate * symbolLength)];
        }

        // dump the amplitudes
        FileOp.outputFloatSeq(amplitudes, "amplitudes.csv");

        // dump the phases
        FileOp.outputFloatSeq(phases, "phases.csv");

        // dump the subcarrier phases
        FileOp.outputFloatSeq(subCarrierPhases, "subCarrierPhases.csv");

        // dump the symbol samples
        FileOp.outputFloatSeq(samples, "symbol.csv");
    }

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public void demodulateSymbol() {
        // skip the cyclic prefix
        receiver.tickDone += ofdm_info.cyclicPrefixNSamples;

        highpass = new Highpass((float) receiver.sampleRate);
        // get the samples of the symbol
        float[] samples = new float[symbolLength];
        for (int i = 0; i < symbolLength; i++) {
            // samples[i] = highpass.filter(receiver.samples.get(receiver.tickDone + i));
            samples[i] = receiver.samples.get(receiver.tickDone + i);
        }
        receiver.tickDone += symbolLength;

        // do the FFT
        Complex[] fftResult = FFT.fft(samples);

        // calculate the keys of the subcarriers
        for (int i = 0; i < ofdm_info.numSubCarriers; i++) {
            // see notes.ipynb for the derivation
            double thisCarrierPhase = fftResult[
                    (int) Math.round((ofdm_info.bandWidthLow + i * ofdm_info.subCarrierWidth) / 
                    ofdm_info.sampleRate * symbolLength)].phase();
            int numKeys = (int) Math.round(Math.pow(2, ofdm_info.keyingCapacity));
            double lastPhaseSegment = 2 * Math.PI / numKeys / 2;
            int thisCarrierIndex = (int)Math.floor((thisCarrierPhase + 2 * Math.PI + lastPhaseSegment) % (2 * Math.PI) / 
                (2 * Math.PI) * numKeys);
            
            // push the bits into the receiver's buffer
            for (int j = 0; j < ofdm_info.keyingCapacity; j++) {
                receiver.receiveBuffer.add((thisCarrierIndex & (1 << (ofdm_info.keyingCapacity - j - 1))) != 0);
            }
        }
    }

    /* Demodulate all to demodulate */
    public void demodulate() {
        while ( receiver.unpacking &&
            receiver.tickDone + ofdm_info.cyclicPrefixNSamples + symbolLength <= 
            receiver.getLength()) {
            demodulateSymbol();
            if (receiver.receiveBuffer.size() >= Config.frameLength) {
                // print log
                System.out.println("Received a frame of length " + Config.frameLength);
                receiver.unpacking = false;
                // pop back until the length is Config.frameLength
                while (receiver.receiveBuffer.size() > Config.frameLength) {
                    receiver.receiveBuffer.remove(receiver.receiveBuffer.size() - 1);
                }
                FileOp.outputBitString(receiver.receiveBuffer, "receiveBuffer.txt");
                break;
            }
        }
    }

    /* Demodulate test */
    public void demodulate_test() {
        while ( receiver.unpacking &&
            receiver.tickDone + ofdm_info.cyclicPrefixNSamples + symbolLength <= 
            receiver.getLength()) {
            demodulateSymbol_test();
            receiver.unpacking = false;
            break;
        }
    }
}
