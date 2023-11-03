package com.AcousticNFC;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/*
 * Steps to add one parameter to panel:
 *  1. declare it as a menber of Config
 *  2. if it is a passive parameter, implement the calculation formula in Config.ConfigChange()
 *  3. add it's name label and textfield to the panel in Config.ConfigPanel()
 *  4. add it to an appropriate place of the pannel
 *  5. add a line to Config.ConfigPanel.updateDisplay() to update the textfield
 *  6. add a line to Config.ConfigPanel.updateButton.addActionListener() to update the Config object
 */


public class Config {

    Host host;

    /* values directly assigned are editable on the panel 
     * The calculated ones are not editable, but can be changed by changing the editable ones
     *      Once one of the editable ones is changed, the calculated ones are updated
    */

    // buffer size should be small!
    
    public double sampleRate = 44100;

    public int frameLength;
    public int symbolLength = 64;

    public double cyclicPrefixLength = 0.001;
    public int cyclicPrefixNSamples;
    public boolean cyclicPrefixMute = false;      

    public int subcarrierDist = 1;
    public double subCarrierWidth;
    public double bandWidthLowEdit = 3000;
    public double bandWidthHighEdit = 8000;
    public double bandWidthLow;
    public double bandWidthHigh;
    public int numSubCarriers;
    
    public int keyingCapacity = 1;
    public int symbolCapacity;

    public float SoF_amplitude = 1f;
    public double SoF_T = 0.002905; // The 'T' parameter of SoF, see DOC
    public int sofNSamples;
    public double SoF_fmin = 6000;
    public double SoF_fmax = 16000;
    public double SofSilencePeriod = 0.000;
    public int sofSilentNSamples;

    public double maxSofCorrDetect = 0;
    public double SofDetectThreshld = 0.02; // The threshold for correlation detection

    public double interPacketGapPeriod = 0.001; 
    public int interPacketGapNSamples;

    // debug shared info
    public ArrayList<Boolean> transmitted;
    public int alignNSymbol = 10;
    public int scanWindow = 100;
    public boolean alignBitFunc(int idx) {return (idx % 5 <= 2);}

    public int transmitBitLen = 1000;

    public int packBitLen = 800;
    public int alignBitLen;
    public int decodeBitLen;

    public boolean ECCOn = true;
    public boolean[][] ECCMat = {
        { true, true, true, true, false, false, true },
        { true, false, true, true, false, true, false },
    };
    public int ECCBitRate;

    // compensation
    public int sofAlignCompensate = 0;

    public class ConfigPanel extends JPanel {
        private Config config;

        JLabel sampleRateField;
        JTextField subcarrierDistField;
        JTextField transmitLenField;
        JTextField symbolLengthField;
        JTextField cyclicPrefixLengthField;
        JLabel cyclicPrefixNSamplesField;
        JCheckBox cyclicPrefixMuteField;
        JLabel subCarrierWidthField;
        JTextField bandWidthLowEditField;
        JLabel bandWidthLowField;
        JTextField bandWidthHighEditField;
        JLabel bandWidthHighField;
        JLabel numSubCarriersField;
        JTextField keyingCapacityField;
        JLabel symbolCapacityField;
        JTextField SoF_amplitudeField;
        JTextField SoF_TField;
        JLabel sofNSamplesField;
        JTextField SoF_fminField;
        JTextField SoF_fmaxField;
        JTextField SofSilencePeriodField;
        JLabel sofSilentNSamplesField;
        JLabel maxSofCorrDectField;
        JTextField SofDetectThresholdField;
        JTextField alignNSymbolField;
        JLabel frameLenField;
        JTextField packBitLenField;

        // debug info
        JLabel firstSymbolPhasesField;
        JLabel firstSymbolDataField;

        // statistics
        JLabel BERField;

