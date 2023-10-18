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

    public double SoF_T = 0.002905; // The 'T' parameter of SoF, see DOC
    public int sofNSamples = (int)(2 * SoF_T * sampleRate);
    public double SofSilencePeriod = 0.004;
    public int sofSilentNSamples = (int)(SofSilencePeriod * sampleRate);
}
