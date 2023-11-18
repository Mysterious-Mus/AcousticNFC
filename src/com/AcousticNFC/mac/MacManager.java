package com.AcousticNFC.mac;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Mac;
import javax.sound.sampled.AudioFileFormat.Type;

import com.AcousticNFC.Config;
import com.AcousticNFC.Config.ConfigTerm;
import com.AcousticNFC.mac.MacFrame.Header;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.ANSI;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.sync.Permission;
import com.AcousticNFC.utils.sync.Notifier;
import com.AcousticNFC.Config.ConfigTerm;

public class MacManager {

    public static class Configs {
        public static ConfigTerm<Integer> ACK_EXPIRE_TIME = 
            new ConfigTerm<Integer>("ACK_EXPIRE_TIME", 250, false);
        public static ConfigTerm<Integer> BACKOFF_UNIT = 
            new ConfigTerm<Integer>("BACKOFF_UNIT", 150, false);

        public static ConfigTerm<Integer> BACKOFF_MAX_TIMES =
            new ConfigTerm<Integer>("BACKOFF_MAX_TIMES", 5, false);
        public static ConfigTerm<Integer> BACKOFF_AFTER_ACK =
            new ConfigTerm<Integer>("BACKOFF_AFTER_ACK", 200, false);
    }

    String appName;
    byte ADDR;

    public void syncAddr(byte ADDR) {
        this.ADDR = ADDR;
    }

    public interface FrameReceivedListener {
        public void frameReceived(MacFrame frame);
    }
    private FrameReceivedListener frameReceivedListener;

    enum State {
        IDLE,
        SENDING,
        SENDING_ACK,
        RECEIVING_HEADER,
        RECEIVING_PAYLOAD,
        ERROR
    }

    private State state;

    private PhysicalManager physicalManager;

    public interface physicalCallback {
        public void frameDetected();
        public void headerReceived(MacFrame.Header header);
        public void frameReceived(MacFrame frame);
        public void channelClear(boolean clear);
    }

