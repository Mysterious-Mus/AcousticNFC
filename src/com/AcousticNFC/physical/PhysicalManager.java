package com.AcousticNFC.physical;

import java.util.Set;

import com.AcousticNFC.Host;
import com.AcousticNFC.mac.EthernetFrame;
import com.AcousticNFC.physical.receive.Receiver;
import com.AcousticNFC.physical.transmit.EthernetPacket;
import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.TypeConvertion;
import com.synthbot.jasiohost.AsioChannel;

public class PhysicalManager {

    private EthernetPacket ethernetPacket;

    public PhysicalManager() {
        ethernetPacket = new EthernetPacket(Host.cfg);
        Host.receiver = new Receiver(Host.cfg);
    }

    /**
     * The send function in Physical Layer
     *
     * @param frame: the frame to be sent
     * @return void
     */
    public void send(byte[] frame) {
        float [] samples = ethernetPacket.getPacket(frame);

        // play the samples
        Host.player = new Player(samples);
    }

    /**
     * The receive function in Physical Layer
     * 
    */
    public void receive() {
        System.out.println("Receiving");
        Host.receiver.Receive();
    }


    
}
