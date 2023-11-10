package com.AcousticNFC.ASIO;

// AsioDriverListener
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioDriverState;
import com.synthbot.jasiohost.AsioSampleType;
import com.AcousticNFC.Config;
import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;

// ArrayList
import java.util.HashSet;
import java.util.Set;

import com.AcousticNFC.utils.CyclicBuffer;

// Map
import java.util.Map;
import java.nio.channels.AsynchronousFileChannel;
import java.util.ArrayList;
import java.util.HashMap;

import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.TypeConvertion;

public class ASIOHost implements AsioDriverListener{

    private AsioDriver asioDriver;
    private Set<AsioChannel> activeChannels = new HashSet<AsioChannel>();
    
    private int bufferSize;

    private static Map<AsioChannel, Player> contentPlayer = new HashMap<>();
	private static Map<AsioChannel, CyclicBuffer<Float>> receiveBuffer = new HashMap<>();

	public static Set<AsioChannel> availableInChannels = new HashSet<>();
	public static Set<AsioChannel> availableOutChannels = new HashSet<>();

	/**
	 * construct the ASIO host<p>
	 * only one ASIO host should be allowed<p>
	 * 
	 * statically: <p>
	 * the physical layer can register contents to play by channel name<p>
	 * the physical layer can register receive task by registering the name along with the cyclicbuffer to feed<p>
     * to properly select channel names, each physicalManager would maintain the unselected channel names,
     * which is a statical Set
	 */
    public ASIOHost() {
        driverInit();
    }

    /**
     * register a player to play content to the channel <p>
     * @param channel
     * @param content
     */
	public static void registerPlayer(AsioChannel channel) {
        if (channel == null) {
            return;
        }
        // sanity check
        if (!availableOutChannels.contains(channel)) {
            System.out.println("Channel " + channel.toString() + " is not available.");
            return;
        }
        // assign an empty player to the channel
        contentPlayer.put(channel, new Player());
        // maintain the available list
        availableOutChannels.remove(channel);
	}

    /**
     * play content to the channel
     * @param channel
     * @param content
     */
    public static void play(AsioChannel channel, ArrayList<Float> content) {
        if (channel == null) return;
        // sanity check
        if (!contentPlayer.containsKey(channel)) {
            System.out.println("Channel " + channel + " is not registered.");
            return;
        }
        // get the player
        Player player = contentPlayer.get(channel);
        // add the content to the player
        player.addContent(content);
    }

    public static void unregisterPlayer(AsioChannel channel) {
        if (channel == null) return;
        contentPlayer.remove(channel);
        // maintain the available list
        availableOutChannels.add(channel);
    }

    /**
     * register a cyclic buffer to receive data from the channel
     * @param channel
     * @param buffer
     */
	public static void registerReceiver(AsioChannel channel, CyclicBuffer<Float> buffer) {
        if (channel == null) return;
		receiveBuffer.put(channel, buffer);
        // maintain the available list
        availableInChannels.remove(channel);
	}

    /**
     * unregister a player
     * @param channel
     */
    public static void unregisterReceiver(AsioChannel channel) {
        if (channel == null) return;
        receiveBuffer.remove(channel);
        // maintain the available list
        availableInChannels.add(channel);
    }

    public synchronized void bufferSwitch(long systemTime, long samplePosition, Set<AsioChannel> channels) {
		// Create a copy of the keySet because we are removing during the loop
		Set<AsioChannel> keys = new HashSet<>(contentPlayer.keySet());
		// look up all the play channels
		for (AsioChannel channel : keys) {
			// get the player
            if (channel == null) continue;
			Player player = contentPlayer.get(channel);
            if (player.empty()) continue;
			// check if the channel exists
			if (channel == null || !channels.contains(channel)) {
				// report channel not found
				System.out.println("Channel " + channel + " not found.");
				// remove the registration
                unregisterPlayer(channel);
			}
			else {
				// get the float[] from the player
				float[] content = new float[bufferSize];
				boolean isLastBuffer = player.playContent(bufferSize, content);
				// copy the content to the channel
				channel.write(content);
			}
		}
		
		// look up all receiving channels
		for (AsioChannel channel : receiveBuffer.keySet()) {
			// check if the channel exists
            if (channel == null) continue;
			if (!channels.contains(channel)) {
				// report channel not found
				System.out.println("Channel " + channel + " not found.");
				unregisterReceiver(channel);		
			}
			else {
				// get the buffer
				CyclicBuffer<Float> buffer = receiveBuffer.get(channel);
				// read the content from the channel
				float[] content = new float[bufferSize];
				channel.read(content);
				// write the content to the buffer
				buffer.pusharr(TypeConvertion.floatArr2FloatList(content));
			}
		}
    }

    public void driverInit() {
        try {
            asioDriver = AsioDriver.getDriver("ASIO4ALL v2");
        }
        catch (Exception e) {
            System.out.println("ASIO driver not found.");
            // shutdown program
            System.exit(0);
            return;
        }
        asioDriver.addAsioDriverListener(this);
    
        // clear channels
        activeChannels.clear();
        availableInChannels.clear();
        availableOutChannels.clear();
        // activate output channels
        for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++)
        {
            AsioChannel asioChannel = asioDriver.getChannelOutput(i);
            activeChannels.add(asioChannel);
            availableOutChannels.add(asioChannel);
        }
		
        // activate input channels
        for (int i = 0; i < asioDriver.getNumChannelsInput(); i++)
        {
			AsioChannel asioChannel = asioDriver.getChannelInput(i);
            activeChannels.add(asioChannel);
            availableInChannels.add(asioChannel);
        }

		// print the number of input and output channels
		System.out.println("Number of input channels: " + asioDriver.getNumChannelsInput());
		System.out.println("Number of output channels: " + asioDriver.getNumChannelsOutput());
		
        bufferSize = asioDriver.getBufferPreferredSize();
        double sampleRate = asioDriver.getSampleRate();
        // if sample rate is not 44100, throw warning
        if (Math.abs(sampleRate - 44100) > 1e-6) {
          System.out.println("Warning: sample rate is not 44100. System will probably fail.");
        }
        Config.UpdSampleRate(sampleRate);
        asioDriver.createBuffers(activeChannels);
        asioDriver.start();
      }
    
      public void driverShutdown() {
        if (asioDriver != null) {
          asioDriver.shutdownAndUnloadDriver();
          activeChannels.clear();
          asioDriver = null;
        }
      }

    public void bufferSizeChanged(int bufferSize) {
        System.out.println("bufferSizeChanged() callback received.");
    }

    public void latenciesChanged(int inputLatency, int outputLatency) {
        System.out.println("latenciesChanged() callback received.");
    }

    public void resetRequest() {
        /*
        * This thread will attempt to shut down the ASIO driver. However, it will
        * block on the AsioDriver object at least until the current method has returned.
        */
        new Thread() {
        @Override
        public void run() {
            System.out.println("resetRequest() callback received. Returning driver to INITIALIZED state.");
            asioDriver.returnToState(AsioDriverState.INITIALIZED);

            // reboot
            driverShutdown();
            driverInit();
        }
        }.start();
    }

    public void resyncRequest() {
        System.out.println("resyncRequest() callback received.");
    }

    public void sampleRateDidChange(double sampleRate) {
        System.out.println("sampleRateDidChange() callback received.");
    }
}
