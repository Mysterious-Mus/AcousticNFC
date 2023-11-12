package com.AcousticNFC.APP;

import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.mac.MacManager;
import com.AcousticNFC.utils.sync.Permission;
import com.AcousticNFC.utils.sync.TaskNotify;
import com.AcousticNFC.Config;
import com.AcousticNFC.UI.UIHost;

import javax.swing.JPanel;
// gridbag
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
// label
import javax.swing.JLabel;
// text field
import javax.swing.JTextField;
// action listener
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
// array list
import java.util.ArrayList;

public class ReceiveApp {

    MacManager macManager;

    private TaskNotify receiveNotify = new TaskNotify();
    ReceiveCtrl receiveCtrl;
    private boolean isReceiving = false;
    private ArrayList<MacFrame> receivedFrames = new ArrayList<MacFrame>();

    private class ReceiveCtrl extends JPanel {

        AddressTxtField tgtAddressTxtField, srcAddressTxtField;

        private class AddressTxtField extends JTextField {
            public AddressTxtField() {
                super();
                this.setText("00");
            }

            public int getAddress() {
                int result =  Integer.parseInt(this.getText(), 16);
                // print error message if address is out of range
                if (result < MacFrame.Configs.addrLb || result > MacFrame.Configs.addrUb) {
                    System.out.println("Address out of range");
                }
                return result;
            }
        }

        private class ReceiveBtn extends JButton {
            public ReceiveBtn() {
                super("Receive");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        receivedFrames.clear();
                        isReceiving = true;
                    }
                });
            }
        }

        private class StopBtn extends JButton {
            public StopBtn() {
                super("Stop");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        isReceiving = false;
                    }
                });
            }
        }

        public ReceiveCtrl() {
            this.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

            // panel label
            gbc.gridwidth = 2; this.add(new JLabel("Receive"), gbc);

            // target address
            gbc.gridwidth = 1; gbc.gridy++; gbc.gridx = 0; this.add(new JLabel("Host Address: 0x"), gbc);
            gbc.gridx++; tgtAddressTxtField = new AddressTxtField(); this.add(tgtAddressTxtField, gbc);

            // buttons
            gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
            this.add(new ReceiveBtn(), gbc);
            gbc.gridx++; this.add(new StopBtn(), gbc);

            // append to appctrl UIs
            UIHost.appCtrls.add(this);
        }

        public byte getSrcAddress() {
            return (byte) srcAddressTxtField.getAddress();
        }

        public byte getTgtAddress() {
            return (byte) tgtAddressTxtField.getAddress();
        }
    }

    // start working thread
    public ReceiveApp() {
        // claim mac manager
        macManager = new MacManager("ReceiveApp", frameReceivedListener);
        // launch UI
        receiveCtrl = new ReceiveCtrl();
    }

    private MacManager.FrameReceivedListener frameReceivedListener = new MacManager.FrameReceivedListener() {
        @Override
        public void frameReceived(MacFrame frame) {
            if (!isReceiving) return;
            System.out.println("Frame received");
        }
    };
}