        public ConfigPanel(Config config_src) {
            this.config = config_src;

            // init the UI elems
            sampleRateField = new JLabel(Double.toString(config.sampleRate));
            subcarrierDistField = new JTextField(Integer.toString(config.subcarrierDist));
            transmitLenField = new JTextField(Integer.toString(config.transmitBitLen));
            symbolLengthField = new JTextField(Integer.toString(config.symbolLength));
            cyclicPrefixLengthField = new JTextField(Double.toString(config.cyclicPrefixLength));
            cyclicPrefixNSamplesField = new JLabel(Integer.toString(config.cyclicPrefixNSamples));
            cyclicPrefixMuteField = new JCheckBox("", config.cyclicPrefixMute);
            subCarrierWidthField = new JLabel(Double.toString(config.subCarrierWidth));
            bandWidthLowEditField = new JTextField(Double.toString(config.bandWidthLowEdit));
            bandWidthLowField = new JLabel(Double.toString(config.bandWidthLow));
            bandWidthHighEditField = new JTextField(Double.toString(config.bandWidthHighEdit));
            bandWidthHighField = new JLabel(Double.toString(config.bandWidthHigh));
            numSubCarriersField = new JLabel(Integer.toString(config.numSubCarriers));
            keyingCapacityField = new JTextField(Integer.toString(config.keyingCapacity));
            symbolCapacityField = new JLabel(Integer.toString(config.symbolCapacity));
            SoF_amplitudeField = new JTextField(Float.toString(config.SoF_amplitude));
            SoF_TField = new JTextField(Double.toString(config.SoF_T));
            sofNSamplesField = new JLabel(Integer.toString(config.sofNSamples));
            SoF_fminField = new JTextField(Double.toString(config.SoF_fmin));
            SoF_fmaxField = new JTextField(Double.toString(config.SoF_fmax));
            SofSilencePeriodField = new JTextField(Double.toString(config.SofSilencePeriod));
            sofSilentNSamplesField = new JLabel(Integer.toString(config.sofSilentNSamples));
            maxSofCorrDectField = new JLabel(Double.toString(config.maxSofCorrDetect));
            SofDetectThresholdField = new JTextField(Double.toString(config.SofDetectThreshld));
            alignNSymbolField = new JTextField(Integer.toString(config.alignNSymbol));
            frameLenField = new JLabel(Integer.toString(config.frameLength));
            packBitLenField = new JTextField(Integer.toString(config.packBitLen));

            // debug fields
            firstSymbolPhasesField = new JLabel("");
            firstSymbolDataField = new JLabel("");

            // statistics
            BERField = new JLabel("");

            // Add a button to update the Config object with the entered values
            JButton updateButton = new JButton("Update");
            updateButton.addActionListener(e -> {
                // update all config
                config.sampleRate = Double.parseDouble(sampleRateField.getText());
                config.subcarrierDist = Integer.parseInt(subcarrierDistField.getText());
                config.transmitBitLen = Integer.parseInt(transmitLenField.getText());
                config.symbolLength = Integer.parseInt(symbolLengthField.getText());
                config.cyclicPrefixLength = Double.parseDouble(cyclicPrefixLengthField.getText());
                config.cyclicPrefixMute = cyclicPrefixMuteField.isSelected();
                config.bandWidthLowEdit = Double.parseDouble(bandWidthLowEditField.getText());
                config.bandWidthHighEdit = Double.parseDouble(bandWidthHighEditField.getText());
                config.keyingCapacity = Integer.parseInt(keyingCapacityField.getText());
                config.SoF_amplitude = Float.parseFloat(SoF_amplitudeField.getText());
                config.SoF_T = Double.parseDouble(SoF_TField.getText());
                config.SoF_fmin = Double.parseDouble(SoF_fminField.getText());
                config.SoF_fmax = Double.parseDouble(SoF_fmaxField.getText());
                config.SofSilencePeriod = Double.parseDouble(SofSilencePeriodField.getText());
                config.SofDetectThreshld = Double.parseDouble(SofDetectThresholdField.getText());
                config.alignNSymbol = Integer.parseInt(alignNSymbolField.getText());
                config.packBitLen = Integer.parseInt(packBitLenField.getText());
                
                ConfigChange();
            });

            // The button to reset the observed max correlation
            JButton resetMaxCorrButton = new JButton("Reset Max Corr");
            resetMaxCorrButton.addActionListener(e -> {
                config.maxSofCorrDetect = 0;
                maxSofCorrDectField.setText(Double.toString(config.maxSofCorrDetect));
            });

            // The button to set the SoF detect threshold as 90% of the observed max correlation
            JButton setSofDetectThresholdButton = new JButton("Set 70%");
            setSofDetectThresholdButton.addActionListener(e -> {
                config.SofDetectThreshld = 0.7 * config.maxSofCorrDetect;
                SofDetectThresholdField.setText(Double.toString(config.SofDetectThreshld));
            });

            // The button to set the threshold back to the default value, disabling the SoF detection
            JButton disableSofDetectThresholdButton = new JButton("Disable Detection");
            disableSofDetectThresholdButton.addActionListener(e -> {
                config.SofDetectThreshld = 1000;
                SofDetectThresholdField.setText(Double.toString(config.SofDetectThreshld));
            });

            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JPanel grid1 = new JPanel();

            grid1.setLayout(new GridLayout(0, 4));

            grid1.add(new JLabel("Sample Rate(Hz):"));
            grid1.add(sampleRateField);
            grid1.add(new JLabel("Subcarrier Dist:"));
            grid1.add(subcarrierDistField);

            grid1.add(new JLabel("Symbol Length(Bits):"));
            grid1.add(symbolLengthField);
            grid1.add(new JLabel("Sub Carrier Width(Hz):"));
            grid1.add(subCarrierWidthField);
            
            grid1.add(new JLabel("Cyclic Prefix Length(s):"));
            grid1.add(cyclicPrefixLengthField);
            grid1.add(new JLabel("Cyclic Prefix Length(Samples):"));
            grid1.add(cyclicPrefixNSamplesField);
            
            grid1.add(new JLabel("Cyclic Prefix Mute:"));
            grid1.add(cyclicPrefixMuteField);
            grid1.add(new JLabel(""));
            grid1.add(new JLabel(""));
            
            grid1.add(new JLabel("Band Width Low Edit(Hz):"));
            grid1.add(bandWidthLowEditField);
            grid1.add(new JLabel("Band Width Low(Hz):"));
            grid1.add(bandWidthLowField);
            
            grid1.add(new JLabel("Band Width High Edit(Hz):"));
            grid1.add(bandWidthHighEditField);
            grid1.add(new JLabel("Band Width High(Hz):"));
            grid1.add(bandWidthHighField);
            
            grid1.add(new JLabel(""));
            grid1.add(new JLabel(""));
            grid1.add(new JLabel("Number of Sub Carriers:"));
            grid1.add(numSubCarriersField);

            grid1.add(new JLabel("Keying Capacity:"));
            grid1.add(keyingCapacityField);
            grid1.add(new JLabel("Symbol Capacity:"));
            grid1.add(symbolCapacityField);

            grid1.add(new JLabel("SoF Amplitude:"));
            grid1.add(SoF_amplitudeField);
            grid1.add(new JLabel(""));
            grid1.add(new JLabel(""));
            
            grid1.add(new JLabel("SoF T(s):"));
            grid1.add(SoF_TField);
            grid1.add(new JLabel("SoF NSamples:"));
            grid1.add(sofNSamplesField);
            
            grid1.add(new JLabel("SoF Fmin(Hz):"));
            grid1.add(SoF_fminField);
            grid1.add(new JLabel("SoF Fmax(Hz):"));
            grid1.add(SoF_fmaxField);
            
            grid1.add(new JLabel("SoF Silence Period(s):"));
            grid1.add(SofSilencePeriodField);
            grid1.add(new JLabel("SoF Silent NSamples:"));
            grid1.add(sofSilentNSamplesField);

            grid1.add(new JLabel("N Bits Transmit:"));
            grid1.add(transmitLenField);
            grid1.add(new JLabel("Align N Symbol:"));
            grid1.add(alignNSymbolField);

            grid1.add(new JLabel("Pack Bit Len:"));
            grid1.add(packBitLenField);
            grid1.add(new JLabel("Frame Length:"));
            grid1.add(frameLenField);

            this.add(grid1);
        
            JPanel updateButtonPanel = new JPanel(new BorderLayout());
            updateButtonPanel.add(updateButton, BorderLayout.CENTER);
            this.add(updateButtonPanel);

            JPanel grid2 = new JPanel();
            grid2.setLayout(new GridLayout(0, 4));

            grid2.add(new JLabel("Max SoF Corr Dectected:"));
            grid2.add(maxSofCorrDectField);
            grid2.add(new JLabel("SoF Threshold:"));
            grid2.add(SofDetectThresholdField);
            

            grid2.add(resetMaxCorrButton);
            grid2.add(setSofDetectThresholdButton);
            grid2.add(disableSofDetectThresholdButton);
            grid2.add(new JLabel(""));

            this.add(grid2);

            JPanel debugPanel = new JPanel(new GridLayout(0,2));
            debugPanel.add(new JLabel("First Symbol Phases:"));
            debugPanel.add(firstSymbolPhasesField);

            debugPanel.add(new JLabel("First Symbol Data:"));
            debugPanel.add(firstSymbolDataField);

            this.add(debugPanel);

            JPanel statisticsPanel = new JPanel(new GridLayout(0,2));
            statisticsPanel.add(new JLabel("BER:"));
            statisticsPanel.add(BERField);

            this.add(statisticsPanel);
        }

