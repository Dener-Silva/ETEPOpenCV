package com.example.dener.opencvdemo;

/**
 * Created by dener on 18/05/2016.
 * Criei esta classe apenas para medir o tempo que algumas funções levam.
 */
public class Stopwatch {
    private long startTime = 0;

    public void start() {
        startTime = System.nanoTime();
    }

    public long getElapsedTime() {
        if (startTime == 0) return 0;
        long stopTime = System.nanoTime();
        return (stopTime - startTime) / 1000000;
    }
}
