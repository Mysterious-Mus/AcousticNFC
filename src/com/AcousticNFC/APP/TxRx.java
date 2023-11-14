package com.AcousticNFC.APP;

import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.mac.MacManager;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.sync.Permission;
import com.AcousticNFC.utils.sync.Notifier;
import com.AcousticNFC.Config;
import com.AcousticNFC.UI.UIHost;
import com.AcousticNFC.APP.utils.AddressTxtField;

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
import com.AcousticNFC.Main;

public class TxRx {

    String name;
    byte srcAddr, dstAddr;

    MacManager macManager;

    ReceiveCtrl Ctrl;
    private boolean isReceiving = false;
    private int receiveLength;
    private int errPackCnt;
    private int errCrcCnt;
    private ArrayList<MacFrame> receivedFrames = new ArrayList<MacFrame>();
    private Notifier transmitNotify = new Notifier();

    private class TransmitThread extends Thread {
        @Override
        public void run() {
            while (true) {
                transmitNotify.mWait();
                // the former Config.transmitbitlen of config.transmitted
                macManager.syncAddr(Ctrl.getHostAddr());
                macManager.send(
                    Ctrl.getDstAddress(),
                    new ArrayList<>(Config.transmitted.subList(0, Config.transmitBitLen))
                );
            }
        }
    }
    private Thread mTransmitThread = new TransmitThread();

    private class ReceiveCtrl extends JPanel {

        AddressTxtField hostAddrTxtField, dstAddressTxtField;

        private class ReceiveBtn extends JButton {
            public ReceiveBtn() {
                super("Receive");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        receivedFrames.clear();
                        macManager.syncAddr((byte) hostAddrTxtField.getAddress());
                        isReceiving = true;
                        errPackCnt = 0;
                        errCrcCnt = 0;
                        receiveLength = Math.ceilDiv(Config.transmitBitLen, 8 * MacFrame.Configs.payloadNumBytes.v());
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
                        // if the transmit thread is running
                        if (mTransmitThread.isAlive()) {
                            // stop it
                            transmitNotify.cancelNotify();
                            macManager.interrupted = true;
                        }
                    }
                });
            }
        }

        private class TransmitBtn extends JButton {
            public TransmitBtn() {
                super("Transmit");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        transmitNotify.mNotify();
                    }
                });
            }
        }

        public ReceiveCtrl() {
            this.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

            // panel label
            gbc.gridwidth = 2; this.add(new JLabel(name), gbc);

            // host addr
            gbc.gridwidth = 1; gbc.gridy++; gbc.gridx = 0; this.add(new JLabel("Host Address: 0x"), gbc);
            gbc.gridx++; hostAddrTxtField = new AddressTxtField(
                String.format("%02x", srcAddr));
            this.add(hostAddrTxtField, gbc);

            // target address
            gbc.gridwidth = 1; gbc.gridy++; gbc.gridx = 0; this.add(new JLabel("Dst Address: 0x"), gbc);
            gbc.gridx++; dstAddressTxtField = new AddressTxtField(
                String.format("%02x", dstAddr));
            this.add(dstAddressTxtField, gbc);

            // buttons
            gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
            this.add(new ReceiveBtn(), gbc);
            gbc.gridy++; this.add(new TransmitBtn(), gbc);
            gbc.gridy++; this.add(new StopBtn(), gbc);


            // append to appctrl UIs
            UIHost.appCtrls.add(this);
        }

        public byte getHostAddr() {
            return (byte) hostAddrTxtField.getAddress();
        }

        public byte getDstAddress() {
            return (byte) dstAddressTxtField.getAddress();
        }
    }

    public TxRx(String name, byte srcAddr, byte dstAddr) {
        this.name = name;
        this.srcAddr = srcAddr;
        this.dstAddr = dstAddr;
        // claim mac manager
        macManager = new MacManager((byte) 0, name, frameReceivedListener);
        // launch UI
        Ctrl = new ReceiveCtrl();
        mTransmitThread.start();
    }

    private MacManager.FrameReceivedListener frameReceivedListener = new MacManager.FrameReceivedListener() {
        @Override
        public void frameReceived(MacFrame frame) {
            if (!isReceiving) {
                return;
            }
            // check repeat
            if (!receivedFrames.isEmpty() &&
                frame.getHeader().getField(MacFrame.Configs.HeaderFields.SEQUENCE_NUM) == 
                receivedFrames.get(receivedFrames.size() - 1).getHeader()
                .getField(MacFrame.Configs.HeaderFields.SEQUENCE_NUM))
            {        
                return;
            }
            // push in this frame
            receivedFrames.add(frame);

            // report groupdiff
            ArrayList<Boolean> data =  TypeConvertion.byteArray2BooleanList(frame.getData());

            int packIdx = receivedFrames.size() - 1;

            // check the whole package
            boolean failed = false;
            for (int i = 0; i < data.size(); i++) {
                if (packIdx * data.size() + i < Config.transmitBitLen) {
                    if (Config.transmitted.get(packIdx * data.size() + i) != data.get(i)) {
                        failed = true;
                        break;
                    }
                }
            }
            
            if (failed) {
                errPackCnt ++;
                int groupLen = 40;
                int packBitLen = MacFrame.Configs.payloadNumBytes.v() * 8;
                System.out.println("GroupDiffs " + packIdx + ":");
                for (int groupId = 0; groupId < Math.ceilDiv(packBitLen, groupLen); 
                groupId++) {
                    int groupDiff = 0;
                    for (int i = 0; i < groupLen; i++) {
                        if (packIdx * packBitLen + groupId * groupLen + i < Config.transmitBitLen) {
                            groupDiff += Config.transmitted.get(packIdx * packBitLen + groupId * groupLen + i) == 
                                data.get(groupId * groupLen + i) ? 0 : 1;
                        }
                    }
                    System.out.print(groupDiff + " ");
                }
                System.out.println();
            }

            errCrcCnt += frame.verify() ? 0 : 1;

            // if done
            if (receivedFrames.size() == receiveLength) {
                // stop receiving
                isReceiving = false;
                // print errPackCnt and errCrcCnt
                System.out.println("errPackCnt: " + errPackCnt);
                System.out.println("errCrcCnt: " + errCrcCnt);
            }
        }
    };
}