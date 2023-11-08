package com.AcousticNFC.ASIO;

// AsioDriverListener
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioDriverState;
import com.AcousticNFC.Config;
import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;

// ArrayList
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ASIOHost implements AsioDriverListener{

    private AsioDriver asioDriver;
    private Set<AsioChannel> activeChannels = new HashSet<AsioChannel>();
    
    private int bufferSize;

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
        // activate output channels
        for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++)
        {
          AsioChannel asioChannel = asioDriver.getChannelOutput(i);
          activeChannels.add(asioChannel);
          break;
        }
    
        // activate input channels
        for (int i = 0; i < asioDriver.getNumChannelsInput(); i++)
        {
          AsioChannel asioChannel = asioDriver.getChannelInput(i);
          activeChannels.add(asioChannel);
          break;
        }
    
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
