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

package com.AcousticNFC;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RescaleOp;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

// AsioDriverListener
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioDriverState;
import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;

import com.AcousticNFC.Config;
import com.AcousticNFC.utils.Recorder;
import com.AcousticNFC.utils.Player;
import com.AcousticNFC.utils.Music;
import com.AcousticNFC.transmit.SoF;
import com.AcousticNFC.utils.BitString;
import com.AcousticNFC.transmit.OFDM;
import com.AcousticNFC.transmit.Framer;
import com.AcousticNFC.receive.Receiver;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.*;

/**
 * The <code>Host</code> is a GUI for all the functionalities of AcousticNFC
 */
public class Host extends JFrame implements AsioDriverListener {
  
  public Config cfg;

  private static final long serialVersionUID = 1L;
  
  private AsioDriver asioDriver;
  private Set<AsioChannel> activeChannels;
  private int bufferSize;
  private float[] output;
  // recordings
  private Recorder recorder;
  // receiver
  private Receiver receiver;
  // play contents
  private Player player;
  // music generator
  private Music music;
  // working state
  private Framer framer;
  private BitString bitString;
  private enum State { IDLE, RECORDING, PLAYING, RECORDING_PLAYING};
  private State state;
  private enum ReceiverState { IDLE, RECEIVING};
  private ReceiverState receiverState;

  // UI
  final JComboBox comboBox = new JComboBox(AsioDriver.getDriverNames().toArray());
  final JButton buttonRecord = new JButton("Record");
  final JButton buttonStop = new JButton("Stop");
  final JButton buttonControlPanel = new JButton("Control Panel");
  final JButton buttonReplay = new JButton("Replay");
  final JButton buttonRecordAndPlay = new JButton("Play music and record");
  final JLabel  stateLabel = new JLabel("Idle                 ");
  final JButton buttonPj1Pt2 = new JButton("Project 1 Part 2: Generate Correct Sound");
  final JButton buttonPlayToySoF = new JButton("Play Toy SoF");
  final JButton loadBitString = new JButton("Load Bit String");
  final JButton initOFDM = new JButton("Init OFDM");
  final JButton buttonTransmit = new JButton("Transmit Bit String");
  final JLabel  receiverStateLabel = new JLabel("Receiver State: Idle");
  final JButton buttonReceive = new JButton("Receive");
  final JButton buttonStopReceive = new JButton("Stop Receiving");
  
  final AsioDriverListener host = this;

  final Lock BufferIntrLock = new ReentrantLock();
  private boolean PlayContentLock = false;

  class SoFCalcThread extends Thread {
    public void run() {
      while(true) {
        try {
          // acquire the lock
          // BufferIntrLock.lock();
          // update the correlations
          receiver.process();
          // release the lock
          // BufferIntrLock.unlock();
        } catch(Exception e) {
            e.printStackTrace();  // Log the exception
            break;  // Break the loop if an exception occurs
        }
      }
    }
  }

