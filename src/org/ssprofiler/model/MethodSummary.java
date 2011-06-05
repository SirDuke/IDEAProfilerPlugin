package org.ssprofiler.model;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 29.05.11
 */
public class MethodSummary {
    private String methodName;
    private long cpuTimeTotal;
    private long cpuTimeOwn;
    private long systemTimeTotal;
    private long systemTimeOwn;
    private int count;

    private StackTraceTree root;

    public MethodSummary(String methodName) {
        this.methodName = methodName;
    }

    void process(StackTraceTree stackTraceTree) {
        cpuTimeOwn += stackTraceTree.getCpuTimeOwn();
        cpuTimeTotal += stackTraceTree.getCpuTimeTotal();
        systemTimeOwn += stackTraceTree.getSystemTimeOwn();
        systemTimeTotal += stackTraceTree.getSystemTimeTotal();
        count += stackTraceTree.getCount();
    }

    public String getMethodName() {
        return methodName;
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

    public StackTraceTree getCallers(Collection<ThreadDump> stackTraceTrees) {
        if (root == null) {
            root = new StackTraceTree(methodName, 0);
            CallersGeneratingVisitor visitor = new CallersGeneratingVisitor(root);
            for (Iterator<ThreadDump> iterator = stackTraceTrees.iterator(); iterator.hasNext();) {
                ThreadDump dumpTree = iterator.next();
                dumpTree.scanStackTrace(visitor);
            }
        }
        return root;
    }

    class CallersGeneratingVisitor implements StackTraceTreeVisitor {
        private StackTraceTree root;

        CallersGeneratingVisitor(StackTraceTree root) {
            this.root = root;
        }

        public boolean visit(StackTraceTree stackTraceTree) {
            if (stackTraceTree.getMethodName().equals(root.getMethodName())) {
                root.merge(stackTraceTree);
                return false;
            }
            return true;
        }
    }
}
