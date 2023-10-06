/*
 *  Copyright 2009,2010 Martin Roth (mhroth@gmail.com)
 * 
 *  This file is part of JAsioHost.
 *
 *  JAsioHost is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JAsioHost is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JAsioHost.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.acousticnfc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

// AsioDriverListener
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioDriverState;
import com.acousticnfc.utils.Music;
import com.acousticnfc.utils.Player;
import com.acousticnfc.utils.Recorder;
import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;

/**
 * The <code>Host</code> is a GUI for all the functionalities of AcousticNFC
 */
public class AcousticHost extends JFrame implements AsioDriverListener {
  
  private static final long serialVersionUID = 1L;
  
  private AsioDriver asioDriver;
  private Set<AsioChannel> activeChannels;
  private int sampleIndex;
  private int bufferSize;
  private double sampleRate;
  private float[] output;
  // recordings
  private Recorder recorder;
  // play contents
  private Player player;
  // music generator
  private Music music;
  // working state
  private enum State { IDLE, RECORDING, PLAYING, RECORDING_PLAYING};
  private State state;

  // UI
  final JComboBox comboBox = new JComboBox(AsioDriver.getDriverNames().toArray());
  final JButton buttonRecord = new JButton("Record");
  final JButton buttonStop = new JButton("Stop");
  final JButton buttonControlPanel = new JButton("Control Panel");
  final JButton buttonReplay = new JButton("Replay");
  final JButton buttonRecordAndPlay = new JButton("Record and Play");
  
  final AsioDriverListener host = this;

  public AcousticHost() {
    // the title
    super("Acoustic NFC");
    
    activeChannels = new HashSet<AsioChannel>();

    // init recorders
    recorder = new Recorder();

    // init workstate
    state = State.IDLE;

    // button callbacks
    setButtonCallbacks();

    // init driver
    driverInit();
  
    // layout panel
    layoutPanel();
  }

  // layout panel
  public void layoutPanel() {
    this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

    // Line 1
    this.add(comboBox);

    // Line 2
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.add(buttonRecord);
    panel.add(buttonStop);
    panel.add(buttonReplay);
    panel.add(buttonControlPanel);
    this.add(panel);

    // Line 3
    this.add(buttonRecordAndPlay);

    this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent event) {
        if (asioDriver != null) {
          asioDriver.shutdownAndUnloadDriver();
        }
      }
    });
    
    this.setSize(400, 150);
    this.setResizable(false);
    this.setVisible(true);
  }

  // button callback
  public void setButtonCallbacks() {
    buttonRecord.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // restart the driver to sync the new setting
        driverShutdown();
        driverInit();
        state = State.RECORDING;
        // restart the recorder
        recorder = new Recorder();
      }
    });
    
    buttonStop.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        state = State.IDLE;
        // print the length of the recording
        System.out.println("Recording length: " + recorder.getRecordings().length);
        // output the recordings as a csv file
        recorder.outputRecordings("recordings");
      }
    });

    buttonReplay.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // set content to play: the recordings
        player = new Player(recorder.getRecordings());
        // set work state
        state = State.PLAYING;
      }
    });

    buttonControlPanel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        if (asioDriver != null && 
            asioDriver.getCurrentState().ordinal() >= AsioDriverState.INITIALIZED.ordinal()) {
          asioDriver.openControlPanel();          
        }
      }
    });

    buttonRecordAndPlay.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // restart the driver to sync the new setting
        driverShutdown();
        driverInit();
        state = State.RECORDING_PLAYING;
        // restart the recorder
        recorder = new Recorder();
        // generate music
        music = new Music(sampleRate);
        // set player to play the music
        player = new Player(music.generateChordProgression());
      }
    });
  }
  
  // initialize the driver according to the selection
  public void driverInit() {
    asioDriver = AsioDriver.getDriver(comboBox.getSelectedItem().toString());
    asioDriver.addAsioDriverListener(host);

    // activate output channels
    for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++)
    {
      AsioChannel asioChannel = asioDriver.getChannelOutput(i);
      // print channel name
      System.out.println("Output channel: " + asioChannel.getChannelName());
      activeChannels.add(asioChannel);
      // break;
    }

    // activate input channels
    for (int i = 0; i < asioDriver.getNumChannelsInput(); i++)
    {
      AsioChannel asioChannel = asioDriver.getChannelInput(i);
      // print channel name
      System.out.println("Input channel: " + asioChannel.getChannelName());
      activeChannels.add(asioChannel);
      // break;
    }

    sampleIndex = 0;
    bufferSize = asioDriver.getBufferPreferredSize();
    sampleRate = asioDriver.getSampleRate();
    output = new float[bufferSize];
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

  // IO handler below
  public void bufferSwitch(long systemTime, long samplePosition, Set<AsioChannel> channels) {
    // if we need to record
    if (state == State.RECORDING || state == State.RECORDING_PLAYING) {
      // pick the first active input channel
      AsioChannel inputChannel = null;
      // loop through the channels
      for (AsioChannel channel : channels) {
        // check if the channel is an input channel and is active
        if (channel.isActive() && channel.isInput()) {
          // get the input channel
          inputChannel = channel;
        }
      }
      // if not found
      if (inputChannel == null) {
        // print error
        System.out.println("No active input channel found.");
        // stop recording
        state = State.IDLE;
        return;
      }
      // print channel name
      System.out.println("Input channel: " + inputChannel.getChannelName());
      // buffer tmp
      float[] input = new float[bufferSize];
      // read from the input channel
      inputChannel.read(input);
      // print some input points
      System.out.println("Sample: " + input[0] + " " + input[bufferSize / 2] + " " + input[bufferSize - 1]);
      // record
      recorder.record(input);
    }

    // If we need to play
    if (state == State.PLAYING || state == State.RECORDING_PLAYING) {
      // pick the first active output channel
      AsioChannel outputChannel = null;
      // loop through the channels
      for (AsioChannel channel : channels) {
        // check if the channel is an output channel and is active
        if (channel.isActive() && !channel.isInput()) {
          // get the output channel
          outputChannel = channel;
        }
      }
      // if not found
      if (outputChannel == null) {
        // print error
        System.out.println("No active output channel found.");
        // stop playing
        state = State.IDLE;
        return;
      }
      output = new float[bufferSize];
      // play
      if (!player.playContent(bufferSize, output)) {
        // if this call handles the last buffer
        state = State.IDLE;
      }
      // write to the output channel
      outputChannel.write(output);
      // print replay info
      System.out.println("Replay: " + player.getBufferIndex() + " / " + player.getBufferLength());
      // print some points of the sample
      System.out.println("Sample: " + output[0] + " " + output[bufferSize / 2] + " " + output[bufferSize - 1]);
    }
    else {
      // not playing, send 0
      for (int i = 0; i < bufferSize; i++) {
        output[i] = 0;
      }
      // pick the first active output channel
      AsioChannel outputChannel = null;
      // loop through the channels
      for (AsioChannel channel : channels) {
        // check if the channel is an output channel and is active
        if (channel.isActive() && !channel.isInput()) {
          // get the output channel
          outputChannel = channel;
        }
      }
      // if found
      if (outputChannel != null) {
        // write to the output channel
        outputChannel.write(output);
      }
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
      }
    }.start();
  }

  public void resyncRequest() {
    System.out.println("resyncRequest() callback received.");
  }

  public void sampleRateDidChange(double sampleRate) {
    System.out.println("sampleRateDidChange() callback received.");
  }
  
  public static void main(String[] args) {
    @SuppressWarnings("unused")
    AcousticHost host = new AcousticHost();
  }

}
