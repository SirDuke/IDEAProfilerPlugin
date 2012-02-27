/*
 * Copyright 2012, Ivan Serduk. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Ivan Serduk OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.ui.treeStructure.Tree;
import org.ssprofiler.idea.profileplugin.projectcontext.ProjectContext;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public class ThreadDumpsTree extends Tree implements ThreadFilterListener {
    private Collection<ThreadDump> threadDumps;
    private long timeStamp;
    private ProjectContext projectContext;

    public ThreadDumpsTree(Collection<ThreadDump> threadDumps, ProjectContext projectContext) {
        super(new DefaultTreeModel(new DefaultMutableTreeNode("Select time at the chart above to view stacktraces")));

        this.projectContext = projectContext;

        this.threadDumps = threadDumps;
        initTree(threadDumps);
        addMouseListener(new TreeMouseListener());
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
                for (String aStack : stack) {
                    DefaultMutableTreeNode stackTraceNode = new DefaultMutableTreeNode(aStack);
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
        for (ThreadDump dump : threadDumps) {
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
    
    private class TreeMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if ((SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1)) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof String) {
                    projectContext.openSourceFile((String) userObject);
                }
            }
        }
    }
}
