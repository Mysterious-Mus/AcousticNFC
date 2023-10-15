package com.AcousticNFC.transmit;

/* Use OFDM to modulate a bit string
 * Always padded with 0s, the length of the modulated signal will be a multiple of:
 * keyingCapacity * numSubCarriers
 * The maximum amplitude of the modulated signal is 1
 */
public class OFDM {
    
    double sampleRate;

    int subCarrierWidth = 171; // Hz
    double symbolLength; // seconds
    int symbolNSamples; // only the data part, without the cyclic prefix

    int bandWidthLow = 6000; // Hz
    int bandWidthHigh = 7000; // Hz

    int numSubCarriers;

    int symbolCapacity; // bits per symbol

    double cyclicPrefixLength = 0.004; // seconds
    int cyclicPrefixNSamples;

    int keyingCapacity = 1; // bits per subcarrier

    public OFDM(double sampleRate) {
        this.sampleRate = sampleRate;

        // recalibrate bandWidthLow: move to the next multiple
        bandWidthLow = (bandWidthLow + subCarrierWidth - 1) / subCarrierWidth * subCarrierWidth;
        // then the scans should start exactly at bandWidthLow

        // num subcarriers
        numSubCarriers = (bandWidthHigh - bandWidthLow) / subCarrierWidth + 1;

        // determine the symbol length
        symbolLength = (double) 1 / subCarrierWidth;

        // determine the number of samples per symbol
        symbolNSamples = (int) (sampleRate * symbolLength);

        // determine the number of samples per cyclic prefix
        cyclicPrefixNSamples = (int) (sampleRate * cyclicPrefixLength);

        // determine the symbol capacity
        symbolCapacity = keyingCapacity * numSubCarriers;

        // print some info
        System.out.println("OFDM parameters:");
        System.out.println("sampleRate: " + sampleRate);
        System.out.println("subCarrierWidth: " + subCarrierWidth);
        System.out.println("symbolLength: " + symbolLength);
        System.out.println("symbolNSamples: " + symbolNSamples);
        System.out.println("bandWidthLow: " + bandWidthLow);
        System.out.println("bandWidthHigh: " + bandWidthHigh);
        System.out.println("numSubCarriers: " + numSubCarriers);
        System.out.println("symbolCapacity: " + symbolCapacity);
        System.out.println("cyclicPrefixLength: " + cyclicPrefixLength);
        System.out.println("cyclicPrefixNSamples: " + cyclicPrefixNSamples);
        System.out.println("keyingCapacity: " + keyingCapacity);
    }

    /* Applies PSK modulation to a binary string of length 'keyingCapacity'. 
     * Here, the maximum amplitude is 1.
     * The Cyclical Prefix is not added.
     * Bits are kept in their original order: the first bit is the most significant bit,
     * to create a 'key' index.
     * Phase for each 'key' is calculated as: 2 * PI * index / numKeys.
    */
    public float[] phaseShiftKeying(int[] data, int carrierFreq) {
        // sanity: data length should be equal to keyingCapacity
        assert data.length == keyingCapacity;

        // number of keys
        int numKeys = (int) Math.pow(2, keyingCapacity);

        // Index
        int index = 0;
        for (int i = 0; i < keyingCapacity; i++) {
            index += data[i] * Math.pow(2, keyingCapacity - i - 1);
        }

        // Phase
        float phase = (float) (2 * Math.PI * index / numKeys);

        // Modulated signal
        float[] modulatedSignal = new float[symbolNSamples];
        for (int i = 0; i < symbolNSamples; i++) {
            double t = (double) i / sampleRate;
            // use cos because we use complex representation
            modulatedSignal[i] = (float) Math.cos(2 * Math.PI * carrierFreq * t + phase);
        }

        return modulatedSignal;
    }

    /* Generate an OFDM symbol 
     * The input length should be numSubCarriers * keyingCapacity
     * The subcarriers with lower frequencies transmit the former bits
     * Maximum amplitude is 1
     * Cyclical Prefix is added before the data
    */
    public float[] symbolGen(int[] data) {
        // Sanity: data length should be equal to numSubCarriers * keyingCapacity
        assert data.length == numSubCarriers * keyingCapacity;

        // Determine the number of samples per symbol
        int numSamplesPerWholeSymbol = cyclicPrefixNSamples + symbolNSamples;

        // Generate the symbol
        float[] symbol = new float[numSamplesPerWholeSymbol];
        for (int i = 0; i < numSubCarriers; i++) {
            // Get the subcarrier frequency
            int carrierFreq = bandWidthLow + i * subCarrierWidth;

            // Get the data for this subcarrier
            int[] subCarrierData = new int[keyingCapacity];
            for (int j = 0; j < keyingCapacity; j++) {
                subCarrierData[j] = data[i * keyingCapacity + j];
            }

            // Modulate the subcarrier
            float[] modulatedSubCarrier = phaseShiftKeying(subCarrierData, carrierFreq);

            // Add the subcarrier to the symbol
            for (int j = 0; j < symbolNSamples; j++) {
                symbol[cyclicPrefixNSamples + j] += modulatedSubCarrier[j] / numSubCarriers;
            }
        }

        // Add the cyclic prefix
        for (int i = 0; i < cyclicPrefixNSamples; i++) {
            symbol[i] = symbol[numSamplesPerWholeSymbol - cyclicPrefixNSamples + i];
        }

        return symbol;
    }

    /* Modulate the input data using OFDM
     * The actual output is padded with 0s to make the length a multiple of
     * keyingCapacity * numSubCarriers = symbolCapacity
     * So make sure the packet length is known by the receiver.
     * The maximum amplitude is 1.
     * Cyclical Prefix is added before each symbol.
     */
    public float[] modulate(int[] data) {
        // pad the input with 0s to make the length a multiple of symbolCapacity
        int numSymbols = (int) Math.ceil((double) data.length / symbolCapacity);
        int[] paddedData = new int[numSymbols * symbolCapacity];
        for (int i = 0; i < data.length; i++) {
            paddedData[i] = data[i];
        }
        // Sanity check
        assert paddedData.length % symbolCapacity == 0;

        // modulate
        int resultNSamples = numSymbols * (cyclicPrefixNSamples + symbolNSamples);
        float[] result = new float[resultNSamples];
        for (int i = 0; i < numSymbols; i++) {
            // Get the data for this symbol
            int[] symbolData = new int[symbolCapacity];
            for (int j = 0; j < symbolCapacity; j++) {
                symbolData[j] = paddedData[i * symbolCapacity + j];
            }

            // Generate the symbol
            float[] symbol = symbolGen(symbolData);

            // Add the symbol to the result
            for (int j = 0; j < cyclicPrefixNSamples + symbolNSamples; j++) {
                result[i * (cyclicPrefixNSamples + symbolNSamples) + j] = symbol[j];
            }
        }

        return result;
    }
}
