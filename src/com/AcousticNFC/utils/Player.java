package com.AcousticNFC.utils;

public class Player {
    
    private float[] playerBuffer;
    private int playerBufferIndex;

    public Player(float[] playerBuffer) {
        super();

        this.playerBuffer = playerBuffer;
        // apply gain
        for (int i = 0; i < playerBuffer.length; i++) {
            playerBuffer[i] *= gain;
        }
        this.playerBufferIndex = 0;
    }

    // return false if this call handles the last buffer
    public boolean playContent(int bufferSize, float[] outBuffer) {
        // get the content to play
        // check if it's the last buffer to play
        if (playerBufferIndex + bufferSize > playerBuffer.length) {
            // copy the remaining content
            System.arraycopy(playerBuffer, playerBufferIndex, outBuffer, 0, playerBuffer.length - playerBufferIndex);
            // fill the rest of the buffer with 0
            for (int i = playerBuffer.length - playerBufferIndex; i < bufferSize; i++) {
                outBuffer[i] = 0;
            }
            // update the index
            playerBufferIndex = playerBuffer.length;
            // return false to indicate that this call handles the last buffer
            return false;
        }
        else {
            // copy the content
            System.arraycopy(playerBuffer, playerBufferIndex, outBuffer, 0, bufferSize);
            // update the index
            playerBufferIndex += bufferSize;
            // return true to indicate that this call doesn't handle the last buffer
            return true;
        }
    }

    public int getBufferIndex() {
        return playerBufferIndex;
    }

    public int getBufferLength() {
        return playerBuffer.length;
    }
}
