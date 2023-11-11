package com.AcousticNFC.utils.sync;

import java.util.concurrent.Semaphore;

public class TaskNotify {
    
    private Semaphore taskSemaphore;

    public TaskNotify() {
        taskSemaphore = new Semaphore(0);
    }

    public void waitTask() {
        try {
            taskSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void notifyTask() {
        // release if no permits
        if (taskSemaphore.availablePermits() == 0) {
            taskSemaphore.release();
        }
    }
}
