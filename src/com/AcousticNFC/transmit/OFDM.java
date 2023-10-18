package com.AcousticNFC.transmit;

import com.AcousticNFC.Config;
import com.AcousticNFC.receive.Receiver;

/* Use OFDM to modulate a bit string
 * Always padded with 0s, the length of the modulated signal will be a multiple of:
 * keyingCapacity * numSubCarriers
 * The maximum amplitude of the modulated signal is 0.2
 */
public class OFDM {

    Config cfg;
    
    public OFDM(Config cfg_src) {
        cfg = cfg_src;
    }

    /* Applies PSK modulation to a binary string of length 'keyingCapacity'. 
     * Here, the maximum amplitude is 0.2.
     * The Cyclical Prefix is not added.
     * Bits are kept in their original order: the first bit is the most significant bit,
     * to create a 'key' index.
     * Phase for each 'key' is calculated as: 2 * PI * index / numKeys.
    */
    public float[] phaseShiftKeying(int[] data, double carrierFreq) {
        // sanity: data length should be equal to keyingCapacity
        assert data.length == cfg.keyingCapacity;

        // number of keys
        int numKeys = (int) Math.pow(2, cfg.keyingCapacity);

        // Index
        int index = 0;
        for (int i = 0; i < cfg.keyingCapacity; i++) {
            index += data[i] * Math.pow(2, cfg.keyingCapacity - i - 1);
        }

        // Phase
        float phase = (float) (2 * Math.PI * index / numKeys);

        // Modulated signal
        float[] modulatedSignal = new float[cfg.symbolLength];
        for (int i = 0; i < cfg.symbolLength; i++) {
            double t = (double) i / cfg.sampleRate;
            // use cos because we use complex representation
            modulatedSignal[i] = 0.8F * (float) Math.cos(2 * Math.PI * carrierFreq * t + phase);
        }

        return modulatedSignal;
    }

    /* Generate an OFDM symbol 
     * The input length should be numSubCarriers * keyingCapacity
     * The subcarriers with lower frequencies transmit the former bits
     * Maximum amplitude is 0.2
     * Cyclical Prefix is added before the data
    */
    public float[] symbolGen(int[] data) {
        // Sanity: data length should be equal to numSubCarriers * keyingCapacity
        assert data.length == cfg.numSubCarriers * cfg.keyingCapacity;

        // Determine the number of samples per symbol
        int numSamplesPerWholeSymbol = cfg.cyclicPrefixNSamples + cfg.symbolLength;

        // Generate the symbol
        float[] symbol = new float[numSamplesPerWholeSymbol];
        for (int i = 0; i < cfg.numSubCarriers; i++) {
            // Get the subcarrier frequency
            double carrierFreq = cfg.bandWidthLow + i * cfg.subCarrierWidth;

            // Get the data for this subcarrier
            int[] subCarrierData = new int[cfg.keyingCapacity];
            for (int j = 0; j < cfg.keyingCapacity; j++) {
                subCarrierData[j] = data[i * cfg.keyingCapacity + j];
            }

            // Modulate the subcarrier
            float[] modulatedSubCarrier = phaseShiftKeying(subCarrierData, carrierFreq);

            // Add the subcarrier to the symbol
            for (int j = 0; j < cfg.symbolLength; j++) {
                symbol[cfg.cyclicPrefixNSamples + j] += modulatedSubCarrier[j] / cfg.numSubCarriers;
            }
        }

        // Add the cyclic prefix
        // for (int i = 0; i < cyclicPrefixNSamples; i++) {
        //     symbol[i] = symbol[cyclicPrefixNSamples + i];
        // }

        return symbol;
    }

    /* Modulate the input data using OFDM
     * The actual output is padded with 0s to make the length a multiple of
     * keyingCapacity * numSubCarriers = symbolCapacity
     * So make sure the packet length is known by the receiver.
     * The maximum amplitude is 0.2.
     * Cyclical Prefix is added before each symbol.
     */
    public float[] modulate(int[] data) {
        // pad the input with 0s to make the length a multiple of symbolCapacity
        int numSymbols = (int) Math.ceil((double) data.length / cfg.symbolCapacity);
        int[] paddedData = new int[numSymbols * cfg.symbolCapacity];
        for (int i = 0; i < data.length; i++) {
            paddedData[i] = data[i];
        }
        // Sanity check
        assert paddedData.length % cfg.symbolCapacity == 0;

        // modulate
        int resultNSamples = numSymbols * (cfg.cyclicPrefixNSamples + cfg.symbolLength);
        float[] result = new float[resultNSamples];
        for (int i = 0; i < numSymbols; i++) {
            // Get the data for this symbol
            int[] symbolData = new int[cfg.symbolCapacity];
            for (int j = 0; j < cfg.symbolCapacity; j++) {
                symbolData[j] = paddedData[i * cfg.symbolCapacity + j];
            }

            
            // Generate the symbol
            float[] symbol = symbolGen(symbolData);

            // Add the symbol to the result
            for (int j = 0; j < cfg.cyclicPrefixNSamples + cfg.symbolLength; j++) {
                result[i * (cfg.cyclicPrefixNSamples + cfg.symbolLength) + j] = symbol[j];
            }
        }

        return result;
    }
}
