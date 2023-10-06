package com.acousticnfc.utils;

import java.io.FileWriter;
import java.io.IOException;

public class Recorder {

    private float[] recordings; // the recordings
    private final float maxAmplitude = 0.2f;

    public Recorder() {
        super();

        recordings = new float[0];
    }

    public void record(float[] input) {
        // add the new samples to the recordings
        float[] newRecordings = new float[recordings.length + input.length];
        System.arraycopy(recordings, 0, newRecordings, 0, recordings.length);
        System.arraycopy(input, 0, newRecordings, recordings.length, input.length);
        recordings = newRecordings;
    }

    public void gain_recordings() {
        // find the max amplitude in the recording
        float max = 0;
        for (int i = 0; i < recordings.length; i++) {
            if (Math.abs(recordings[i]) > max) {
                max = Math.abs(recordings[i]);
            }
        }

        // compute and print the gain ratio
        float gain = maxAmplitude / max;
        System.out.println("Gain ratio: " + maxAmplitude / max);

        // gain the recordings
        for (int i = 0; i < recordings.length; i++) {
            recordings[i] = recordings[i] * gain;
        }
    }

    public float[] getRecordings() {
        gain_recordings();
        return recordings;
    }

    public void outputRecordings(String fileName) {
        gain_recordings();
        try {
            FileWriter writer = new FileWriter(fileName + ".csv");
            for (int i = 0; i < recordings.length; i++) {
                writer.append(Float.toString(recordings[i]));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