    /**
     * Atomic operations for physical callbacks
     */
    private physicalCallback phyInterface = new physicalCallback() {
        @Override
        public void channelClear(boolean clear) {
            if(clear) channelClearNot.permit();
            else channelClearNot.unpermit();
        }

        @Override
        public synchronized void frameDetected() {
            // first disable detection
            physicalManager.permissions.detect.unpermit();
            switch (state) {
                case IDLE:
                    // we can start receiving
                    // enable decoding
                    physicalManager.permissions.decode.permit();
                    // set the state to receiving
                    state = State.RECEIVING_HEADER;
                    idleNot.unpermit();
                    break;
                default:
                    // print error
                    System.out.println(appName + "Error: frame detected in wrong state:" + state);
                    break;
            }
        }

        @Override
        public synchronized void headerReceived(MacFrame.Header header) {
            switch (state) {
                case RECEIVING_HEADER:
                    if (header.check() && header.getType() != null) {
                        // we have a valid header
                        switch (header.getType()) {
                            case DATA:
                                // set the state to receiving
                                state = State.RECEIVING_PAYLOAD;
                                // print message
                                // System.out.println(appName + " header received");
                                break;
                            case ACK:
                                // receiving is done
                                physicalManager.permissions.decode.unpermit();
                                physicalManager.permissions.detect.permit();
                                state = State.IDLE;
                                if (header.getField(MacFrame.Configs.HeaderFields.DEST_ADDR) == ADDR) {
                                    // notify the sender immediately
                                    ackReceived = true;
                                    idleNot.permit();
                                    ACKorExpiredNot.mNotify();
                                }
                                else {
                                    // this ack is not notifying me, so I can quickly get in
                                    idleNot.permit();
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    else {
                        // we have a deprecated header
                        // print message
                        // print fields of the header
                        // for (MacFrame.Configs.HeaderFields field: MacFrame.Configs.HeaderFields.values()) {
                        //     if (field != MacFrame.Configs.HeaderFields.COUNT)
                        //     System.out.print(field.name() + ": " + header.getField(field) + " ");
                        // }
                        // System.out.println();
                        // System.out.println(appName + " deprecated header received");
                        physicalManager.permissions.decode.unpermit();
                        physicalManager.permissions.detect.permit();
                        state = State.IDLE;
                        idleNot.permit();
                    }
                    break;
                default:
                    physicalManager.permissions.decode.unpermit();
                    physicalManager.permissions.detect.permit();
                    state = State.IDLE;
                    idleNot.permit();
                    // print error
                    System.out.println(appName + "Error: header received in wrong state:" + state);
                break;
            }
        }

        @Override
        public synchronized void frameReceived(MacFrame frame) {
            // first disable decoding
            physicalManager.permissions.decode.unpermit();
            switch (state) {
                case RECEIVING_PAYLOAD:
                
                    if (frame.verify() && frame.getHeader().getField(MacFrame.Configs.HeaderFields.DEST_ADDR) == ADDR) {
                        frameReceivedListener.frameReceived(frame);
                        // send ack
                        state = State.SENDING_ACK;
                        MacFrame.Header ackHeader = new MacFrame.Header();
                        ackHeader.SetField(MacFrame.Configs.HeaderFields.DEST_ADDR, 
                            frame.getHeader().getField(MacFrame.Configs.HeaderFields.SRC_ADDR));
                        ackHeader.SetField(MacFrame.Configs.HeaderFields.SRC_ADDR, ADDR);
                        ackHeader.SetField(MacFrame.Configs.HeaderFields.TYPE, MacFrame.Configs.Types.ACK.getValue());
                        MacFrame ackFrame = new MacFrame(
                            ackHeader,
                            new byte[0]
                            );
                        lastAckTime = System.currentTimeMillis();
                        physicalManager.send(ackFrame);
                    }

                    physicalManager.permissions.detect.permit();
                    state = State.IDLE;
                    idleNot.permit();
                    break;
                default:
                    physicalManager.permissions.decode.unpermit();
                    physicalManager.permissions.detect.permit();
                    state = State.IDLE;
                    idleNot.permit();
                    // print error
                    System.out.println(appName + "Error: frame received in wrong state:" + state);
                    break;
            }
        }
    };

    public MacManager(byte ADDR, String appName, FrameReceivedListener frameReceivedListener) {
        this.ADDR = ADDR;
        this.appName = appName;
        this.frameReceivedListener = frameReceivedListener;
        physicalManager = new PhysicalManager(
            appName,
            phyInterface);

        state = State.IDLE;
        idleNot.permit();
        physicalManager.permissions.detect.permit();
    }

    /**
     * break the data into mac frames
     * @param bitString to be sent
     * @return Macframes
     */
    public static MacFrame[] distribute(MacFrame.Header header, ArrayList<Boolean> bitString) {
        /* break the large data into frames */
        byte[] input = TypeConvertion.booleanList2ByteArray(bitString);

        int frameNum = Math.ceilDiv(input.length, MacFrame.Configs.payloadNumBytes.v());

        MacFrame[] macFrames = new MacFrame[frameNum];
        for (int i = 0; i < frameNum; i++) {
            byte[] data = Arrays.copyOfRange(input, i * MacFrame.Configs.payloadNumBytes.v(), 
                Math.min((i + 1) * MacFrame.Configs.payloadNumBytes.v(), input.length));
            
            // padding
            if (data.length < MacFrame.Configs.payloadNumBytes.v()) {
                data = Arrays.copyOf(data, MacFrame.Configs.payloadNumBytes.v());
            }
            // Add mac header
            // increment sequence number
            header.SetField(MacFrame.Configs.HeaderFields.SEQUENCE_NUM, 
                (byte) ((header.getField(MacFrame.Configs.HeaderFields.SEQUENCE_NUM) + 1)&0xFF));
            macFrames[i] = new MacFrame(header, data);
        }

        return macFrames;
    }

    Permission idleNot = new Permission(false);
    Notifier ACKorExpiredNot = new Notifier();
    Permission channelClearNot = new Permission(false);
    long lastAckTime = 0;

    /**
     * Send the data, the thread will work till send complete or send error
     * @param bitString to be sent 
     * @return Void
     */
    boolean ackReceived = false;
    public boolean interrupted = false;
    public void send(byte dstAddr, ArrayList<Boolean> bitString) {
        interrupted = false;
        System.out.println("start sending data");
        // record time
        long startTime = System.currentTimeMillis();
        // make frame header
        MacFrame.Header header = new MacFrame.Header();
        header.SetField(MacFrame.Configs.HeaderFields.DEST_ADDR, dstAddr);
        header.SetField(MacFrame.Configs.HeaderFields.SRC_ADDR, ADDR);
        header.SetField(MacFrame.Configs.HeaderFields.TYPE, MacFrame.Configs.Types.DATA.getValue());
        header.SetField(MacFrame.Configs.HeaderFields.SEQUENCE_NUM, (byte) 0);

        MacFrame[] frames = distribute(header, bitString);

        for (int frameID = 0; frameID < frames.length; frameID++) {
            ackReceived = false;
            int backoffTimes = 0;
            // physical Layer
            while (!ackReceived) {
                channelClearNot.waitTillPermitted();
                idleNot.waitTillPermitted();
                
                // wait a while, others may want to send
                if(frameID > 0 && backoffTimes == 0) {
                    try {
                        Thread.sleep(Configs.BACKOFF_AFTER_ACK.v());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    channelClearNot.waitTillPermitted();
                    idleNot.waitTillPermitted();
                }


                // print message
                // System.out.println(appName + " frame " + frameID + " not acked, backoff " + backoffTimes + " times");
                if (backoffTimes > 0 && lastAckTime + 5000 < System.currentTimeMillis()) {
                    while (true) {
                        boolean judge = (System.currentTimeMillis() - startTime) % 6000 < 3000;
                        int appId = Integer.parseInt(appName.substring(5));
                        if ((appId == 2 && judge)||(appId == 1 && !judge))
                            try {

                                    Thread.sleep(Configs.BACKOFF_AFTER_ACK.v());
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        else break;
                    }
                }
                
                backoffTimes++;
                
                if (interrupted) {
                    // print message, including app name
                    System.out.println(ANSI.ANSI_BLUE + appName + " transmission interrupted" + ANSI.ANSI_RESET);
                    return;
                }
                
                // enter sending state
                physicalManager.permissions.decode.unpermit();
                physicalManager.permissions.detect.unpermit();
                state = State.SENDING;

                physicalManager.send(frames[frameID]);

                // enter idle state
                physicalManager.permissions.decode.unpermit();
                physicalManager.permissions.detect.permit();
                state = State.IDLE;
                idleNot.permit();
                // print message
                // System.out.println(appName + " frame " + frameID + " sent");
                ACKorExpiredNot.cancelNotify();
                ACKorExpiredNot.delayedNotify(Configs.ACK_EXPIRE_TIME.v());
                ACKorExpiredNot.mWait();
                if (ackReceived) {
                    
                    System.out.println(appName + " ACK " + frameID + "/" + frames.length + " received at " +
                                        (System.currentTimeMillis() - startTime) 
                                        + " time estimated: " + 
                            (System.currentTimeMillis() - startTime) * (frames.length) / (frameID + 1));
                }
            }
        }
        System.out.println(ANSI.ANSI_BLUE + "send successfully" + ANSI.ANSI_RESET);
        // print time consumed
        System.out.println("Time consumed: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
