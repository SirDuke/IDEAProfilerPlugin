package org.ssprofiler.model;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 25.05.11
 */
public class TimeInterval implements Serializable {
    private long startTime;
    private long endTime;

    public TimeInterval(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void extend(long newEndTime) {
        endTime = newEndTime;
    }

    public boolean contains(long time) {
        return (startTime < time) && (time <=endTime);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
