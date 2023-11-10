package com.AcousticNFC.UI;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;

import com.AcousticNFC.Main;

public class UIHost extends JFrame{
    
    public UIHost() {
        super("Acoustic NFC");
        setCloseOp();
        layoutPanel();
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

    private void layoutPanel() {
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.X_AXIS));
        // column 1 subpanel: channel selections
        JPanel chanSelCol = new JPanel();
        // boxlayout Y
        chanSelCol.setLayout(new BoxLayout(chanSelCol, BoxLayout.Y_AXIS));
        // add channel selection panels
        // chanSelCol.add(Main.physicalManager.getChannelSelectPanel());
        for (int i = 0; i < Main.physicalManagers.size(); i++) {
            chanSelCol.add(Main.physicalManagers.get(i).getChannelSelectPanel());
        }

        // add first column
        this.add(chanSelCol);

        this.setSize(1000, 600);
        this.setResizable(false);
        this.setVisible(true);
    }
}