  public Host() {
    // the title
    super("Acoustic NFC");

    this.cfg = new Config(this);

    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            // Your custom logic here
            System.out.println("Window is closing");
            // call System.exit(0) or dispose() as required
            System.exit(0);
        }
    });
    
    activeChannels = new HashSet<AsioChannel>();

    // init recorders
    recorder = new Recorder();

    // init workstate
    setState(State.IDLE);
    setReceiverState(ReceiverState.IDLE);

    // button callbacks
    setButtonCallbacks();

    // init driver
    driverInit();

    // init framer
    framer = new Framer(cfg);
    // get the bit string
    bitString = new BitString("bit_string.txt");
  
    // layout panel
    layoutPanel();

    // init receiver
    receiver = new Receiver(cfg);

    // launch the SoF calculation thread
    SoFCalcThread soFCalcThread = new SoFCalcThread();
    soFCalcThread.start();
  }

  // layout panel
  public void layoutPanel() {
    this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

    // Line 1
    JPanel panel1 = new JPanel();
    BoxLayout boxLayout = new BoxLayout(panel1, BoxLayout.X_AXIS);
    panel1.setLayout(boxLayout);
    panel1.add(comboBox);
    stateLabel.setText("Idle                                                                          ");
    stateLabel.setPreferredSize(stateLabel.getPreferredSize());
    stateLabel.setMinimumSize(stateLabel.getPreferredSize());
    panel1.add(stateLabel);
    this.add(panel1);
    
    // Line 2
    JPanel panel2 = new JPanel();
    boxLayout = new BoxLayout(panel2, BoxLayout.X_AXIS);
    panel2.setLayout(boxLayout);
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
    panel2.add(buttonRecord);
    panel2.add(buttonRecordAndPlay);
    panel2.add(buttonStop);
    panel2.add(buttonReplay);
    panel2.add(buttonControlPanel);
    this.add(panel2);

    // Line 3
    JPanel panel3 = new JPanel();
    boxLayout = new BoxLayout(panel3, BoxLayout.X_AXIS);
    panel3.setLayout(boxLayout);
    panel3.add(buttonPj1Pt2);
    panel3.add(buttonPlayToySoF);
    panel3.add(loadBitString);
    panel3.add(initOFDM);
    this.add(panel3);

    // Line 4
    JPanel panel4 = new JPanel();
    boxLayout = new BoxLayout(panel4, BoxLayout.X_AXIS);
    panel4.setLayout(boxLayout);
    panel4.add(buttonTransmit);
    receiverStateLabel.setText("Receiver State: Idle      ");
    receiverStateLabel.setPreferredSize(receiverStateLabel.getPreferredSize());
    receiverStateLabel.setMinimumSize(receiverStateLabel.getPreferredSize());
    panel4.add(receiverStateLabel);
    panel4.add(buttonReceive);
    panel4.add(buttonStopReceive);
    this.add(panel4);

    // cfg panel
    this.add(cfg.panel);

    this.setSize(1000, 600);
    this.setResizable(false);
    this.setVisible(true);
  }

  // state switch wrapper
  private void setState(State state) {
    // the mapper from state to label
    switch (state) {
      case IDLE:
        stateLabel.setText("Idle");
        break;
        case RECORDING:
        stateLabel.setText("Recording");
        break;
        case PLAYING:
        stateLabel.setText("Playing");
        break;
        case RECORDING_PLAYING:
        stateLabel.setText("Recording and Playing");
        break;
        default:
        stateLabel.setText("Unknown");
        break;
    }
    this.state = state;
  }

  // receiveState switch wrapper
  private void setReceiverState(ReceiverState state) {
    this.receiverState = state;
    // the mapper from state to label
    switch (state) {
      case IDLE:
        receiverStateLabel.setText("Receiver State: Idle");
        break;
      case RECEIVING:
        receiverStateLabel.setText("Receiver State: Receiving");
        break;
      default:
        receiverStateLabel.setText("Unknown");
        break;
    }
  }

  // system state
  public Boolean isBusy() {
    // print system state
    System.out.println("System state: " + state + " " + receiverState);
    return state != State.IDLE || receiverState != ReceiverState.IDLE;
  }

  // button callback
  public void setButtonCallbacks() {
    buttonRecord.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // restart the driver to sync the new setting
        driverShutdown();
        driverInit();
        setState(State.RECORDING);
        // restart the recorder
        recorder = new Recorder();
      }
    });
    
    buttonStop.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        setState(State.IDLE);
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
        setState(State.PLAYING);
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
        setState(State.RECORDING_PLAYING);
        // restart the recorder
        recorder = new Recorder();
        // generate music
        music = new Music(cfg);
        // set player to play the music
        player = new Player(music.generateChordProgression());
      }
    });

    buttonPj1Pt2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // restart the driver to sync the new setting
        driverShutdown();
        driverInit();
        setState(State.PLAYING);
        // generate music
        music = new Music(cfg);
        // set player to play the music
        player = new Player(music.generateProj1Pt2Sound());
      }
    });

    loadBitString.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        BitString bitString = new BitString("bit_string.txt");
      }
    });

    initOFDM.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // init OFDM
        OFDM ofdm = new OFDM(cfg);
      }
    });

    buttonTransmit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // restart the driver to sync the new setting if idle
        if (state == State.IDLE && receiverState == ReceiverState.IDLE) {
          driverShutdown();
          driverInit();
        }
        // prepare play content
        float[] playContent = framer.pack(bitString.getBitString());
        // use lock to tell the intr, player is not ready
        PlayContentLock = true;
        // set player
        setState(State.PLAYING);
        player = new Player(playContent);
        // release
        PlayContentLock = false;
      }
    });

    buttonReceive.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        // restart the driver to sync the new setting
        driverShutdown();
        driverInit();
        setReceiverState(ReceiverState.RECEIVING);
        // init receiver
        receiver = new Receiver(cfg);
      }
    });

    buttonStopReceive.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        setReceiverState(ReceiverState.IDLE);
        receiverStateLabel.setText("Receiver State: Idle");
        // print the length of the recording
        System.out.println("Receiving length: " + receiver.getLength());
        // output the recordings as a csv file
        receiver.dumpResults();
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
    cfg.UpdSampleRate(sampleRate);
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

  private enum ChannelType { INPUT, OUTPUT };
  private AsioChannel findAsioChannel(ChannelType type, Set<AsioChannel> channels) {
    // loop through the channels
    for (AsioChannel channel : channels) {
      // check if the channel is an input channel and is active
      if (channel.isActive()) {
        // get the input channel
        if (type == ChannelType.INPUT && channel.isInput()) {
          return channel;
        }
        else if (type == ChannelType.OUTPUT && !channel.isInput()) {
          return channel;
        }
      }
    }

    return null;
  }

  // IO handler below, should be fast
  public void bufferSwitch(long systemTime, long samplePosition, Set<AsioChannel> channels) {
    // pick the first active input channel
    AsioChannel inputChannel = findAsioChannel(ChannelType.INPUT, channels);
    // pick the first active output channel
    AsioChannel outputChannel = findAsioChannel(ChannelType.OUTPUT, channels);

    // if we need to record
    if (state == State.RECORDING || state == State.RECORDING_PLAYING) {
      // if not found
      if (inputChannel == null) {
        // print error
        System.out.println("No active input channel found.");
        // stop recording
        setState(State.IDLE);
        return;
      }
      // buffer tmp
      float[] input = new float[bufferSize];
      // read from the input channel
      inputChannel.read(input);
      // record
      recorder.record(input);
    }

    // If we need to play (we shouldn't be modifying the content to play)
    if ((state == State.PLAYING || state == State.RECORDING_PLAYING) && !PlayContentLock) {
      // if not found
      if (outputChannel == null) {
        // print error
        System.out.println("No active output channel found.");
        // stop playing
        setState(State.IDLE);
        return;
      }

      output = new float[bufferSize];
      // play
      if (!player.playContent(bufferSize, output)) {
        // if this call handles the last buffer
        setState(State.IDLE);
      }
      // write to the output channel
      outputChannel.write(output);
    }
    else {
      // not playing, send 0
      for (int i = 0; i < bufferSize; i++) {
        output[i] = 0;
      }
      // if found
      if (outputChannel != null) {
        // write to the output channel
        outputChannel.write(output);
      }
    }

    // if we need to receive
    if (receiverState == ReceiverState.RECEIVING) {
      // if not found
      if (inputChannel == null) {
        // print error
        System.out.println("No active input channel found.");
        // stop recording
        setReceiverState(ReceiverState.IDLE);
        return;
      }
      // buffer tmp
      float[] input = new float[bufferSize];
      // read from the input channel
      inputChannel.read(input);
      // lock
      // BufferIntrLock.lock();
      // receive
      receiver.feedSamples(input);
      // unlock
      // BufferIntrLock.unlock();
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
  
  public static void main(String[] args) {
    @SuppressWarnings("unused")
    Host host = new Host();
  }

}
