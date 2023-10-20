package com.AcousticNFC;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Config {

    Host host;

    /* values directly assigned are editable on the panel 
     * The calculated ones are not editable, but can be changed by changing the editable ones
     *      Once one of the editable ones is changed, the calculated ones are updated
    */
    
    public double sampleRate = 44100;

    public int frameLength = 40;
    public int symbolLength = 256;

    public double cyclicPrefixLength = 0.004;
    public int cyclicPrefixNSamples;
    public boolean cyclicPrefixMute = true;      

    public int subcarrierDist = 2;
    public double subCarrierWidth;
    public double bandWidthLowEdit = 4000;
    public double bandWidthHighEdit = 6000;
    public double bandWidthLow;
    public double bandWidthHigh;
    public int numSubCarriers;
    
    public int keyingCapacity = 1;
    public int symbolCapacity;

    public float SoF_amplitude = 0.8f;
    public double SoF_T = 0.002905; // The 'T' parameter of SoF, see DOC
    public int sofNSamples;
    public double SoF_fmin = 200;
    public double SoF_fmax = 6000;
    public double SofSilencePeriod = 0.004;
    public int sofSilentNSamples;

    public double maxSofCorrDetect = 0;
    public double SofDetectThreshld = 1000; // The threshold for correlation detection

    // debug shared info
    public ArrayList<Boolean> transmitted;
    public int allSymbolLength;

    // compensation
    public int sofAlignCompensate = -9;

    public class ConfigPanel extends JPanel {
        private Config config;

        JLabel sampleRateField;
        JTextField subcarrierDistField;
        JTextField frameLengthField;
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
            frameLengthField = new JTextField(Integer.toString(config.frameLength));
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

            // debug fields
            firstSymbolPhasesField = new JLabel("");
            firstSymbolDataField = new JLabel("");

            // statistics
            BERField = new JLabel("");

            // Add a button to update the Config object with the entered values
            JButton updateButton = new JButton("Update");
            updateButton.addActionListener(e -> {
                if (config.host.isBusy()) {
                    JOptionPane.showMessageDialog(ConfigPanel.this, "System is busy. Please try again later.", "Save Config", JOptionPane.WARNING_MESSAGE);
                } else {
                    // update all config
                    config.sampleRate = Double.parseDouble(sampleRateField.getText());
                    config.subcarrierDist = Integer.parseInt(subcarrierDistField.getText());
                    config.frameLength = Integer.parseInt(frameLengthField.getText());
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
                    
                    ConfigChange();
                    updateDisplay();
                }
            });

            // The button to reset the observed max correlation
            JButton resetMaxCorrButton = new JButton("Reset Max Corr");
            resetMaxCorrButton.addActionListener(e -> {
                config.maxSofCorrDetect = 0;
                maxSofCorrDectField.setText(Double.toString(config.maxSofCorrDetect));
            });

            // The button to set the SoF detect threshold as 90% of the observed max correlation
            JButton setSofDetectThresholdButton = new JButton("Set 90%");
            setSofDetectThresholdButton.addActionListener(e -> {
                config.SofDetectThreshld = 0.9 * config.maxSofCorrDetect;
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

            grid1.add(new JLabel("Frame Length(Bits):"));
            grid1.add(frameLengthField);

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
            frameLengthField.setText(Integer.toString(config.frameLength));
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
        }
    }

    ConfigPanel panel;

    public Config(Host host) {
        this.host = host;

        // Create a panel with text fields for each field in the Config class
        panel = new ConfigPanel(this);

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
            (int) Math.round((bandWidthHigh - bandWidthLow) / subCarrierWidth);
        symbolCapacity = numSubCarriers * keyingCapacity;
        sofNSamples = (int)(2 * SoF_T * sampleRate);
        sofSilentNSamples = (int)(SofSilencePeriod * sampleRate);

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
