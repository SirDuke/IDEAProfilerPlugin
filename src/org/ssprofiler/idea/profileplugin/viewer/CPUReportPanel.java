/*
 * Copyright 2011, Ivan Serduk. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Ivan Serduk OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import org.ssprofiler.model.MemoryInfo;
import org.ssprofiler.model.MethodSummary;
import org.ssprofiler.model.StackTraceTree;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 14.05.11
 */
public class CPUReportPanel extends DialogWrapper {
    private static final long NANO = 1000000000;

    private static String SAMPLING_TREE_COLUMN_HEADER = "Sampling Tree";
    private static String SAMPLES_COLUMN_HEADER = "Samples count";
    private static String CPU_COLUMN_HEADER = "CPU time usage";

    private JTabbedPane tabbedPane1;
    private JPanel panelMain;
    private JPanel panelCPUChart;
    private JPanel panelSamples;
    private JPanel panelMemory;
    private JPanel panelSamplingSummary;

    private Map<Long, ThreadDump> threadDumps;
    private List<MemoryInfo> memoryDataList;
    private long minTime, maxTime;
    private DecimalFormat doubleFormat = new DecimalFormat("##.##");
    private Dimension preferredSize;

    protected CPUReportPanel(boolean canBeParent) {
        super(canBeParent);
        init();
    }


    public void init(Dimension preferredSize,
                     Map<Long, ThreadDump> threadDumps,
                     List<MemoryInfo> memoryDataList,
                     List<MethodSummary> methodSummaries,
                     long minTime,
                     long maxTime) {
        this.preferredSize = preferredSize;
        this.threadDumps = threadDumps;
        this.memoryDataList = memoryDataList;
        this.minTime = minTime;
        this.maxTime = maxTime;
        panelMain.setPreferredSize(preferredSize);
        initCPUChartPanel();
        initMemoryChartPanel();
        initSampingTreePanel(threadDumps.values());
        initSamplingSummaryTree(threadDumps.values(), methodSummaries);
    }

    public JPanel getMainPanel() {
        return panelMain;
    }

    private void initMemoryChartPanel() {
        if (memoryDataList == null) {
            panelMemory.add(new JLabel("No Data available"));
            return;
        }
        panelMemory.setLayout(new GridLayout(2, 1));

        MemoryChartComponent mmc = new MemoryChartComponent(memoryDataList);
        panelMemory.add(new JBScrollPane(mmc));

        JTree jTree = new com.intellij.ui.treeStructure.Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Select time at the chart above to view stacktraces")));
        initTree(jTree);
        panelMemory.add(new JBScrollPane(jTree));

        mmc.addMouseListener(new ChartMouseListenter(jTree, threadDumps.values(), minTime, maxTime));
    }

    private void initCPUChartPanel() {
        if (threadDumps == null) {
            panelCPUChart.add(new JLabel("No Data available"));
            return;
        }
        panelCPUChart.setLayout(new GridLayout(2,1));

        ThreadChartComponent tcc = new ThreadChartComponent(threadDumps.values(), minTime, maxTime);
        panelCPUChart.add(new JBScrollPane(tcc));

        JTree jTree = new com.intellij.ui.treeStructure.Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Select time at the chart above to view stacktraces")));
        initTree(jTree);
        panelCPUChart.add(new JBScrollPane(jTree));

        tcc.addMouseListener(new ChartMouseListenter(jTree, threadDumps.values(), minTime, maxTime));
    }

