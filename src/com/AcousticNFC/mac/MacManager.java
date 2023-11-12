package com.AcousticNFC.mac;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.sound.sampled.AudioFileFormat.Type;

import com.AcousticNFC.Config;
import com.AcousticNFC.mac.MacFrame.Header;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.ANSI;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.sync.Permission;


public class MacManager {

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
    }

    /**
     * Atomic operations for physical callbacks
     */
    private physicalCallback phyInterface = new physicalCallback() {
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
                    break;
                default:
                    // print error
                    System.out.println("Error: frame detected in wrong state:" + state);
                    break;
            }
        }

        @Override
        public synchronized void headerReceived(MacFrame.Header header) {
            switch (state) {
                case RECEIVING_HEADER:
                    if (header.check()) {
                        switch (header.getType()) {
                            case DATA:
                                // set the state to receiving
                                state = State.RECEIVING_PAYLOAD;
                                break;
                            case ACK:
                                // receiving is done
                                physicalManager.permissions.decode.unpermit();
                                physicalManager.permissions.detect.permit();
                                state = State.IDLE;
                                break;
                            default:
                                break;
                        }
                    }
                    else {
                        // receiving is done
                        physicalManager.permissions.decode.unpermit();
                        physicalManager.permissions.detect.permit();
                        state = State.IDLE;
                    }
                    break;
                default:
                    // print error
                    System.out.println("Error: header received in wrong state:" + state);
                    break;
            }
        }

        @Override
        public synchronized void frameReceived(MacFrame frame) {
            // first disable decoding
            physicalManager.permissions.decode.unpermit();
            switch (state) {
                case RECEIVING_PAYLOAD:
                
                physicalManager.permissions.detect.permit();
                state = State.IDLE;
                
                if (frame.verify() && frame.getHeader().getField(MacFrame.Configs.HeaderFields.DEST_ADDR) == ADDR) {
                        frameReceivedListener.frameReceived(frame);
                        // send ack
                        // MacFrame.Header ackHeader = new MacFrame.Header();
                        // ackHeader.SetField(MacFrame.Configs.HeaderFields.DEST_ADDR, 
                        //     frame.getHeader().getField(MacFrame.Configs.HeaderFields.SRC_ADDR));
                        // ackHeader.SetField(MacFrame.Configs.HeaderFields.SRC_ADDR, ADDR);
                        // ackHeader.SetField(MacFrame.Configs.HeaderFields.TYPE, MacFrame.Configs.Types.ACK.getValue());
                        // MacFrame ackFrame = new MacFrame(
                        //     ackHeader,
                        //     new byte[0]
                        // );
                        // physicalManager.send(ackFrame);
                    }
                    break;
                default:
                    // print error
                    System.out.println("Error: frame received in wrong state:" + state);
                    break;
            }
        }
    };

    public MacManager(byte ADDR, String appName, FrameReceivedListener frameReceivedListener) {
        this.ADDR = ADDR;
        this.frameReceivedListener = frameReceivedListener;
        physicalManager = new PhysicalManager(
            appName,
            phyInterface);

        state = State.IDLE;
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

        int frameNum = Math.ceilDiv(input.length, MacFrame.Configs.payloadNumBytes);

        MacFrame[] macFrames = new MacFrame[frameNum];
        for (int i = 0; i < frameNum; i++) {
            byte[] data = Arrays.copyOfRange(input, i * MacFrame.Configs.payloadNumBytes, 
                Math.min((i + 1) * MacFrame.Configs.payloadNumBytes, input.length));
            
            // padding
            if (data.length < MacFrame.Configs.payloadNumBytes) {
                data = Arrays.copyOf(data, MacFrame.Configs.payloadNumBytes);
            }
            // Add mac header
            macFrames[i] = new MacFrame(header, data);
        }

        return macFrames;
    }

    /**
     * Send the data, the thread will work till send complete or send error
     * @param bitString to be sent 
     * @return Void
     */
    public void send(byte dstAddr, ArrayList<Boolean> bitString) {

        System.out.println("start sending data");
        // record time
        long startTime = System.currentTimeMillis();
        // make frame header
        MacFrame.Header header = new MacFrame.Header();
        header.SetField(MacFrame.Configs.HeaderFields.DEST_ADDR, dstAddr);
        header.SetField(MacFrame.Configs.HeaderFields.SRC_ADDR, ADDR);
        header.SetField(MacFrame.Configs.HeaderFields.TYPE, MacFrame.Configs.Types.DATA.getValue());

        MacFrame[] frames = distribute(header, bitString);

        for (int frameID = 0; frameID < frames.length; frameID++) {
            // physical Layer
            physicalManager.send(frames[frameID]);
        }
        System.out.println(ANSI.ANSI_BLUE + "send successfully" + ANSI.ANSI_RESET);
        // print time consumed
        System.out.println("Time consumed: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
