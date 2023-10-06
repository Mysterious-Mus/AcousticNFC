package com.AcousticNFC.utils;

public class Music {
    private final double sampleRate;
    private final double duration = 1.5; // 4seconds for each chord
    private final float amplitude = 0.08f;
    private final float[] rootNotes = {261.63F, 220.00F, 174.61F, 196.00F};
    private float[] frequencies = new float[] {0, 523.25F, 587.33F, 659.25F, 698.46F, 783.99F, 880.00F, 987.77F};
    private int[][] notes = new int[][] {{1,3,5}, {7,2,5}, {6,1,3}, {5,1,3}, {4,6,1}, {3,5,1}, {2,4,6}, {4,5,6,1}};

    private enum Chord {
        MAJOR, MINOR, DIMINISHED, AUGMENTED
    }
    private Chord[] chords = {Chord.MAJOR, Chord.MINOR, Chord.MAJOR, Chord.MAJOR};

    public float[] generateChordFreqs(float rootNote, Chord chord) {
        float[] frequencies = new float[4];
        switch (chord) {
            case MAJOR:
                frequencies[0] = rootNote;
                frequencies[1] = rootNote * 5 / 4;
                frequencies[2] = rootNote * 3 / 2;
                frequencies[3] = rootNote * 2;
                break;
            case MINOR:
                frequencies[0] = rootNote;
                frequencies[1] = rootNote * 6 / 5;
                frequencies[2] = rootNote * 3 / 2;
                frequencies[3] = rootNote * 2;
                break;
            case DIMINISHED:
                frequencies[0] = rootNote;
                frequencies[1] = rootNote * 6 / 5;
                frequencies[2] = rootNote * 7 / 5;
                frequencies[3] = rootNote * 2;
                break;
            case AUGMENTED:
                frequencies[0] = rootNote;
                frequencies[1] = rootNote * 5 / 4;
                frequencies[2] = rootNote * 3 / 2;
                frequencies[3] = rootNote * 5 / 2;
                break;
        }
        return frequencies;
    }
    
    public float[] genChordComb(int[] notes) {
        float[] frequencies = new float[notes.length];
        for (int i = 0; i < notes.length; i++) {
            frequencies[i] = this.frequencies[notes[i]];
        }
        return frequencies;
    }

    public Music(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public float[] generateChord(float[] frequencies) {
        int n = (int) (sampleRate * duration);
        float[] samples = new float[n];
        for (int i = 0; i < frequencies.length; i++) {
            double frequency = frequencies[i];
            for (int j = 0; j < n; j++) {
                samples[j] += (float) (amplitude * Math.sin(2 * Math.PI * frequency * j / sampleRate));
            }
        }
        return samples;
    }

    public float[] generateChordProgression() {
        float[] samples = new float[0];
        for (int i = 0; i < notes.length; i++) {
            // float[] chordFreqs = generateChordFreqs(rootNotes[i], chords[i]);
            float[] chordFreqs = genChordComb(notes[i]);
            float[] chordSamples = generateChord(chordFreqs);
            float[] newSamples = new float[samples.length + chordSamples.length];
            System.arraycopy(samples, 0, newSamples, 0, samples.length);
            System.arraycopy(chordSamples, 0, newSamples, samples.length, chordSamples.length);
            samples = newSamples;
        }
        return samples;
    }
}

