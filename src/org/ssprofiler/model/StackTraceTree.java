package org.ssprofiler.model;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 25.05.11
 */
public class StackTraceTree implements Serializable{
    private static final long serialVersionUID = 2308870863448966218L;

    private Map<String, StackTraceTree> subTrees;
    private List<TimeInterval> intervals;
    private TimeInterval currentTimeInteval;
    private StackTraceTree currentActiveSubTree;
    private String methodName;
    private int count;
    private long cpuTimeTotal;
    private long cpuTimeOwn;
    private long systemTimeTotal;
    private long systemTimeOwn;

    private transient int level;

    public StackTraceTree(String methodName, int level) {
        this.methodName = methodName;
        this.level = level;
        subTrees = new HashMap<String, StackTraceTree>();
        intervals = new ArrayList<TimeInterval>();
    }

    public void processThreadInfo(StackTraceElement[] stackTrace, long systemTime, long systemTimeDiff, long cpuTime) {
        // methods in stackTrace are mentioned in back order - first element corresponds to most recently called method, last element is the root method of the thread
        int level = stackTrace.length - this.level - 1;
        cpuTimeTotal += cpuTime;
        systemTimeTotal += systemTimeDiff;
        count++;
        if (level == 0) {
            cpuTimeOwn += cpuTime;
            systemTimeOwn += systemTimeDiff;
            if (currentActiveSubTree != null) {
                currentActiveSubTree.closeCurrentTimeInterval();
                currentActiveSubTree = null;
            }

        }  else {
            String nextMethodName = (stackTrace[level - 1].getClassName() + "." + stackTrace[level - 1].getMethodName()).intern();
            if ((currentActiveSubTree != null) && (nextMethodName.equals(currentActiveSubTree.getMethodName()))) {
                currentActiveSubTree.processThreadInfo(stackTrace, systemTime, systemTimeDiff, cpuTime);
            } else {
                if (currentActiveSubTree != null) currentActiveSubTree.closeCurrentTimeInterval();

                currentActiveSubTree = subTrees.get(nextMethodName);
                if (currentActiveSubTree == null) {
                    currentActiveSubTree = new StackTraceTree(nextMethodName, this.level + 1);
                    subTrees.put(nextMethodName, currentActiveSubTree);
                }
                currentActiveSubTree.startNewTimeInterval(currentTimeInteval.getEndTime());
                currentActiveSubTree.processThreadInfo(stackTrace, systemTime, systemTimeDiff, cpuTime);
            }

        }
        currentTimeInteval.extend(systemTime);
    }

    void startNewTimeInterval(long startTime) {
        currentTimeInteval = new TimeInterval(startTime, startTime);
        intervals.add(currentTimeInteval);
    }

    private void closeCurrentTimeInterval() {
        currentTimeInteval = null;
    }

    public String getMethodName() {
        return methodName;
    }

    public void getStackTrace(LinkedList<String> stackTraceList, long time) {
        stackTraceList.add(methodName);
        for (Iterator<StackTraceTree> iterator = subTrees.values().iterator(); iterator.hasNext();) {
            StackTraceTree next = iterator.next();
            if (next.containsTime(time)) {
                next.getStackTrace(stackTraceList, time);
                return;
            }
        }
    }

    public Collection<StackTraceTree> getChildren() {
        return subTrees.values();
    }

    private boolean containsTime(long time) {
        for (Iterator<TimeInterval> iterator = intervals.iterator(); iterator.hasNext();) {
            TimeInterval next = iterator.next();
            if (next.contains(time)) return true;
            if (next.getStartTime() > time) return false;
        }
        return false;
    }

    public long getCpuTimeTotal() {
        return cpuTimeTotal;
    }

    public long getCpuTimeOwn() {
        return cpuTimeOwn;
    }

    public long getSystemTimeTotal() {
        return systemTimeTotal;
    }

    public long getSystemTimeOwn() {
        return systemTimeOwn;
    }

    public int getCount() {
        return count;
    }

    void visit(StackTraceTreeVisitor visitor) {
        for (Iterator<StackTraceTree> iterator = subTrees.values().iterator(); iterator.hasNext();) {
            StackTraceTree subTree = iterator.next();
            boolean visitChildren = visitor.visit(subTree);
            if (visitChildren) {
                subTree.visit(visitor);
            }
        }
    }

    void merge(StackTraceTree anotherTree) {
        count += anotherTree.count;
        cpuTimeOwn += anotherTree.cpuTimeOwn;
        cpuTimeTotal += anotherTree.cpuTimeTotal;
        systemTimeOwn += anotherTree.systemTimeOwn;
        systemTimeTotal += anotherTree.systemTimeTotal;

        for (Iterator<StackTraceTree> iterator = anotherTree.subTrees.values().iterator(); iterator.hasNext();) {
            StackTraceTree anotherChild = iterator.next();
            StackTraceTree child = subTrees.get(anotherChild.getMethodName());
            if (child == null) {
                child = new StackTraceTree(anotherChild.getMethodName(), level + 1);
                subTrees.put(child.getMethodName(), child);
            }
            child.merge(anotherChild);
        }
    }

    @Override
    public String toString() {
        return methodName;
    }
}

