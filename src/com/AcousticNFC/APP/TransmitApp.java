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

public class TransmitApp {

    private MacManager.FrameReceivedListener frameReceivedListener = new MacManager.FrameReceivedListener() {
        @Override
        public void frameReceived(MacFrame frame) {
        }
    };

    MacManager macManager;

    private TaskNotify transmitNotify = new TaskNotify();
    TransmitCtrl transmitCtrl;
    
    // working thread
    private Thread mTransmitThread = new Thread() {
        @Override
        public void run() {
            while (true) {
                transmitNotify.waitTask();
                macManager.send(
                    transmitCtrl.getSrcAddress(),
                    transmitCtrl.getTgtAddress(),
                    Config.transmitted
                );
            }
        }
    };

    private class TransmitCtrl extends JPanel {

        AddressTxtField tgtAddressTxtField, srcAddressTxtField;
        TransmitBtn transmitBtn;

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

        private class TransmitBtn extends JButton {
            public TransmitBtn() {
                super("Transmit");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        transmitNotify.notifyTask();
                    }
                });
            }
        }

        public TransmitCtrl() {
            this.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

            // panel label
            gbc.gridwidth = 2; this.add(new JLabel("Transmit"), gbc);

            // src addr
            gbc.gridwidth = 1; gbc.gridy++; this.add(new JLabel("Source Address: 0x"), gbc);
            gbc.gridx++; srcAddressTxtField = new AddressTxtField(); this.add(srcAddressTxtField, gbc);

            // target address
            gbc.gridwidth = 1; gbc.gridy++; gbc.gridx = 0; this.add(new JLabel("Target Address: 0x"), gbc);
            gbc.gridx++; tgtAddressTxtField = new AddressTxtField(); this.add(tgtAddressTxtField, gbc);

            // button transmit
            gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
            transmitBtn = new TransmitBtn(); this.add(transmitBtn, gbc);

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
    public TransmitApp() {
        // claim mac manager
        macManager = new MacManager("TransmitApp", frameReceivedListener);
        mTransmitThread.start();
        // launch UI
        transmitCtrl = new TransmitCtrl();
    }
}
