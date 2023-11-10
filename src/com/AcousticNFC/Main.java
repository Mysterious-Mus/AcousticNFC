package com.AcousticNFC;

import java.util.ArrayList;
import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.UI.UIHost;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.Config;

public class Main {
    
    Config config;

    ASIOHost asioHost;
    public static ArrayList<PhysicalManager> physicalManagers = new ArrayList<>();
    UIHost uiHost;

    public Main() {
        config = new Config();
        asioHost = new ASIOHost();
        for (int i = 0; i < 2; i++) {
            physicalManagers.add(new PhysicalManager("PhysicalManager" + i));
        }
        uiHost = new UIHost();
    }

    public static void main(String[] args) {
        Main main = new Main();
    }
}