        public void updateDisplay() {
            // update all text fields
            sampleRateField.setText(Double.toString(config.sampleRate));
            subcarrierDistField.setText(Integer.toString(config.subcarrierDist));
            transmitLenField.setText(Integer.toString(config.transmitBitLen));
            symbolLengthField.setText(Integer.toString(config.symbolLength));
            cyclicPrefixLengthField.setText(Double.toString(config.cyclicPrefixLength));
            cyclicPrefixNSamplesField.setText(Integer.toString(config.cyclicPrefixNSamples));
            cyclicPrefixMuteField.setSelected(config.cyclicPrefixMute);
            subCarrierWidthField.setText(Double.toString(config.subCarrierWidth));
            bandWidthLowEditField.setText(Double.toString(config.bandWidthLowEdit));
            bandWidthLowField.setText(Double.toString(config.bandWidthLow));
            bandWidthHighEditField.setText(Double.toString(config.bandWidthHighEdit));
            bandWidthHighField.setText(Double.toString(config.bandWidthHigh));
            numSubCarriersField.setText(Integer.toString(config.numSubCarriers));
            keyingCapacityField.setText(Integer.toString(config.keyingCapacity));
            symbolCapacityField.setText(Integer.toString(config.symbolCapacity));
            SoF_amplitudeField.setText(Float.toString(config.SoF_amplitude));
            SoF_TField.setText(Double.toString(config.SoF_T));
            sofNSamplesField.setText(Integer.toString(config.sofNSamples));
            SoF_fminField.setText(Double.toString(config.SoF_fmin));
            SoF_fmaxField.setText(Double.toString(config.SoF_fmax));
            SofSilencePeriodField.setText(Double.toString(config.SofSilencePeriod));
            sofSilentNSamplesField.setText(Integer.toString(config.sofSilentNSamples));
            maxSofCorrDectField.setText(Double.toString(config.maxSofCorrDetect));
            SofDetectThresholdField.setText(Double.toString(config.SofDetectThreshld));
            alignNSymbolField.setText(Integer.toString(config.alignNSymbol));
            frameLenField.setText(Integer.toString(config.frameLength));
            packBitLenField.setText(Integer.toString(config.packBitLen));
        }
    }

