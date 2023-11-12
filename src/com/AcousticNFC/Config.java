package com.AcousticNFC;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.transmit.SoF;

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

    /* values directly assigned are editable on the panel 
     * The calculated ones are not editable, but can be changed by changing the editable ones
     *      Once one of the editable ones is changed, the calculated ones are updated
    */

    // buffer size should be small!
    
    public static double sampleRate = 44100;

    public static int frameLength;
    public static int symbolLength = 32;

    public static double cyclicPrefixLength = 0;
    public static int cyclicPrefixNSamples;
    public static boolean cyclicPrefixMute = false;      

    public static int subcarrierDist = 1;
    public static double subCarrierWidth;
    public static double bandWidthLowEdit = 0;
    public static double bandWidthHighEdit = 13000;
    public static double bandWidthLow;
    public static double bandWidthHigh;
    public static int numSubCarriers;
    
    public static int keyingCapacity = 1;
    public static int symbolCapacity;

    public static float SoF_amplitude = 1f;
    public static double SoF_T = 0.0008; // The 'T' parameter of SoF, see DOC
    /**
     * the number of SoF samples without silence
     */
    public static int sofNSamples;
    public static double SoF_fmin = 6000;
    public static double SoF_fmax = 16000;
    public static double SofSilencePeriod = 0;
    public static int sofSilentNSamples;
    public static float[] SofNoSilence;

    public static double maxSofCorrDetect = 0;
    public static double SofDetectThreshld = 0.003; // The threshold for correlation detection
    public static int SofDetectWindow = 200;

    public static double interPacketGapPeriod = 0.001; 
    public static int interPacketGapNSamples;

    // debug shared info
    public static ArrayList<Boolean> transmitted;
    public static int alignNSymbol = 0;
    public static int scanWindow = 100;
    public static boolean alignBitFunc(int idx) {return (idx % 5 <= 2);}

    public static int transmitBitLen = 6250*8;

    public static int alignBitLen;
    public static int decodeBitLen;

    public static boolean ECCOn = false;
    public static boolean[][] ECCMat = {
        { true, true, true, true, false, false, true },
        { true, false, true, true, false, true, false },
    };
    public static int ECCBitRate;

    // compensation
    public static int sofAlignCompensate = 0;

    public static int PHYSICAL_BUFFER_SIZE = 200000;

    public static class UIParams {
        public static int UIHeight = 600;
        public static int UIWidth = 1600;
    }

    public class ConfigPanel extends JPanel {

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
        JTextField payloadNBytesField;

        // statistics
        JLabel BERField;

        public ConfigPanel() {
            // init the UI elems
            sampleRateField = new JLabel(Double.toString(Config.sampleRate));
            subcarrierDistField = new JTextField(Integer.toString(Config.subcarrierDist));
            transmitLenField = new JTextField(Integer.toString(Config.transmitBitLen));
            symbolLengthField = new JTextField(Integer.toString(Config.symbolLength));
            cyclicPrefixLengthField = new JTextField(Double.toString(Config.cyclicPrefixLength));
            cyclicPrefixNSamplesField = new JLabel(Integer.toString(Config.cyclicPrefixNSamples));
            cyclicPrefixMuteField = new JCheckBox("", Config.cyclicPrefixMute);
            subCarrierWidthField = new JLabel(Double.toString(Config.subCarrierWidth));
            bandWidthLowEditField = new JTextField(Double.toString(Config.bandWidthLowEdit));
            bandWidthLowField = new JLabel(Double.toString(Config.bandWidthLow));
            bandWidthHighEditField = new JTextField(Double.toString(Config.bandWidthHighEdit));
            bandWidthHighField = new JLabel(Double.toString(Config.bandWidthHigh));
            numSubCarriersField = new JLabel(Integer.toString(Config.numSubCarriers));
            keyingCapacityField = new JTextField(Integer.toString(Config.keyingCapacity));
            symbolCapacityField = new JLabel(Integer.toString(Config.symbolCapacity));
            SoF_amplitudeField = new JTextField(Float.toString(Config.SoF_amplitude));
            SoF_TField = new JTextField(Double.toString(Config.SoF_T));
            sofNSamplesField = new JLabel(Integer.toString(Config.sofNSamples));
            SoF_fminField = new JTextField(Double.toString(Config.SoF_fmin));
            SoF_fmaxField = new JTextField(Double.toString(Config.SoF_fmax));
            SofSilencePeriodField = new JTextField(Double.toString(Config.SofSilencePeriod));
            sofSilentNSamplesField = new JLabel(Integer.toString(Config.sofSilentNSamples));
            maxSofCorrDectField = new JLabel(Double.toString(Config.maxSofCorrDetect));
            SofDetectThresholdField = new JTextField(Double.toString(Config.SofDetectThreshld));
            alignNSymbolField = new JTextField(Integer.toString(Config.alignNSymbol));
            frameLenField = new JLabel(Integer.toString(Config.frameLength));
            payloadNBytesField = new JTextField(Integer.toString(MacFrame.Configs.payloadNumBytes));

            // statistics
            BERField = new JLabel("");

            // Add a button to update the Config object with the entered values
            JButton updateButton = new JButton("Update");
            updateButton.addActionListener(e -> {
                // update all config
                Config.sampleRate = Double.parseDouble(sampleRateField.getText());
                Config.subcarrierDist = Integer.parseInt(subcarrierDistField.getText());
                Config.transmitBitLen = Integer.parseInt(transmitLenField.getText());
                Config.symbolLength = Integer.parseInt(symbolLengthField.getText());
                Config.cyclicPrefixLength = Double.parseDouble(cyclicPrefixLengthField.getText());
                Config.cyclicPrefixMute = cyclicPrefixMuteField.isSelected();
                Config.bandWidthLowEdit = Double.parseDouble(bandWidthLowEditField.getText());
                Config.bandWidthHighEdit = Double.parseDouble(bandWidthHighEditField.getText());
                Config.keyingCapacity = Integer.parseInt(keyingCapacityField.getText());
                Config.SoF_amplitude = Float.parseFloat(SoF_amplitudeField.getText());
                Config.SoF_T = Double.parseDouble(SoF_TField.getText());
                Config.SoF_fmin = Double.parseDouble(SoF_fminField.getText());
                Config.SoF_fmax = Double.parseDouble(SoF_fmaxField.getText());
                Config.SofSilencePeriod = Double.parseDouble(SofSilencePeriodField.getText());
                Config.SofDetectThreshld = Double.parseDouble(SofDetectThresholdField.getText());
                Config.alignNSymbol = Integer.parseInt(alignNSymbolField.getText());
                MacFrame.Configs.payloadNumBytes = Integer.parseInt(payloadNBytesField.getText());
                
                ConfigChange();
            });

            // The button to reset the observed max correlation
            JButton resetMaxCorrButton = new JButton("Reset Max Corr");
            resetMaxCorrButton.addActionListener(e -> {
                Config.maxSofCorrDetect = 0;
                maxSofCorrDectField.setText(Double.toString(Config.maxSofCorrDetect));
            });

            // The button to set the SoF detect threshold as 90% of the observed max correlation
            JButton setSofDetectThresholdButton = new JButton("Set 70%");
            setSofDetectThresholdButton.addActionListener(e -> {
                Config.SofDetectThreshld = 0.7 * Config.maxSofCorrDetect;
                SofDetectThresholdField.setText(Double.toString(Config.SofDetectThreshld));
            });

            // The button to set the threshold back to the default value, disabling the SoF detection
            JButton disableSofDetectThresholdButton = new JButton("Disable Detection");
            disableSofDetectThresholdButton.addActionListener(e -> {
                Config.SofDetectThreshld = 1000;
                SofDetectThresholdField.setText(Double.toString(Config.SofDetectThreshld));
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

            grid1.add(new JLabel("Pack Byte Len:"));
            grid1.add(payloadNBytesField);
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

            JPanel statisticsPanel = new JPanel(new GridLayout(0,2));
            statisticsPanel.add(new JLabel("BER:"));
            statisticsPanel.add(BERField);

            this.add(statisticsPanel);
        }

        public void updateDisplay() {
            // update all text fields
            sampleRateField.setText(Double.toString(Config.sampleRate));
            subcarrierDistField.setText(Integer.toString(Config.subcarrierDist));
            transmitLenField.setText(Integer.toString(Config.transmitBitLen));
            symbolLengthField.setText(Integer.toString(Config.symbolLength));
            cyclicPrefixLengthField.setText(Double.toString(Config.cyclicPrefixLength));
            cyclicPrefixNSamplesField.setText(Integer.toString(Config.cyclicPrefixNSamples));
            cyclicPrefixMuteField.setSelected(Config.cyclicPrefixMute);
            subCarrierWidthField.setText(Double.toString(Config.subCarrierWidth));
            bandWidthLowEditField.setText(Double.toString(Config.bandWidthLowEdit));
            bandWidthLowField.setText(Double.toString(Config.bandWidthLow));
            bandWidthHighEditField.setText(Double.toString(Config.bandWidthHighEdit));
            bandWidthHighField.setText(Double.toString(Config.bandWidthHigh));
            numSubCarriersField.setText(Integer.toString(Config.numSubCarriers));
            keyingCapacityField.setText(Integer.toString(Config.keyingCapacity));
            symbolCapacityField.setText(Integer.toString(Config.symbolCapacity));
            SoF_amplitudeField.setText(Float.toString(Config.SoF_amplitude));
            SoF_TField.setText(Double.toString(Config.SoF_T));
            sofNSamplesField.setText(Integer.toString(Config.sofNSamples));
            SoF_fminField.setText(Double.toString(Config.SoF_fmin));
            SoF_fmaxField.setText(Double.toString(Config.SoF_fmax));
            SofSilencePeriodField.setText(Double.toString(Config.SofSilencePeriod));
            sofSilentNSamplesField.setText(Integer.toString(Config.sofSilentNSamples));
            maxSofCorrDectField.setText(Double.toString(Config.maxSofCorrDetect));
            SofDetectThresholdField.setText(Double.toString(Config.SofDetectThreshld));
            alignNSymbolField.setText(Integer.toString(Config.alignNSymbol));
            frameLenField.setText(Integer.toString(Config.frameLength));
            payloadNBytesField.setText(Integer.toString(MacFrame.Configs.payloadNumBytes));
        }
    }

    public static ConfigPanel panel;

    public Config() {
        // Create a panel with text fields for each field in the Config class
        panel = new ConfigPanel();
        ConfigChange();
    }
    
    public static void ConfigChange() {
        // update all config
        cyclicPrefixNSamples = 
            (int)(sampleRate * cyclicPrefixLength);
        subCarrierWidth =
            sampleRate / symbolLength * subcarrierDist;
        bandWidthLow =
            Math.max(Math.ceil(bandWidthLowEdit / subCarrierWidth), 1) 
            * subCarrierWidth;
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
        decodeBitLen = ECCOn? MacFrame.Configs.payloadNumBytes * 8 * ECCBitRate : MacFrame.Configs.payloadNumBytes * 8;
        frameLength = MacFrame.getFrameBitLen();
        SofNoSilence = SoF.generateSoFNoSilence();
        
        panel.updateDisplay();
    }

    public static void UpdSampleRate(double sampleRate) {
        Config.sampleRate = sampleRate;

        ConfigChange();
        // tell the panel to update the text fields
        panel.updateDisplay();
    }

    public static void UpdCorrdetect(double corr) {
        if (corr > maxSofCorrDetect) {
            maxSofCorrDetect = corr;
            panel.maxSofCorrDectField.setText(Double.toString(maxSofCorrDetect));
        }
    }

    public static void UpdBER(double BER) {
        panel.BERField.setText(Double.toString(BER));
    }
}
