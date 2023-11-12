package com.AcousticNFC;

import java.util.ArrayList;
import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.UI.UIHost;
import com.AcousticNFC.mac.MacManager;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.BitString;
import com.AcousticNFC.Config;
import com.AcousticNFC.APP.ReceiveApp;
import com.AcousticNFC.APP.TransmitApp;

public class Main {
    
    Config config;

    ASIOHost asioHost;
    public static UIHost uiHost;
    public static TransmitApp transmitApp;

    public Main() {
        config = new Config();
        BitString bitStr = new BitString("bit_string.txt");
        Config.transmitted = bitStr.getBitString();
        asioHost = new ASIOHost();

        transmitApp = new TransmitApp();
        new ReceiveApp();
        // ui should be launched last because it has to collect all the panels,
        // also, it should wait for other threads to be ready
        uiHost = new UIHost();
    }

    public static void main(String[] args) {
        Main main = new Main();
    }
}