    ConfigPanel panel;

    public Config(Host host) {
        this.host = host;

        // Create a panel with text fields for each field in the Config class
        panel = new ConfigPanel(this);

        ConfigChange();
    }

    public Config() {
        ConfigChange();
    }
    
    public void ConfigChange() {
        // update all config
        cyclicPrefixNSamples = 
            (int)(sampleRate * cyclicPrefixLength);
        subCarrierWidth =
            sampleRate / symbolLength * subcarrierDist;
        bandWidthLow =
            Math.ceil(bandWidthLowEdit / subCarrierWidth) * subCarrierWidth;
        bandWidthHigh =
            Math.floor(bandWidthHighEdit / subCarrierWidth) * subCarrierWidth;
        numSubCarriers =
            (int) Math.round((bandWidthHigh - bandWidthLow) / subCarrierWidth) + 1;
        symbolCapacity = numSubCarriers * keyingCapacity;
        sofNSamples = (int)(2 * SoF_T * sampleRate);
        sofSilentNSamples = (int)(SofSilencePeriod * sampleRate);

        interPacketGapNSamples = (int)(interPacketGapPeriod * sampleRate);

        alignBitLen = alignNSymbol * keyingCapacity * numSubCarriers;
        ECCBitRate = ECCMat.length;
        decodeBitLen = ECCOn? packBitLen * ECCBitRate : packBitLen;
        frameLength = alignBitLen + decodeBitLen;
        
        this.panel.updateDisplay();
    }

    public void UpdSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;

        ConfigChange();
        // tell the panel to update the text fields
        panel.updateDisplay();
    }

    public void UpdCorrdetect(double corr) {
        if (corr > maxSofCorrDetect) {
            maxSofCorrDetect = corr;
            panel.maxSofCorrDectField.setText(Double.toString(maxSofCorrDetect));
        }
    }

    public void UpdFirstSymbolPhases(String info) {
        panel.firstSymbolPhasesField.setText(info);
    }

    public void UpdFirstSymbolData(String info) {
        panel.firstSymbolDataField.setText(info);
    }

    public void UpdBER(double BER) {
        panel.BERField.setText(Double.toString(BER));
    }
}
