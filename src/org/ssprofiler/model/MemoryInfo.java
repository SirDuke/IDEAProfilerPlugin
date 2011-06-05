package org.ssprofiler.model;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 31.05.11
 */
public class MemoryInfo implements Serializable{
    private long systemTime; // in nanoseconds
    private long usedHeap;
    private long usedNonHeap;

    public MemoryInfo(long systemTime, long usedHeap, long usedNonHeap) {
        this.systemTime = systemTime;
        this.usedHeap = usedHeap;
        this.usedNonHeap = usedNonHeap;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public long getUsedHeap() {
        return usedHeap;
    }

    public long getUsedNonHeap() {
        return usedNonHeap;
    }
}
