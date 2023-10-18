package com.AcousticNFC;

public class Config {

    /* values directly assigned are editable on the panel 
     * The calculated ones are not editable, but can be changed by changing the editable ones
     *      Once one of the editable ones is changed, the calculated ones are updated
    */
    
    public double sampleRate = 44100;

    public int frameLength = 1024;
    public int symbolLength = 256;

    public double cyclicPrefixLength = 0.004;
    public int cyclicPrefixNSamples = 
        (int)(sampleRate * cyclicPrefixLength);
    public boolean cyclicPrefixMute = true;      

    public double subCarrierWidth = 
        sampleRate / symbolLength;
    public double bandWidthLowEdit = 4000;
    public double bandWidthHighEdit = 6000;
    public double bandWidthLow =
        Math.ceil(bandWidthLowEdit / subCarrierWidth) * subCarrierWidth;
    public double bandWidthHigh =
        Math.floor(bandWidthHighEdit / subCarrierWidth) * subCarrierWidth;
    public int numSubCarriers = 
        (int) Math.round((bandWidthHigh - bandWidthLow) / subCarrierWidth);
    
    public int keyingCapacity = 1;
    public int symbolCapacity = numSubCarriers * keyingCapacity;

    public float SoF_amplitude = 0.8f;
    public double SoF_T = 0.003; // The 'T' parameter of SoF, see DOC
    public int sofNSamples = (int)(2 * SoF_T * sampleRate);
    public double SoF_fmin = 6000;
    public double SoF_fmax = 12000;
    public double SofSilencePeriod = 0.004;
    public int sofSilentNSamples = (int)(SofSilencePeriod * sampleRate);

    public int sofDetectWindowLen = 1000;
    public double sofDetectWindowSensitivity = 30;

    public Config() {
        // print all config
        System.out.println("Config:");
        System.out.println("sampleRate: " + sampleRate);
        System.out.println("frameLength: " + frameLength);
        System.out.println("symbolLength: " + symbolLength);
        System.out.println("cyclicPrefixLength: " + cyclicPrefixLength);
        System.out.println("cyclicPrefixNSamples: " + cyclicPrefixNSamples);
        System.out.println("cyclicPrefixMute: " + cyclicPrefixMute);
        System.out.println("subCarrierWidth: " + subCarrierWidth);
        System.out.println("bandWidthLowEdit: " + bandWidthLowEdit);
        System.out.println("bandWidthHighEdit: " + bandWidthHighEdit);
        System.out.println("bandWidthLow: " + bandWidthLow);
        System.out.println("bandWidthHigh: " + bandWidthHigh);
        System.out.println("numSubCarriers: " + numSubCarriers);
        System.out.println("keyingCapacity: " + keyingCapacity);
        System.out.println("symbolCapacity: " + symbolCapacity);
        System.out.println("SoF_T: " + SoF_T);
        System.out.println("sofNSamples: " + sofNSamples);
        System.out.println("SofSilencePeriod: " + SofSilencePeriod);
        System.out.println("sofSilentNSamples: " + sofSilentNSamples);
        System.out.println("sofDetectWindowLen: " + sofDetectWindowLen);
        System.out.println("sofDetectWindowSensitivity: " + sofDetectWindowSensitivity);
    }

    public void UpdSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;

        cyclicPrefixNSamples = 
            (int)(sampleRate * cyclicPrefixLength);
        subCarrierWidth =
            sampleRate / symbolLength;
        bandWidthLow =
            Math.ceil(bandWidthLowEdit / subCarrierWidth) * subCarrierWidth;
        bandWidthHigh =
            Math.floor(bandWidthHighEdit / subCarrierWidth) * subCarrierWidth;
        numSubCarriers =
            (int) Math.round((bandWidthHigh - bandWidthLow) / subCarrierWidth);
        symbolCapacity = numSubCarriers * keyingCapacity;
        sofNSamples = (int)(2 * SoF_T * sampleRate);
        sofSilentNSamples = (int)(SofSilencePeriod * sampleRate);
    }
}
