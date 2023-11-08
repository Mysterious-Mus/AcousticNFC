package com.AcousticNFC.mac;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.sound.sampled.AudioFileFormat.Type;

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
                event = Event.IDLE;
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
    public static MacFrame[] distribute( ArrayList<Boolean> bitString) {
        /* break the large data into frames */
        byte[] input = TypeConvertion.booleanList2ByteArray(bitString);

        int payloadlen = Math.ceilDiv(Host.cfg.packBitLen , 8);
        int frameNum = Math.ceilDiv(Host.cfg.transmitBitLen, Host.cfg.packBitLen);
         

        byte destinationAddress = 1;
        byte sourceAddress = 1;
        byte type = 0;

        MacFrame[] macFrames = new MacFrame[frameNum];
        for (int i = 0; i < frameNum; i++) {
            int start = i * payloadlen;
            int end = Math.min(start + payloadlen, Host.cfg.transmitBitLen);
            // Copy the input bytes to the frame array
            byte[] data = new byte[payloadlen];

            System.arraycopy(input, start, data, 0, end - start);
            
            // pad the data with 0s
            if (end - start < payloadlen) {
                Arrays.fill(data, end - start, payloadlen, (byte) 0);
            }
            // Add mac header
            macFrames[i] = new MacFrame(destinationAddress, sourceAddress, type, data);
        }

        return macFrames;
    }

    /**
     * Send the data
     * @param bitString to be sent 
     * @return None
     */
    public void send( ArrayList<Boolean> bitString) {

        System.out.println("start sending data");
        MacFrame[] frames = distribute(bitString);

        //! Test !!! transmit the first frame
        for (int frameID = 0; frameID < 1; frameID++) {
            // physical Layer
            physicalManager.send(frames[frameID]);
        }
        System.out.println(ANSI.ANSI_BLUE + "send successfully" + ANSI.ANSI_RESET);
    }

    public Event receive() {
        System.out.println("Start receiving");
        MacFrame frame = new MacFrame(physicalManager.receive());
        computeBRR(frame);
        // The frame is received, now check the CRC
        if (frame.is_valid == false) {
            return Event.CRC_ERROR;
        }
        else if (frame.type == 0xFF) {
            // check Data type 
            // TODO: check the destination address
            return Event.VALID_ACK;
        }
        return Event.VALID_DATA;
    }

    public void computeBRR(MacFrame frame) {
        ArrayList<Boolean> data =  TypeConvertion.byteArray2BooleanList(frame.data);

        int numErrors = 0;

        for (int i = 0; i < Host.cfg.packBitLen; i++) {
            if (data.get(i) != Host.cfg.transmitted.get(i)) {
                numErrors++;
            }
        }
        // print first bits of transmitted and get
        int packCnt = Math.ceilDiv(Host.cfg.packBitLen, Host.cfg.packBitLen);
        int bound = Host.cfg.packBitLen;
        int groupLen = 40;
        for (int packIdx = 0; packIdx < packCnt; packIdx ++) {
            System.out.println("GroupDiffs " + packIdx + ":");
            for (int groupId = 0; groupId < Math.ceil((double) Host.cfg.packBitLen / groupLen); groupId++) {
                int groupDiff = 0;
                for (int i = 0; i < groupLen; i++) {
                    if (packIdx * Host.cfg.packBitLen + groupId * groupLen + i < bound) {
                        groupDiff += Host.cfg.transmitted.get(packIdx * Host.cfg.packBitLen + groupId * groupLen + i) == 
                            data.get(packIdx * Host.cfg.packBitLen + groupId * groupLen + i) ? 0 : 1;
                    }
                }
                System.out.print(groupDiff + " ");
            }
            System.out.println();
        }
        Host.cfg.UpdBER((double)numErrors / Host.cfg.packBitLen);
    }

}
