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
    private Player player;
    private Receiver receiver;

    public PhysicalManager() {
        ethernetPacket = new EthernetPacket(Host.cfg);
        receiver = new Receiver(Host.cfg);
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
        player = new Player(samples);
    }

    //     // IO handler below, should be fast
    // public synchronized void bufferSwitch(long systemTime, long samplePosition, Set<AsioChannel> channels) {

    //     lastPosition = (int)samplePosition;
    //     // pick the first active input channel
    //     AsioChannel inputChannel = findAsioChannel(ChannelType.INPUT, channels);
    //     // pick the first active output channel
    //     AsioChannel outputChannel = findAsioChannel(ChannelType.OUTPUT, channels);

    //     // if we need to record
    //     if (state == State.RECORDING || state == State.RECORDING_PLAYING) {
    //     // if not found
    //     if (inputChannel == null) {
    //         // print error
    //         System.out.println("No active input channel found.");
    //         // stop recording
    //         setState(State.IDLE);
    //         return;
    //     }
    //     // buffer tmp
    //     float[] input = new float[bufferSize];
    //     // read from the input channel
    //     inputChannel.read(input);
    //     // record
    //     recorder.record(input);
    //     }

    //     // If we need to play (we shouldn't be modifying the content to play)
    //     if ((state == State.PLAYING || state == State.RECORDING_PLAYING) && !PlayContentLock) {
    //     // if not found
    //     if (outputChannel == null) {
    //         // print error
    //         System.out.println("No active output channel found.");
    //         // stop playing
    //         setState(State.IDLE);
    //         return;
    //     }

    //     output = new float[bufferSize];
    //     // play
    //     if (!player.playContent(bufferSize, output)) {
    //         // if this call handles the last buffer
    //         setState(State.IDLE);
    //     }
    //     // write to the output channel
    //     outputChannel.write(output);
    //     }
    //     else {
    //     // not playing, send 0
    //     for (int i = 0; i < bufferSize; i++) {
    //         output[i] = 0;
    //     }
    //     // if found
    //     if (outputChannel != null) {
    //         // write to the output channel
    //         outputChannel.write(output);
    //     }
    //     }

    //     // if we need to receive
    //     if (receiverState == ReceiverState.RECEIVING) {
    //     // if not found
    //     if (inputChannel == null) {
    //         // print error
    //         System.out.println("No active input channel found.");
    //         // stop recording
    //         setReceiverState(ReceiverState.IDLE);
    //         return;
    //     }
    //     // buffer tmp
    //     float[] input = new float[bufferSize];
    //     // read from the input channel
    //     inputChannel.read(input);
    //     // lock
    //     // BufferIntrLock.lock();
    //     // receive
    //     receiver.feedSamples(input);
    //     // unlock
    //     // BufferIntrLock.unlock();
    //     }
    //     guard --;
    // }

    
}
