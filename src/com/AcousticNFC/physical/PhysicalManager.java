package com.AcousticNFC.physical;

import javax.swing.JPanel;

import java.util.ArrayList;

import com.AcousticNFC.Config;
import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.receive.Receiver;
import com.AcousticNFC.physical.transmit.EthernetPacket;
import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.sync.Permission;
import com.AcousticNFC.utils.CyclicBuffer;
import com.synthbot.jasiohost.AsioChannel;
import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.ASIO.ASIOHost.NewBufferListener;
import com.AcousticNFC.UI.panels.ChannelSelectPanel;
import com.AcousticNFC.physical.transmit.SoF;
import com.AcousticNFC.utils.sync.TaskNotify;
import com.AcousticNFC.mac.MacManager.physicalCallback;
import com.AcousticNFC.physical.receive.Demodulator;

public class PhysicalManager {

    private String physMgrName;
    private CyclicBuffer<Float> sampleBuffer = new CyclicBuffer<Float>(Config.PHYSICAL_BUFFER_SIZE);
    private AsioChannel receiveChannel;
    private AsioChannel sendChannel;

    private TaskNotify newSampleNotify = new TaskNotify();

    private physicalCallback macInterface;

    /**
     * Interface for ASIOHost for sample receiving
     */
    private NewBufferListener receiveListener = new NewBufferListener() {
        @Override
        public void handleNewBuffer(float[] buffer) {
            // if both detect and decode are not permitted, discard the samples
            if (!permissions.detect.isPermitted() && !permissions.decode.isPermitted()) {
                sampleBuffer.setFIW(sampleBuffer.tailIdx() + buffer.length);
            }
            else {
                sampleBuffer.pusharr(TypeConvertion.floatArr2FloatList(buffer));
                newSampleNotify.notifyTask();
            }
        }
    };

    /**
     * Interface for channelSelUI to listen to channel change events
     */
    public interface ChannelChangedListener {
        public void ChannelChanged(AsioChannel channel);
    }
    private ChannelChangedListener inChangedListener = new ChannelChangedListener() {
        @Override
        public void ChannelChanged(AsioChannel channel) {
            ASIOHost.unregisterReceiver(receiveChannel);
            receiveChannel = channel;
            ASIOHost.registerReceiver(receiveChannel, receiveListener);
        }
    };
    private ChannelChangedListener outChangedListener = new ChannelChangedListener() {
        @Override
        public void ChannelChanged(AsioChannel channel) {
            ASIOHost.unregisterPlayer(sendChannel);
            sendChannel = channel;
            ASIOHost.registerPlayer(sendChannel);
        }
    };
    private ChannelSelectPanel channelSelectPanel;

    public class Permissions {
        public Permission detect = new Permission(false);
        public Permission decode = new Permission(false);
    }

    public Permissions permissions = new Permissions();

    private class ReceivingStates {
        public enum Status {
            DETECTING,
            DECODING,
        };
        Status status;
        public int decodeStartIdx;

        public ReceivingStates() {
            status = Status.DETECTING;
            decodeStartIdx = 0;
        }
    }

    ReceivingStates receivingStates = new ReceivingStates();

