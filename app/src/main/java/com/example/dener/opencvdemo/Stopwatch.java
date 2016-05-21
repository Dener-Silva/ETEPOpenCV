package com.example.dener.opencvdemo;

/**
 * Created by dener on 18/05/2016.
 * Criei esta classe apenas para medir o tempo que algumas funções levam.
 */
public class Stopwatch {
    private long startTime = 0;
    private long lastSplit;
    private long split;

    public void start() {
        startTime = System.nanoTime();
        split = startTime;
        lastSplit = split;
    }

    public long split() {
        if (startTime == 0) return 0;
        lastSplit = split;
        split = System.nanoTime();
        return (split - lastSplit) / 1000000;
    }

    public long getElapsedTime() {
        if (startTime == 0) return 0;
        long stopTime = System.nanoTime();
        return (stopTime - startTime) / 1000000;
    }
}
