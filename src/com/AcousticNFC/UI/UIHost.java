package com.AcousticNFC.UI;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UIHost extends JFrame{
    
    public UIHost() {
        super("Acoustic NFC");
        setCloseOp();
    }

    private void setCloseOp() {
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Your custom logic here
                System.out.println("Window is closing");
                // call System.exit(0) or dispose() as required
                System.exit(0);
            }
        });
    }
}
