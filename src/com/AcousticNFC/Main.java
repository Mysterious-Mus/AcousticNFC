package com.AcousticNFC;

import java.util.ArrayList;
import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.UI.UIHost;
import com.AcousticNFC.mac.MacManager;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.BitString;
import com.AcousticNFC.Config;

public class Main {
    
    Config config;

    ASIOHost asioHost;
    public static MacManager macManager;
    public static UIHost uiHost;

    public Main() {
        config = new Config();
        BitString bitStr = new BitString("bit_string.txt");
        Config.transmitted = bitStr.getBitString();
        asioHost = new ASIOHost();
        macManager = new MacManager();
        uiHost = new UIHost();
    }

    public static void main(String[] args) {
        Main main = new Main();
    }
}
