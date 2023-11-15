package com.AcousticNFC;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.transmit.OFDM;
import com.AcousticNFC.physical.transmit.SoF;
import com.AcousticNFC.ASIO.ASIOHost;

/*
 * Steps to add one parameter to panel:
 *  1. declare it with ConfigTerm<T> class
 *  2. initialize it at the ctor Config() since some of them won't init properly before used
 *      also for proper update order
 *  2. add it with wanted order in panel
 */


public class Config {

    public static ArrayList<ConfigTerm> ConfigTermList = new ArrayList<>();
    public static Map<String, ConfigTerm> configTermsMap = new HashMap<String, ConfigTerm>();

    /**
     * A class to hold the name and value of a config term
     * The Config class holds a reference of each and is in charge of
     * modifying them during runtime parameter tuning
     * 
     * NAME is supposed to be the variable name for the sake of maintenance
     */
    public static class ConfigTerm<T> {
    
        T value = null;
        String name;
        boolean passive;

        TermDisp displayer;

        private class TermDisp {

            public JComponent displayer;

            public TermDisp() {
                this.displayer = passive? new JLabel(value2Str()) : new JTextField(value2Str());

                // set concentration lost callback
                if (!passive) {
                    ((JTextField)displayer).addActionListener(e -> {
                        // update the value
                        T newVal = fromString(((JTextField)displayer).getText());
                        if (newValCheck(newVal)) {
                            value = newVal;
                        }
                        updDisp();
                        // update the passive ones
                        Config.ConfigChange();
                    });
                }
            }

            public void updDisp() {
                if (passive) {
                    ((JLabel)displayer).setText(value2Str());
                }
                else {
                    ((JTextField)displayer).setText(value2Str());
                }
            }

        }

        public JComponent displayer() {
            return displayer.displayer;
        }

        public ConfigTerm(String name, T value, boolean passive) {
            this.name = name;
            this.value = value;
            this.passive = passive;
            this.displayer = new TermDisp();
            configTermsMap.put(name, this);
        }

        public boolean isPassive() {
            return passive;
        }

        public String value2Str() {
            if (value instanceof Integer) {
                return Integer.toString((Integer) value);
            }
            else if (value instanceof Float) {
                return Float.toString((Float) value);
            }
            else if (value instanceof Double) {
                return Double.toString((Double) value);
            }
            else if (value instanceof Boolean) {
                return Boolean.toString((Boolean) value);
            }
            else {
                return "Unsupported Type";
            }
        }

        public T fromString(String x) {
            if (value instanceof Integer) {
                return (T) Integer.valueOf(x);
            }
            else if (value instanceof Float) {
                return (T) Float.valueOf(x);
            }
            else if (value instanceof Double) {
                return (T) Double.valueOf(x);
            }
            else if (value instanceof Boolean) {
                return (T) Boolean.valueOf(x);
            }
            else {
                return null;
            }
        }
    
        public T v() {
            return value;
        }

        public void set(T x) {
            value = x;
            displayer.updDisp();
        }

        public void PassiveParamUpdVal() {
            // print not implemented
            System.out.println("PassiveParamUpdVal not implemented: " + name);
        }

        public boolean newValCheck(T newVal) {return true;};
    }

    // debug shared info
    public static ArrayList<Boolean> transmitted;

    public static int decodeBitLen;

    public static boolean ECCOn = false;
    public static boolean[][] ECCMat = {
        { true, true, true, true, false, false, true },
        { true, false, true, true, false, true, false },
    };
    public static int ECCBitRate;

    public class ConfigPanel extends JPanel {

        public ConfigPanel() {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            constructRow("sampleRate", "subCarrierDist");
            constructRow("symbolLength", "subCarrierWidth");
            constructRow("cyclicPrefixLenth", "cyclicPrefixNSamples");
            constructRow("cyclicPrefixMute", null);
            constructRow("bandWidthLowEdit", "bandWidthLow");
            constructRow("bandWidthHighEdit", "bandWidthHigh");
            constructRow("numSubCarriers", null);
            constructRow("payloadNumBytes", null);
            constructRow("PSKeyingCapacity", "ASKeyingCapacity");
            constructRow("symbolCapacity", "SoF_amplitude");
            constructRow("SoF_T", "sofNSamples");
            constructRow("SoF_fmin", "SoF_fmax");
            constructRow("SofEndMuteT", "sofEndMuteNSamples");
        }

        private void constructRow(String paramName1, String paramName2) {
            JPanel row = new JPanel();
            row.setLayout(new GridLayout(0, 4));
            if (paramName1 != null) {
                row.add(new JLabel(paramName1)); row.add(configTermsMap.get(paramName1).displayer());
            }
            else {
                row.add(new JLabel()); row.add(new JLabel());
            }
            if (paramName2 != null) {
                row.add(new JLabel(paramName2)); row.add(configTermsMap.get(paramName2).displayer());
            }
            else {
                row.add(new JLabel()); row.add(new JLabel());
            }
            this.add(row);
        }
    }
    public static ConfigPanel panel;

    public Config() {
        ConfigTermList.add(ASIOHost.Configs.sampleRate);
        ConfigTermList.add(MacFrame.Configs.payloadNumBytes);
        ConfigTermList.add(OFDM.Configs.subCarrierDist);
        ConfigTermList.add(OFDM.Configs.symbolLength);
        ConfigTermList.add(OFDM.Configs.subCarrierWidth);
        ConfigTermList.add(OFDM.Configs.cyclicPrefixLenth);
        ConfigTermList.add(OFDM.Configs.cyclicPrefixNSamples);
        ConfigTermList.add(OFDM.Configs.cyclicPrefixMute);
        ConfigTermList.add(OFDM.Configs.bandWidthLowEdit);
        ConfigTermList.add(OFDM.Configs.bandWidthLow);
        ConfigTermList.add(OFDM.Configs.bandWidthHighEdit);
        ConfigTermList.add(OFDM.Configs.bandWidthHigh);
        ConfigTermList.add(OFDM.Configs.numSubCarriers);
        ConfigTermList.add(OFDM.Configs.PSKeyingCapacity);
        ConfigTermList.add(OFDM.Configs.ASKeyingCapacity);
        ConfigTermList.add(OFDM.Configs.symbolCapacity);
        ConfigTermList.add(SoF.Configs.SoF_amplitude);
        ConfigTermList.add(SoF.Configs.SoF_T);
        ConfigTermList.add(SoF.Configs.sofNSamples);
        ConfigTermList.add(SoF.Configs.SoF_fmin);
        ConfigTermList.add(SoF.Configs.SoF_fmax);
        ConfigTermList.add(SoF.Configs.SofEndMuteT);
        ConfigTermList.add(SoF.Configs.sofEndMuteNSamples);
        ConfigTermList.add(SoF.Configs.maxSofCorrDetect);
        ConfigTermList.add(SoF.Configs.SofDetectThreshold);
        ConfigTermList.add(SoF.Configs.SofDetectWindow);

        // Create a panel with text fields for each field in the Config class
        panel = new ConfigPanel();
        ConfigChange();
    }
    
    public static void ConfigChange() {
        // check all passive configs
        for (ConfigTerm term : ConfigTermList) {
            if (term.isPassive()) {
                term.PassiveParamUpdVal();
            }
        }
        // update all config

        ECCBitRate = ECCMat.length;
        decodeBitLen = ECCOn? MacFrame.Configs.payloadNumBytes.v() * 8 * ECCBitRate : MacFrame.Configs.payloadNumBytes.v() * 8;   
    }
}