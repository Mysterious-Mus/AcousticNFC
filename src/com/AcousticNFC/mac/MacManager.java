package com.AcousticNFC.mac;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.sound.sampled.AudioFileFormat.Type;

import com.AcousticNFC.Config;
import com.AcousticNFC.Host;
import com.AcousticNFC.physical.PhysicalManager;
import com.AcousticNFC.utils.ANSI;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.sync.Permission;


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

    public interface physicalCallback {
        public void frameDetected();
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
            switch (currentState) {
                case IDLE:
                    // we can start receiving
                    // enable decoding
                    physicalManager.permissions.decode.permit();
                    // set the state to receiving
                    currentState = State.RECEIVING;
                    break;
                default:
                    break;
            }
        }

        @Override
        public synchronized void frameReceived(MacFrame frame) {
            // first disable decoding
            physicalManager.permissions.decode.unpermit();
            switch (currentState) {
                case RECEIVING:
                    computeBER(frame);
                    backToIdle();
                    break;
                default:
                    break;
            }
        }
    };

    public MacManager() {
        physicalManager = new PhysicalManager(
            "Physical Manager",
            phyInterface);

        backToIdle();
    }

    /**
     * Go back to idle state
     * immediately start transmitting the next frame if there is any
     */
    private synchronized void backToIdle() {
        currentState = State.IDLE;
        // enable detection
        physicalManager.permissions.detect.permit();
    }

    /**
     * break the data into mac frames
     * @param bitString to be sent
     * @return Macframes
     */
    public static MacFrame[] distribute( ArrayList<Boolean> bitString) {
        /* break the large data into frames */
        byte[] input = TypeConvertion.booleanList2ByteArray(bitString);

        int payloadlen = Math.ceilDiv(Config.packBitLen , 8);
        int frameNum = Math.ceilDiv(Config.transmitBitLen, Config.packBitLen);
         

        byte destinationAddress = 1;
        byte sourceAddress = 1;
        byte type = 0;

        MacFrame[] macFrames = new MacFrame[frameNum];
        for (int i = 0; i < frameNum; i++) {
            int start = i * payloadlen;
            int end = Math.min(start + payloadlen, Config.transmitBitLen);
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
     * Send the data, the thread will wait till send complete or send error
     * @param bitString to be sent 
     * @return Void
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

    /**
     * A method for testing
     * compute BER for the first frame sent and report
     */
    public static void computeBER(MacFrame frame) {
        ArrayList<Boolean> data =  TypeConvertion.byteArray2BooleanList(frame.data);

        int numErrors = 0;

        for (int i = 0; i < Config.packBitLen; i++) {
            if (data.get(i) != Config.transmitted.get(i)) {
                numErrors++;
            }
        }
        // print first bits of transmitted and get
        int packCnt = Math.ceilDiv(Config.packBitLen, Config.packBitLen);
        int bound = Config.packBitLen;
        int groupLen = 40;
        for (int packIdx = 0; packIdx < packCnt; packIdx ++) {
            System.out.println("GroupDiffs " + packIdx + ":");
            for (int groupId = 0; groupId < Math.ceil((double) Config.packBitLen / groupLen); groupId++) {
                int groupDiff = 0;
                for (int i = 0; i < groupLen; i++) {
                    if (packIdx * Config.packBitLen + groupId * groupLen + i < bound) {
                        groupDiff += Config.transmitted.get(packIdx * Config.packBitLen + groupId * groupLen + i) == 
                            data.get(packIdx * Config.packBitLen + groupId * groupLen + i) ? 0 : 1;
                    }
                }
                System.out.print(groupDiff + " ");
            }
            System.out.println();
        }
        Config.UpdBER((double)numErrors / Config.packBitLen);
    }

}
