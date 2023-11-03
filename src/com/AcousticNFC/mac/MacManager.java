package com.AcousticNFC.mac;

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
                event = receive();
                break;
            case IDLE:
                currentState = State.IDLE;
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

        int payloadlen = (int) Host.cfg.packBitLen / 8;
        int frameNum = (input.length + payloadlen - 1) / payloadlen;
         
        byte[][] frames = new byte[frameNum][payloadlen];

        byte[] destinationAddress = new byte[] {0x00};
        byte[] sourceAddress = new byte[] {0x00};

        for (int i = 0; i < frameNum; i++) {
            int start = i * payloadlen;
            int end = Math.min(start + payloadlen, input.length);
            // Copy the input bytes to the frame array
            System.arraycopy(input, start, frames[i], 0, end - start);
            
            // pad the data with 0s
            if (end - start < payloadlen) {
                Arrays.fill(frames[i], end - start, payloadlen, (byte) 0);
            }
            // Add mac header
            frames[i] = EthernetFrame.CreateFrame(destinationAddress, sourceAddress, frames[i]);
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
        System.out.println(ANSI.ANSI_CYAN + "send successfully" + ANSI.ANSI_RESET);
    }

    public Event receive() {
        System.out.println("Start receiving");
        return Event.VALID_DATA;
    }

}
