package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.ui.treeStructure.Tree;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
                System.out.println(node.getUserObject());
/*private PsiClass[] getClasses() {
                    Project project = myElement.getProject();

                    Module module = getModule(project);

                    GlobalSearchScope globalsearchscope;
                    if (module != null) {
                        globalsearchscope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
                    } else {
                        globalsearchscope = GlobalSearchScope.projectScope(project);
                    }

                    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                    if (potentialClassName.indexOf('.') != -1) {

                        return javaPsiFacade.findClasses(potentialClassName, globalsearchscope);
                    } else {
                        PsiShortNamesCache cache = javaPsiFacade.getShortNamesCache();

                        return cache.getClassesByName(potentialClassName, globalsearchscope);
                    }
                }
*/
            }
        }
    }
}
