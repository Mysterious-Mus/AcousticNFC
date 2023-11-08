package com.AcousticNFC.physical;

import java.util.Set;

import com.AcousticNFC.Host;
import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.receive.Receiver;
import com.AcousticNFC.physical.transmit.EthernetPacket;
import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.TypeConvertion;
import com.synthbot.jasiohost.AsioChannel;

public class PhysicalManager {

    private EthernetPacket ethernetPacket;

    public PhysicalManager() {
        ethernetPacket = new EthernetPacket();
        Host.receiver = new Receiver();
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
