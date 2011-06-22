package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.ui.treeStructure.Tree;
import org.ssprofiler.model.ThreadDump;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public class ThreadDumpsTree extends Tree implements ThreadFilterListener {
    private Collection<ThreadDump> threadDumps;
    private long timeStamp;

    public ThreadDumpsTree(Collection<ThreadDump> threadDumps) {
        super(new DefaultTreeModel(new DefaultMutableTreeNode("Select time at the chart above to view stacktraces")));
        this.threadDumps = threadDumps;
        initTree(threadDumps);
    }

    // time in nanoseconds
    void setTimeStamp(long time) {
        timeStamp = time;
        DefaultTreeModel treeModel = (DefaultTreeModel)this.getModel();
        DefaultMutableTreeNode treeRoot = (DefaultMutableTreeNode) treeModel.getRoot();
        int n = threadDumps.size();
        for (int i = 0; i < n; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeRoot.getChildAt(i);
            ThreadDumpNode threadDumpNode = (ThreadDumpNode) child.getUserObject();
            threadDumpNode.updateStatus(time);
            ThreadDump threadDump = threadDumpNode.getThreadDump();
            child.removeAllChildren();
            if ((threadDump.getFirstDumpSystemTime() < time) && (threadDump.getLastDumpSystemTime() >= time)) {
                String[] stack = threadDump.getStackTrace(time);
                Thread.State state = threadDump.getThreadState(time);
                for (int j = 0; j < stack.length; j++) {
                    DefaultMutableTreeNode stackTraceNode = new DefaultMutableTreeNode(stack[j]);
                    child.add(stackTraceNode);
                }
            }
            treeModel.nodeChanged(child);
            treeModel.nodeStructureChanged(child);
        }
    }

    public void selectionChanged(Collection<ThreadDump> selectedThreads) {
        threadDumps = selectedThreads;
        initTree(threadDumps);
        setTimeStamp(timeStamp);
    }

    private void initTree(Collection<ThreadDump> threadDumps) {
        DefaultTreeModel treeModel = (DefaultTreeModel) this.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Stacktraces");
        root.removeAllChildren();
        Iterator<ThreadDump> iter = threadDumps.iterator();
        while (iter.hasNext()) {
            ThreadDump dump = iter.next();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new ThreadDumpNode(dump));
            root.add(newNode);
        }
        treeModel.setRoot(root);
    }

        static class ThreadDumpNode {
        private ThreadDump dump;
        private String name;

        ThreadDumpNode(ThreadDump dump) {
            this.dump = dump;
            this.name = getDefaultName();
        }

        void updateStatus(long time) {
            Thread.State threadState = dump.getThreadState(time);
            if (threadState != null) {
                name = (dump.getName() + " " + threadState).intern();
            } else {
                name = getDefaultName();
            }
        }

        private String getDefaultName() {
            return (dump.getName() + " NOT_RUNNING").intern();
        }

        ThreadDump getThreadDump() {
            return dump;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
