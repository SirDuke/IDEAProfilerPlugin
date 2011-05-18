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

package org.ssprofiler.idea.profileplugin.viewer.oktech;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import hu.oktech.profiler.analyzer.tree.Tree;
import hu.oktech.profiler.core.data.MemoryData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 14.05.11
 */
public class OktechCPUReportPanel extends DialogWrapper {
    private static String SAMPLING_TREE_COLUMN_HEADER = "Sampling Tree";
    private static String SAMPLES_COLUMNT_HEADER = "Samples count";
    private static String CPU_COLUMN_HEADER = "CPU time usage";

    private JTabbedPane tabbedPane1;
    private JPanel panelMain;
    private JPanel panelCPUChart;
    private JPanel panelSamples;
    private JPanel panelMemory;

    private Map<Long, ThreadDataSummary> threadDataMap;
    private List<MemoryData> memoryDataList;
    private long minTime, maxTime;

    protected OktechCPUReportPanel(boolean canBeParent) {
        super(canBeParent);
        init();
    }


    public void init(Map<Long, ThreadDataSummary> threadDataMap, List<MemoryData> memoryDataList, Tree samplingTree, long minTime, long maxTime) {
        this.threadDataMap = threadDataMap;
        this.memoryDataList = memoryDataList;
        this.minTime = minTime;
        this.maxTime = maxTime;
        panelMain.setPreferredSize(new Dimension(1000, 600));
        initCPUChartPanel();
        initMemoryChartPanel();
        initSampingTreePanel(samplingTree);
    }

    public JPanel getMainPanel() {
        return panelMain;
    }

    private void initMemoryChartPanel() {
        panelMemory.setLayout(new GridLayout(2, 1));

        MemoryChartComponent mmc = new MemoryChartComponent(memoryDataList);
        panelMemory.add(new JBScrollPane(mmc));

        JTree jTree = new com.intellij.ui.treeStructure.Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Select time at the chart above to view stacktraces")));
        panelMemory.add(new JBScrollPane(jTree));

        mmc.addMouseListener(new ChartMouseListenter(jTree, threadDataMap, memoryDataList, minTime, maxTime));
    }

    private void initCPUChartPanel() {
        panelCPUChart.setLayout(new GridLayout(2,1));

        ThreadChartComponent tcc = new ThreadChartComponent(threadDataMap, minTime, maxTime);
        panelCPUChart.add(new JBScrollPane(tcc));

        JTree jTree = new com.intellij.ui.treeStructure.Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Select time at the chart above to view stacktraces")));
        panelCPUChart.add(new JBScrollPane(jTree));

        tcc.addMouseListener(new ChartMouseListenter(jTree, threadDataMap, memoryDataList, minTime, maxTime));
    }

    private void initSampingTreePanel(Tree samplingTree) {
        if (samplingTree == null) {
            panelSamples.add(new JLabel("No data available"));
            return;
        }
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new StackTraceElementInfo("", 0, 0));
        buildSamplingTree(root, samplingTree);
        final ColumnInfo[] columns = initSamplingTreeTableColumns();
        TreeTableModel model = new ListTreeTableModelOnColumns(root, columns);

        final TreeTable treeTable = new TreeTable(model);
        treeTable.getColumn(SAMPLES_COLUMNT_HEADER).setPreferredWidth(100);
        treeTable.getColumn(SAMPLES_COLUMNT_HEADER).setMinWidth(30);
        //treeTable.getColumn("Count").setMaxWidth(100);
        treeTable.getColumn(CPU_COLUMN_HEADER).setPreferredWidth(100);
        treeTable.getColumn(CPU_COLUMN_HEADER).setMinWidth(30);
        //treeTable.getColumn("CPU").setMaxWidth(30);
        treeTable.setPreferredSize(new Dimension(600, 600));

