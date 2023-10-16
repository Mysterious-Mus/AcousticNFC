package com.AcousticNFC.receive;

import com.AcousticNFC.utils.FFT;
import com.AcousticNFC.utils.Complex;
import com.AcousticNFC.receive.Receiver;
import com.AcousticNFC.transmit.OFDM;
import com.AcousticNFC.utils.FileOp;

public class Demodulator {
    
    Receiver receiver;
    OFDM ofdm_info;
    int symbolLength;

    public Demodulator(Receiver receiver) {
        this.receiver = receiver;

        // init a OFDM for demodulation info
        ofdm_info = new OFDM(receiver.sampleRate);

        symbolLength = ofdm_info.symbolNSamples;
    }

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public void demodulateSymbol() {
        // skip the cyclic prefix
        receiver.tickDone += ofdm_info.cyclicPrefixNSamples;

        // get the samples of the symbol
        float[] samples = new float[symbolLength];
        for (int i = 0; i < symbolLength; i++) {
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

        // dump the amplitudes
        FileOp.outputFloatSeq(amplitudes, "amplitudes.csv");

        // dump the symbol samples
        FileOp.outputFloatSeq(samples, "symbol.csv");
    }

    /* Demodulate all to demodulate */
    public void demodulate() {
        while (receiver.tickDone + ofdm_info.cyclicPrefixNSamples + symbolLength < 
            receiver.getLength()) {
            demodulateSymbol();
        }
    }

    /* Demodulate test */
    public void demodulate_test() {
        while (receiver.tickDone + ofdm_info.cyclicPrefixNSamples + symbolLength < 
            receiver.getLength()) {
            demodulateSymbol();
            receiver.unpacking = false;
            break;
        }
    }
}