    private Thread detectThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                permissions.detect.waitTillPermitted();
                if (permissions.decode.isPermitted()) {
                    System.out.println("Error: detectThread: both detect and decode are permitted");
                }
                newSampleNotify.waitTask();
                detectFrame();
            }
        }
    });

    private Thread decodeThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                permissions.decode.waitTillPermitted();
                newSampleNotify.waitTask();
                decode();
            }
        }
    });

    public PhysicalManager(String name, physicalCallback macInterface) {
        this.physMgrName = name;
        this.macInterface = macInterface;

        // at construction, defaultly register the first available channel
        // for both input and output
        receiveChannel = ASIOHost.availableInChannels.iterator().next();
        ASIOHost.registerReceiver(receiveChannel, receiveListener);
        sendChannel = ASIOHost.availableOutChannels.iterator().next();
        ASIOHost.registerPlayer(sendChannel);

        channelSelectPanel = new ChannelSelectPanel(
            physMgrName, receiveChannel, sendChannel, inChangedListener, outChangedListener);

        // launch receiving thread
        detectThread.start();
        decodeThread.start();
    }

    public ChannelSelectPanel getChannelSelectPanel() {
        return channelSelectPanel;
    }

    /**
     * The send function in Physical Layer<p>
     * send the MACFRAME<p>
     * will block the thread until the frame is sent<p>
     *
     * @param frame: the frame to be sent
     * @return void
     */
    public void send(MacFrame macframe) {
        float [] samples = EthernetPacket.getPacket(macframe.getWhole());

        // play the samples
        ASIOHost.play(sendChannel, TypeConvertion.floatArr2FloatList(samples));
        
        // wait till the samples are played
        ASIOHost.waitTransmit(sendChannel);
    }

    /**
     * perform SoF detection<p>
     * pipeline:<p>
     * 1. find a point with correlation > threshold<p>
     * 2. find the greatest point in the window<p>
     */
    private void detectFrame() {
        // we say the candidate is the index of the first sample of the SoF
        // where should our candidates be?
        int earlyCandidate = sampleBuffer.FIW;
        int lateCandidate = sampleBuffer.tailIdx() - Config.sofNSamples;
        int lateCandidateWindow = lateCandidate - Config.SofDetectWindow;
        for (int threshCheckIdx = earlyCandidate; threshCheckIdx <= lateCandidateWindow; threshCheckIdx ++) {
            double corrCheck = SoF.calcCorr(sampleBuffer, threshCheckIdx);
            if (corrCheck > Config.SofDetectThreshld) {
                double maxCorr = corrCheck;
                int maxCorrIdx = threshCheckIdx;
                for (int windowCheckIdx = threshCheckIdx + 1; windowCheckIdx <= threshCheckIdx + Config.SofDetectWindow; windowCheckIdx ++) {
                    double corrWindowCheck = SoF.calcCorr(sampleBuffer, windowCheckIdx);
                    if (corrWindowCheck > maxCorr) {
                        maxCorr = corrWindowCheck;
                        maxCorrIdx = windowCheckIdx;
                    }
                }
                // update the states
                frameDetAct(maxCorrIdx);
                return;
            }
        }
        // discard samples before and including lateCandidateWindow
        sampleBuffer.setFIW(lateCandidateWindow + 1);
    }

    private void frameDetAct(int maxCorrIdx) {
        // callback
        macInterface.frameDetected();
        // if decoding is permitted
        if (permissions.decode.isPermitted()) {
            // update states
            receivingStates.decodeStartIdx = maxCorrIdx + Config.sofNSamples + Config.sofSilentNSamples;
            receivingStates.status = ReceivingStates.Status.DECODING;
            // discard SoF samples
            sampleBuffer.setFIW(receivingStates.decodeStartIdx);
            // clear frameBuffer
            frameBuffer.clear();
        }
        else {
            // discard all samples
            sampleBuffer.setFIW(sampleBuffer.tailIdx());
        }
    }

    ArrayList<Boolean> frameBuffer = new ArrayList<Boolean>();
    private void decode() {
        // sanity check
        if(receivingStates.status != ReceivingStates.Status.DECODING) {
            System.out.println("Error: decode() called when not in DECODING state");
            return;
        }

        // decode till samples are used up or we have a frame
        while (frameBuffer.size() < MacFrame.getFrameBitLen()) {
            // get the next sample
            float[] symbolSamples = popNxtSample();
            if (symbolSamples == null) {
                // if not enough samples, terminate
                break;
            }
            // decode the sample
            ArrayList<Boolean> bits = Demodulator.demodulateSymbol(symbolSamples);
            // add the bits to the frameBuffer
            frameBuffer.addAll(bits);
        }

        // if we gets a frame
        if (frameBuffer.size() >= MacFrame.getFrameBitLen()) {
            // pop padding
            while (frameBuffer.size() > MacFrame.getFrameBitLen()) {
                frameBuffer.remove(frameBuffer.size() - 1);
            }
            // convert to MacFrame
            MacFrame frame = new MacFrame(frameBuffer);
            // invoke callback
            macInterface.frameReceived(frame);
        }
    }

    private float[] popNxtSample() {
        // check buffer size
        if (sampleBuffer.size() < Config.cyclicPrefixNSamples + Config.symbolLength) {
            return null;
        }

        // skip the cyclic prefix
        sampleBuffer.setFIW(sampleBuffer.FIW + Config.cyclicPrefixNSamples);

        // get the samples of the symbol
        float[] samples = new float[Config.symbolLength];
        for (int i = 0; i < Config.symbolLength; i++) {
            samples[i] = sampleBuffer.popFront();
        }

        return samples;
    }
}