        panelSamples.add(new JBScrollPane(treeTable));
    }

    private ColumnInfo[] initSamplingTreeTableColumns() {
        ColumnInfo treeColumn = new ColumnInfo(SAMPLING_TREE_COLUMN_HEADER) {
            @Override
            public Object valueOf(Object o) {
                StackTraceElementInfo stei = (StackTraceElementInfo) ((DefaultMutableTreeNode) o).getUserObject();
                return stei.getName();
            }

            @Override
            public Class getColumnClass() {
                return TreeTableModel.class;
            }
        };
        ColumnInfo countColumn = new ColumnInfo(SAMPLES_COLUMNT_HEADER) {

            @Override
            public Object valueOf(Object o) {
                StackTraceElementInfo stei = (StackTraceElementInfo) ((DefaultMutableTreeNode) o).getUserObject();
                return Long.toString(stei.getCount());
            }
        };
        ColumnInfo cpuColumn = new ColumnInfo(CPU_COLUMN_HEADER) {

            @Override
            public Object valueOf(Object o) {
                StackTraceElementInfo stei = (StackTraceElementInfo) ((DefaultMutableTreeNode) o).getUserObject();
                return Double.toString(stei.getCpuTime() / 1000000000);
            }
        };

        return new ColumnInfo[]{treeColumn, countColumn, cpuColumn};
    }

    private void buildSamplingTree(DefaultMutableTreeNode uiTree, Tree samplingTree) {
        Map<String, Tree> subTrees = samplingTree.getSubTrees();
        if (subTrees != null) {
            for (Iterator<Map.Entry<String, Tree>> iterator = subTrees.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, Tree> entry = iterator.next();
                Tree tree = entry.getValue();
                StackTraceElementInfo stei = new StackTraceElementInfo(entry.getKey(), tree.getCounter(), tree.getCpuTotal());
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(stei);
                uiTree.add(newNode);
                buildSamplingTree(newNode, entry.getValue());
            }
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        return panelMain;
    }

    static class StackTraceElementInfo {
        private String name;
        private long count;
        private double cpuTime;

        public StackTraceElementInfo(String name, long count, double cpuTime) {
            this.name = name;
            this.count = count;
            this.cpuTime = cpuTime;
        }

        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }

        public double getCpuTime() {
            return cpuTime;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class ChartMouseListenter extends MouseAdapter {
        private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        //private JTextArea jTextArea;
        private JTree jTree;
        private Map<Long, ThreadDataSummary> map;
        private List<MemoryData> memoryDataList;
        private long minTime, maxTime;

        ChartMouseListenter(JTree jTree, Map<Long, ThreadDataSummary> map, List<MemoryData> memoryDataList, long minTime, long maxTime) {
            this.jTree = jTree;
            this.map = map;
            this.memoryDataList = memoryDataList;
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

        private String getMemoryData(long time) {
            int i = 0;
            while ((i < memoryDataList.size()) && memoryDataList.get(i).getSystemTime() < time) {
                i++;
            }
            if (i < memoryDataList.size()) {
                MemoryData data = memoryDataList.get(i);
                float usedHeap = data.getUsedHeap();
                float usedNonHeap = data.getUsedNonHeap();

                //convert to megabytes
                usedHeap = usedHeap / 1024 / 1024;
                usedNonHeap = usedNonHeap / 1024 /1024;
                Formatter formatter = new Formatter();
                return formatter.format("(usedHeap: %.2f Mb, usedNonHeap: %.2f Mb)", usedHeap, usedNonHeap).out().toString();
            } else {
                return null;
            }
        }

        private void updateTree(long time) {
            DefaultTreeModel treeModel = (DefaultTreeModel)jTree.getModel();
            DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
            Iterator<Map.Entry<Long, ThreadDataSummary>>  iter = map.entrySet().iterator();
            treeRoot.setUserObject("stack traces for " + dateFormat.format(new Date(time)) + " " + getMemoryData(time) + "\n");
            while (iter.hasNext()) {
                ThreadDataSummary tds = iter.next().getValue();
                StackTraceElement[] st = tds.getStackTrace(time);
                if (st != null) {
                    DefaultMutableTreeNode threadNode = new DefaultMutableTreeNode(tds.getThreadName() + "  " + tds.getStatus(time));
                    for (int i = 0; i < st.length; i++) {
                        DefaultMutableTreeNode stackTraceNode = new DefaultMutableTreeNode(st[i]);
                        threadNode.add(stackTraceNode);
                    }
                    treeRoot.add(threadNode);
                }
            }
            treeModel.setRoot(treeRoot);
        }
    }
}
