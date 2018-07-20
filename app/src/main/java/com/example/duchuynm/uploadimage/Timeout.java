package com.example.duchuynm.uploadimage;

public class Timeout extends Thread {
    private int time;
    private boolean isTimeout = false;
    public Timeout(int timeout) {
        this.time = timeout;
    }

    public boolean isTimeout() {
        return isTimeout;
    }

    public boolean startTimeout() {
        for(int i=time;i>0;i--) {
            time--;
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return isTimeout = true;
    }
}