    private void initTree(JTree jTree) {
        DefaultTreeModel treeModel = (DefaultTreeModel) jTree.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        Iterator<ThreadDump> iter = threadDumps.values().iterator();
        while (iter.hasNext()) {
            ThreadDump dump = iter.next();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new ThreadDumpNode(dump));
            root.add(newNode);
        }
        treeModel.setRoot(root);
    }

    private void initSampingTreePanel(Collection<ThreadDump> threadDumps) {
        if (threadDumps == null) {
            panelSamples.add(new JLabel("No data available"));
            return;
        }
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new StackTraceTree("", 0));
        for (Iterator<ThreadDump> iterator = threadDumps.iterator(); iterator.hasNext();) {
            ThreadDump threadDump = iterator.next();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(threadDump.getStackTraceTree());
            root.add(newNode);
            buildSamplingTree(newNode, threadDump.getStackTraceTree().getChildren());

        }
        final ColumnInfo[] columns = initSamplingTreeTableColumns();
        TreeTableModel model = new ListTreeTableModelOnColumns(root, columns);

        final TreeTable treeTable = new TreeTable(model);

       // treeTable.setPreferredSize(preferredSize);

        panelSamples.add(new JBScrollPane(treeTable));
    }

    private void initSamplingSummaryTree(Collection<ThreadDump> threadDumps, List<MethodSummary> methodSummaries) {
        if (methodSummaries == null) {
            panelSamplingSummary.add(new JLabel("No data available"));
            return;
        }

        //panelSamplingSummary.add(new JBScrollPane(new MethodUsageTreeTable(samplingSummaryTree)));
        ((SamplingSummaryPanel)panelSamplingSummary).init(methodSummaries, threadDumps, preferredSize);

    }

    private ColumnInfo[] initSamplingTreeTableColumns() {
        ColumnInfo treeColumn = new ColumnInfo(SAMPLING_TREE_COLUMN_HEADER) {
            @Override
            public Object valueOf(Object o) {
                return ((DefaultMutableTreeNode) o).getUserObject();
            }

            @Override
            public Class getColumnClass() {
                return TreeTableModel.class;
            }
        };
        ColumnInfo countColumn = new ColumnInfo(SAMPLES_COLUMN_HEADER) {

            @Override
            public Object valueOf(Object o) {
                StackTraceTree stei = (StackTraceTree) ((DefaultMutableTreeNode) o).getUserObject();
                return Long.toString(stei.getCount());
            }
        };
        ColumnInfo cpuColumn = new ColumnInfo(CPU_COLUMN_HEADER) {

            @Override
            public Object valueOf(Object o) {
                StackTraceTree stei = (StackTraceTree) ((DefaultMutableTreeNode) o).getUserObject();
                return Double.toString(stei.getCpuTimeTotal() / NANO);
            }
        };

        return new ColumnInfo[]{treeColumn, countColumn, cpuColumn};
    }

    private void buildSamplingTree(DefaultMutableTreeNode treeNode, Collection<StackTraceTree> children) {
        for (Iterator<StackTraceTree> iterator = children.iterator(); iterator.hasNext();) {
            StackTraceTree subtree = iterator.next();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(subtree);
            treeNode.add(newNode);
            buildSamplingTree(newNode, subtree.getChildren());

        }
    }

    private void createUIComponents() {
        panelSamplingSummary = new SamplingSummaryPanel();
    }


  //  @Override
    protected JComponent createCenterPanel() {
        return panelMain;
    }

    static class ChartMouseListenter extends MouseAdapter {
        private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        //private JTextArea jTextArea;
        private JTree jTree;
        private Collection<ThreadDump> threadDumps;
        private long minTime, maxTime;

        ChartMouseListenter(JTree jTree, Collection<ThreadDump> threadDumps, long minTime, long maxTime) {
            this.jTree = jTree;
            this.threadDumps = threadDumps;
            this.minTime = minTime;
            this.maxTime = maxTime;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Point point = e.getPoint();
            TimeIntervalAxis tia = (TimeIntervalAxis) e.getComponent();
            int x = point.x;
            if ((tia.getStartX() <= x) && (x <= tia.getEndX())) {
                tia.setSelectedX(x);
                e.getComponent().repaint();
                double k = ((double)x - tia.getStartX()) / ((double)tia.getEndX() - tia.getStartX());
                updateTree(minTime + (long) (k * (maxTime - minTime)));

            }

        }

        // time in nanoseconds
        private void updateTree(long time) {
            DefaultTreeModel treeModel = (DefaultTreeModel)jTree.getModel();
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
         //   treeModel.nodeStructureChanged(treeRoot);
        }
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
