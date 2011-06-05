package org.ssprofiler.model;

import java.io.Serializable;
import java.lang.management.ThreadInfo;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 25.05.11
 */
public class ThreadDump implements Serializable {
    private static final long serialVersionUID = 8643854725192683745L;
    private static Thread.State[] THREAD_STATE = Thread.State.values();

    private long id;
    private String name;
    private long previousSystemTime, initialSystemTime;
    private long previousCpuTime;
    private StackTraceTree stackTraceTree;

    private List<TimeInterval>[] threadStateIntervals;
    private Thread.State currentState = Thread.State.NEW;
    private TimeInterval currentStateTimeInterval;

    public ThreadDump(ThreadInfo threadInfo, long systemTime, long cpuTime) {
        id = threadInfo.getThreadId();
        name = threadInfo.getThreadName();
        previousSystemTime = systemTime;
        initialSystemTime = systemTime;
        previousCpuTime = cpuTime;

        stackTraceTree = new StackTraceTree(name, -1);
        stackTraceTree.startNewTimeInterval(systemTime);

        int stateCount = Thread.State.values().length;
        threadStateIntervals = new List[stateCount];
        for (int i = 0; i < stateCount; i++) {
            threadStateIntervals[i] = new LinkedList<TimeInterval>();
        }
        currentStateTimeInterval = new TimeInterval(systemTime, systemTime);
        threadStateIntervals[0].add(currentStateTimeInterval);
    }

    public void addThreadDump(ThreadInfo info, long systemTime, long cpuTime) {
        processState(info.getThreadState(), systemTime);
        StackTraceElement[] stackTrace = info.getStackTrace();
        if ((stackTrace != null) && (stackTrace.length > 0))
            stackTraceTree.processThreadInfo(stackTrace, systemTime, systemTime - previousSystemTime, cpuTime - previousCpuTime);

        previousCpuTime = cpuTime;
        previousSystemTime = systemTime;
    }

    private void processState(Thread.State state, long systemTime) {
        if (currentState.equals(state)) {
            currentStateTimeInterval.extend(systemTime);
        } else {
            List<TimeInterval> listNewStatus = threadStateIntervals[getIndexForState(state)];
            currentStateTimeInterval = new TimeInterval(previousSystemTime, systemTime);
            listNewStatus.add(currentStateTimeInterval);
            currentState = state;
        }
    }

    private int getIndexForState(Thread.State state) {
        for (int i = 0; i < THREAD_STATE.length; i++) {
            if (THREAD_STATE[i].equals(state)) {
                return i;
            }
        }
        return -1;
    }

    public long getThreadId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String[] getStackTrace(long time) {
        if (stackTraceTree == null) {
            return new String[0];
        }
        LinkedList<String> stackTraceList = new LinkedList<String>();
        stackTraceTree.getStackTrace(stackTraceList, time);
        // first element is from "root" node and we're not interested in it
        stackTraceList.remove(0);
        return stackTraceList.toArray(new String[stackTraceList.size()]);
    }

    public Thread.State getThreadState(long time) {
        if ((time < initialSystemTime) || (time > previousSystemTime)) {
            return null;
        }
        for (int i = 0; i < threadStateIntervals.length; i++) {
            Iterator<TimeInterval> iter = threadStateIntervals[i].iterator();
            if (iter.hasNext()) {
                TimeInterval ti = iter.next();
                while ((iter.hasNext()) && (ti.getEndTime() <= time)) {
                    ti = iter.next();
                }
                if (ti.contains(time)) {
                    return THREAD_STATE[i];
                }
            }
        }
        return null;
    }

    public long getFirstDumpSystemTime() {
        return initialSystemTime;
    }

    public long getLastDumpSystemTime() {
        return previousSystemTime;
    }

    public List<TimeInterval> getTimeIntevalsForState(Thread.State state) {
        return threadStateIntervals[getIndexForState(state)];
    }

    void scanStackTrace(StackTraceTreeVisitor visitor) {
        stackTraceTree.visit(visitor);
    }

    public StackTraceTree getStackTraceTree() {
        return stackTraceTree;
    }
}
