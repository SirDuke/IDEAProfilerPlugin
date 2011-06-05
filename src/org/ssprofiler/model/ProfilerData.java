package org.ssprofiler.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 29.05.11
 */
public class ProfilerData {

    private Map<Long,ThreadDump> threadDumps;
    private List<MethodSummary> methodSummaries;
    private List<MemoryInfo> memoryInfoList;
    private long minTime = Long.MAX_VALUE;
    private long maxTime = 0;

    public void readData(String filename) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
        memoryInfoList = new ArrayList<MemoryInfo>();
        Object o = ois.readObject();
        while (o instanceof  MemoryInfo) {
            memoryInfoList.add((MemoryInfo) o);
            o = ois.readObject();
        }
        memoryInfoList = Collections.unmodifiableList(memoryInfoList);
        threadDumps = (Map<Long, ThreadDump>) o;
        for (Iterator<ThreadDump> iterator = threadDumps.values().iterator(); iterator.hasNext();) {
            ThreadDump threadDump = iterator.next();
            if (threadDump.getFirstDumpSystemTime() < minTime) minTime = threadDump.getFirstDumpSystemTime();
            if (threadDump.getLastDumpSystemTime() > maxTime) maxTime = threadDump.getLastDumpSystemTime();
        }
    }

    public Map<Long, ThreadDump> getThreadDumps() {
        return threadDumps;
    }

    public List<MemoryInfo> getMemoryInfo() {
        return memoryInfoList;
    }

    public long getMinTime() {
        return minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public List<MethodSummary> getMethodSummaries() {
        if (methodSummaries == null) {
            methodSummaries = buildMethodSummaries();
        }
        return methodSummaries;
    }

    private List<MethodSummary> buildMethodSummaries() {
        Map<String, MethodSummary> summaryMap = new HashMap<String, MethodSummary>();
        StackTraceTreeVisitor visitor = new MethodSummaryVisitor(summaryMap);
        for (Iterator<ThreadDump> iterator = threadDumps.values().iterator(); iterator.hasNext();) {
            ThreadDump threadDump = iterator.next();
            threadDump.scanStackTrace(visitor);
        }
        ArrayList<MethodSummary> list = new ArrayList<MethodSummary>(summaryMap.size());
        list.addAll(summaryMap.values());
        return Collections.unmodifiableList(list);
    }

    class MethodSummaryVisitor implements StackTraceTreeVisitor {
        private Map<String, MethodSummary> summaryMap;

        MethodSummaryVisitor(Map<String, MethodSummary> summaryMap) {
            this.summaryMap = summaryMap;
        }

        public boolean visit(StackTraceTree stackTraceTree) {
            String methodName = stackTraceTree.getMethodName();
            MethodSummary methodSummary = summaryMap.get(methodName);
            if (methodSummary == null) {
                methodSummary = new MethodSummary(methodName);
                summaryMap.put(methodName, methodSummary);
            }
            methodSummary.process(stackTraceTree);
            return true;
        }
    }

}
