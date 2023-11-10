package com.AcousticNFC.physical;

import java.util.Set;

import javax.swing.JPanel;

import com.AcousticNFC.Config;
import com.AcousticNFC.Host;
import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.receive.Receiver;
import com.AcousticNFC.physical.transmit.EthernetPacket;
import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.TypeConvertion;
import com.AcousticNFC.utils.CyclicBuffer;
import com.synthbot.jasiohost.AsioChannel;
import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.UI.panels.ChannelSelectPanel;

public class PhysicalManager {

    private String physMgrName;
    private EthernetPacket ethernetPacket;
    private CyclicBuffer<Float> sampleBuffer = new CyclicBuffer<Float>(Config.PHYSICAL_BUFFER_SIZE);
    private AsioChannel receiveChannel;
    private AsioChannel sendChannel;
    public interface ChannelChangedListener {
        public void ChannelChanged(AsioChannel channel);
    }
    private ChannelChangedListener inChangedListener = new ChannelChangedListener() {
        @Override
        public void ChannelChanged(AsioChannel channel) {
            ASIOHost.unregisterReceiver(receiveChannel);
            receiveChannel = channel;
            ASIOHost.registerReceiver(receiveChannel, sampleBuffer);
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

    public PhysicalManager(String name) {
        this.physMgrName = name;
        ethernetPacket = new EthernetPacket();
        Host.receiver = new Receiver();

        // at construction, defaultly register the first available channel
        // for both input and output
        receiveChannel = ASIOHost.availableInChannels.iterator().next();
        ASIOHost.registerReceiver(receiveChannel, sampleBuffer);
        sendChannel = ASIOHost.availableOutChannels.iterator().next();
        ASIOHost.registerPlayer(sendChannel);

        channelSelectPanel = new ChannelSelectPanel(
            physMgrName, receiveChannel, sendChannel, inChangedListener, outChangedListener);
    }

    public ChannelSelectPanel getChannelSelectPanel() {
        return channelSelectPanel;
    }

    /**
     * The send function in Physical Layer
     *
     * @param frame: the frame to be sent
     * @return void
     */
    public void send(MacFrame macframe) {
        float [] samples = ethernetPacket.getPacket(macframe.frame);

        // play the samples
        Host.player = new Player(samples);
        Host.setState(Host.State.PLAYING);
    }

    /**
     * The receive function in Physical Layer
     * @return the frame received
    */
    public byte[] receive() {
        System.out.println("Receiving");
        Host.receiver.receive();
        return TypeConvertion.booleanList2ByteArray(Host.receiver.demodulator.frameBuffer);
    }
}
