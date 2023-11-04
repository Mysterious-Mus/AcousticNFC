package com.AcousticNFC.mac;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import com.AcousticNFC.Host;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.ANSI;
import com.AcousticNFC.utils.TypeConvertion;


public class MacManager {

    enum State {
        IDLE,
        SENDING,
        SENDING_ACK,
        RECEIVING,
        ERROR
    }

    private State currentState;

    private PhysicalManager physicalManager;

    public MacManager() {
        physicalManager = new PhysicalManager();
        // Initial state is IDLE
        currentState = State.IDLE;
        event = Event.IDLE;
    }

    public void process() {
        switch (currentState) {
            case IDLE:
                handleIdleState();
                break;
            case SENDING:
                handleSendingState();
                break;
            case RECEIVING:
                handleReceivingState();
                break;
            case ERROR:
                handleErrorState();
                break;
            default:
                break;
        }
    }

    private void handleIdleState() {
        switch (event) {
            case TxPENDING:
                currentState = State.SENDING;
                send(Host.cfg.transmitted);
                event = Event.TxDONE;
                break;
                
                case FRAME_DETECTED:
                currentState = State.RECEIVING;
                System.out.println(ANSI.ANSI_GREEN + "Frame Detected!" + ANSI.ANSI_RESET);
                event = receive();
                break;

                case IDLE:
                currentState = State.IDLE;
                if (Host.receiver.sofDetector.detect() == true) {
                    
                    // Note: If data race happened to "event", the "event" will be overwritten, receiver comes first.
                    event = Event.FRAME_DETECTED;
                }
                break;

            default:
                currentState = State.ERROR;
                break;
        }
        return;
    }

    private void handleSendingState() {
        switch (event) {
            case TxDONE:
                currentState = State.IDLE;
                event = Event.IDLE;
                break;

            default:
                currentState = State.ERROR;

                break;
        }
    }

    private void handleReceivingState() {
        switch (event) {
            case CRC_ERROR:
                currentState = State.IDLE;
                //TODO: drop the frame
                break;
            case VALID_ACK:
                currentState = State.IDLE;
                //TODO: clear timeout
                break;
            
            case VALID_DATA:
                currentState = State.SENDING;
                //TODO: MAC layer process the data
                event = Event.TxDONE;
                break;
            default:
                currentState = State.ERROR;

                break;
        }
    }

    private void handleErrorState() {
        System.err.println(ANSI.ANSI_RED + "Error State" + ANSI.ANSI_RESET);
    }

    public enum Event {
        TxPENDING,
        TxDONE,
        FRAME_DETECTED,
        CRC_ERROR,
        VALID_ACK,
        VALID_DATA,
        IDLE
    }

    public Event event;

    /**
     * break the data into mac frames
     * @param bitString to be sent
     * @return Macframes
     */
    public static byte[][] distribute( ArrayList<Boolean> bitString) {
        /* break the large data into frames */
        byte[] input = TypeConvertion.booleanListByteArrayTo(bitString);

        int payloadlen = Math.ceilDiv(Host.cfg.packBitLen , 8);
        int frameNum = Math.ceilDiv(Host.cfg.transmitBitLen, Host.cfg.packBitLen);
         
        byte[][] frames = new byte[frameNum][payloadlen];

        byte[] destinationAddress = new byte[] {(byte)0x91};
        byte[] sourceAddress = new byte[] {(byte)0x5F};
        byte[] type = new byte[] {0x00};

        for (int i = 0; i < frameNum; i++) {
            int start = i * payloadlen;
            int end = Math.min(start + payloadlen, Host.cfg.transmitBitLen);
            // Copy the input bytes to the frame array
            System.arraycopy(input, start, frames[i], 0, end - start);
            
            // pad the data with 0s
            if (end - start < payloadlen) {
                Arrays.fill(frames[i], end - start, payloadlen, (byte) 0);
            }
            // Add mac header
            frames[i] = EthernetFrame.CreateFrame(destinationAddress, sourceAddress, type, frames[i]);
            System.out.println("length" + frames[i].length);
            for (byte element : frames[i]) {
            String hexString = Integer.toHexString(element & 0xFF);
            if (hexString.length() == 1) {
                hexString = "0" + hexString;
            }
            System.out.print(hexString + " ");
            }
            System.out.println();

        }

        return frames;
    }

    /**
     * Send the data
     * @param bitString to be sent 
     * @return None
     */
    public void send( ArrayList<Boolean> bitString) {

        System.out.println("start sending data");
        byte[][] frames = distribute(bitString);

        //! Test !!! transmit the first frame
        for (int frameID = 0; frameID < 1; frameID++) {
            // physical Layer
            physicalManager.send(frames[frameID]);
        }
        System.out.println(ANSI.ANSI_BLUE + "send successfully" + ANSI.ANSI_RESET);
    }

    public Event receive() {
        System.out.println("Start receiving");
        byte[] frame = physicalManager.receive();
        // The frame is received, now check the CRC
        if (EthernetFrame.checkCRC(frame)) {
            return Event.CRC_ERROR;
        }
        else if (EthernetFrame.getType(frame) == new byte[] {(byte)0xFF}) {
            // check Data type 
            // TODO: check the destination address
            return Event.VALID_ACK;
        }
        byte[] data = EthernetFrame.getData(frame);
        for (byte element : frame) {
            String hexString = Integer.toHexString(element & 0xFF);
            if (hexString.length() == 1) {
                hexString = "0" + hexString;
            }
            System.out.print(hexString + " ");
        }
        System.out.println();
        return Event.VALID_DATA;
    }

}
