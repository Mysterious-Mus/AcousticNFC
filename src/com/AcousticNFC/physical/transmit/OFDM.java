package com.AcousticNFC.physical.transmit;

import java.util.ArrayList;

import com.AcousticNFC.Config;
import com.AcousticNFC.physical.receive.Receiver;

/* Use OFDM to modulate a bit string
 * Always padded with 0s, the length of the modulated signal will be a multiple of:
 * keyingCapacity * numSubCarriers
 * The maximum amplitude of the modulated signal is 0.2
 */
public class OFDM {

    public static class Configs {
        public static Integer ASK_CAPACITY = 1;
    }

    public OFDM() {
    }

    /**
     * Applies PSK modulation to a binary string of length 'keyingCapacity'. 
     * Here, the maximum amplitude is 0.2.
     * The Cyclical Prefix is not added.
     * Bits are kept in their original order: the first bit is the most significant bit,
     * to create a 'key' index.
     * Phase for each 'key' is calculated as: 2 * PI * index / numKeys.
    */
    public static float[] phaseShiftKeying(int[] data, double carrierFreq) {
        // sanity: data length should be equal to keyingCapacity
        assert data.length == Config.PSkeyingCapacity;

        // number of keys
        int numKeys = (int) Math.pow(2, Config.PSkeyingCapacity);

        // Index
        int index = 0;
        for (int i = 0; i < Config.PSkeyingCapacity; i++) {
            index += data[i] * Math.pow(2, Config.PSkeyingCapacity - i - 1);
        }

        // Phase
        float phase = (float) (2 * Math.PI * index / numKeys);

        // Modulated signal
        float[] modulatedSignal = new float[Config.symbolLength];
        for (int i = 0; i < Config.symbolLength; i++) {
            double t = (double) i / Config.sampleRate;
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
    public static float[] symbolGen(int[] data) {
        // Sanity: data length should be equal to numSubCarriers * keyingCapacity
        assert data.length == Config.numSubCarriers * Config.PSkeyingCapacity;

        // Determine the number of samples per symbol
        int numSamplesPerWholeSymbol = Config.cyclicPrefixNSamples + Config.symbolLength;

        // Generate the symbol
        float[] symbol = new float[numSamplesPerWholeSymbol];
        int usedDataPtr = 0;
        for (int i = 0; i < Config.numSubCarriers; i++) {
            // Get the subcarrier frequency
            double carrierFreq = Config.bandWidthLow + i * Config.subCarrierWidth;

            int[] subCarrierPhaseData = new int[Config.PSkeyingCapacity];
            System.arraycopy(data, usedDataPtr, subCarrierPhaseData, 0, Config.PSkeyingCapacity);
            usedDataPtr += Config.PSkeyingCapacity;
            // apply PSK modulation to the subcarrier
            float[] modulatedSubCarrier = phaseShiftKeying(subCarrierPhaseData, carrierFreq);

            int ampIdx;
            // the first carrier is special, it should determine the unit amplitude
            if (i == 0) {
                ampIdx = 0;
            }
            else {
                int[] subCarrierAmpData = new int[Configs.ASK_CAPACITY];
                System.arraycopy(data, usedDataPtr, subCarrierAmpData, 0, Configs.ASK_CAPACITY);
                usedDataPtr += Configs.ASK_CAPACITY;
                // calc ampIdx
                ampIdx = 0;
                for (int j = 0; j < Configs.ASK_CAPACITY; j++) {
                    ampIdx += subCarrierAmpData[j] * (1 << Configs.ASK_CAPACITY - j - 1);
                }
            }

            // apply amplitude modulation
            int Nlevels = 1 << Configs.ASK_CAPACITY;
            for (int j = 0; j < Config.symbolLength; j++) {
                modulatedSubCarrier[j] *= (float) (ampIdx + 1) / Nlevels;
            }

            // Add the subcarrier to the symbol
            for (int j = 0; j < Config.symbolLength; j++) {
                symbol[Config.cyclicPrefixNSamples + j] += modulatedSubCarrier[j] / Config.numSubCarriers;
            }
        }

        // Add the cyclic prefix
        for (int i = Config.cyclicPrefixNSamples / 2; i < Config.cyclicPrefixNSamples; i++) {
            symbol[i] = Config.cyclicPrefixMute ? 0 :
                symbol[numSamplesPerWholeSymbol - Config.cyclicPrefixNSamples + i];
        }

        return symbol;
    }

    /* Modulate the input data using OFDM
     * The actual output is padded with 0s to make the length a multiple of
     * keyingCapacity * numSubCarriers = symbolCapacity
     * So make sure the packet length is known by the receiver.
     * The maximum amplitude is 0.2.
     * Cyclical Prefix is added before each symbol.
     */
    public static float[] modulate(ArrayList<Boolean> data) {
        // pad the input with 0s to make the length a multiple of symbolCapacity
        int numSymbols = (int) Math.ceil((double) data.size() / Config.symbolCapacity);
        boolean[] paddedData = new boolean[numSymbols * Config.symbolCapacity];
        for (int i = 0; i < data.size(); i++) {
            paddedData[i] = data.get(i);
        }
        // Sanity check
        assert paddedData.length % Config.symbolCapacity == 0;

        // modulate
        int resultNSamples = numSymbols * (Config.cyclicPrefixNSamples + Config.symbolLength);
        float[] result = new float[resultNSamples];
        for (int i = 0; i < numSymbols; i++) {
            // Get the data for this symbol
            int[] symbolData = new int[Config.symbolCapacity];
            for (int j = 0; j < Config.symbolCapacity; j++) {
                symbolData[j] = paddedData[i * Config.symbolCapacity + j] ? 1 : 0;
            }

            // Generate the symbol
            float[] symbol = symbolGen(symbolData);

            // Add the symbol to the result
            for (int j = 0; j < Config.cyclicPrefixNSamples + Config.symbolLength; j++) {
                result[i * (Config.cyclicPrefixNSamples + Config.symbolLength) + j] = symbol[j];
            }
        }

        return result;
    }
}
