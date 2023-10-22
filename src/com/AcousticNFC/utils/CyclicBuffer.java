package com.AcousticNFC.utils;

import java.util.ArrayList;

/* A ring buffer that contains FIRSTINDEXWANTED
 * everything before FIRSTINDEXWANTED is discarded
 * also tells the buffer feeder which index to start feeding,
 * since the FIW can be set > last index in the buffer
 */
public class CyclicBuffer<T> {
    
    private ArrayList<T> buffer;
    public int FIW; // inclusive
    private int lastIdx; // exclusive

    public CyclicBuffer(int size) {
        buffer = new ArrayList<T>();
        // push initial contents
        for (int i = 0; i < size; i++) {
            buffer.add(null);
        }
        FIW = 0;
        lastIdx = 0;
    }

    private int getBufferIdx(int idx) {
        return idx % buffer.size();
    }

    public boolean full() {
        return lastIdx - FIW == buffer.size();
    }

    public void push(T t) {
        // assert buffer is not full
        assert !full();

        buffer.set(getBufferIdx(lastIdx), t);
        lastIdx++;
    }

    public T get(int i) {
        return buffer.get(getBufferIdx(i));
    }

    // index of the elem behind the last item(doesn't exist yet but next to feed)
    public int tailIdx() {
        return lastIdx; // this should be the index of the next element to be fed
    }

    public void setFIW(int new_FIW) {
        // can't decrease
        FIW = Math.max(FIW, new_FIW);
        
        // FIW can now be larger than lastIdx, because we can sometimes skip some indices
        if (FIW > lastIdx) {
            lastIdx = FIW;
        }
    }
}
