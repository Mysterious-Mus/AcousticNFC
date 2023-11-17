package com.AcousticNFC;

import java.util.ArrayList;
import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.UI.UIHost;
import com.AcousticNFC.mac.MacManager;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.BitString;
import com.AcousticNFC.Config;
import com.AcousticNFC.APP.TxRx;
import com.AcousticNFC.utils.AddrAlloc;

public class Main {
    
    Config config;

    public static AddrAlloc addrAlloc = new AddrAlloc();

    ASIOHost asioHost;
    public static UIHost uiHost;
    public static TxRx txrxApp;

    public Main() {
        config = new Config();
        BitString bitStr = new BitString("INPUT.bin");
        Config.transmitted = bitStr.getBitString();
        asioHost = new ASIOHost();

        txrxApp = new TxRx("TxRx 1", (byte) 0x00, (byte) 0x01);
        txrxApp = new TxRx("TxRx 2", (byte) 0x01, (byte) 0x00);
        // ui should be launched last because it has to collect all the panels,
        // also, it should wait for other threads to be ready
        uiHost = new UIHost();
    }

    public static void main(String[] args) {
        @SuppressWarnings("unused")
        Main main = new Main();
    }
}
