/*
 * Copyright 2011-2012, Ivan Serduk. All rights reserved.
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

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import org.ssprofiler.model.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 14.05.11
 */
public class CPUReportPanel extends JPanel {
    private static final long NANO = 1000000000;

    private static String SAMPLING_TREE_COLUMN_HEADER = "Sampling Tree";
    private static String SAMPLES_COLUMN_HEADER = "Samples count";
    private static String CPU_COLUMN_HEADER = "CPU time usage";

    private static final int PREFERRED_WIDTH = 1000;
    private static final int PREFERRED_HEIGHT = 600;
    private static final Dimension PREFERRED_SIZE = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);

    private ThreadDumpsPanel panelThreadDumps;
    private JPanel panelSamples;
    private JPanel panelMemory;
    private JPanel panelSamplingSummary;

    private Map<Long, ThreadDump> threadDumps;
    private List<MemoryInfo> memoryDataList;
    private long minTime, maxTime;
    private DecimalFormat doubleFormat = new DecimalFormat("##.##");
    private Dimension preferredSize;

    public CPUReportPanel() {
        super(new BorderLayout());
        JTabbedPane jTabbedPane = new JTabbedPane();
        this.add(jTabbedPane);
        panelThreadDumps = new ThreadDumpsPanel();
        jTabbedPane.add("Threads", panelThreadDumps);
        panelSamples = new JPanel();
        jTabbedPane.add("Samples", panelSamples);
        panelMemory = new JPanel();
        jTabbedPane.add("Memory", panelMemory);
        panelSamplingSummary = new SamplingSummaryPanel();
        jTabbedPane.add("Sampling Summary", panelSamplingSummary);
    }

    public void loadDataFromFile(String filename) throws IOException {
        ProfilerData profilerData = new ProfilerData();
        try {
            profilerData.readData(filename);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Cannot parse file " + filename + ". Received exception: " + e.getMessage());
        }
        init(PREFERRED_SIZE,
                profilerData.getThreadDumps(),
                profilerData.getMemoryInfo(),
                profilerData.getMethodSummaries(),
                profilerData.getMinTime(),
                profilerData.getMaxTime());
    }
    
    private void init(Dimension preferredSize,
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
        this.setPreferredSize(preferredSize);
        panelThreadDumps.init(threadDumps.values(), minTime, maxTime);
        initMemoryChartPanel();
        initSampingTreePanel(threadDumps.values());
        initSamplingSummaryTree(threadDumps.values(), methodSummaries);
    }

    private void initMemoryChartPanel() {
        if (memoryDataList == null) {
            panelMemory.add(new JLabel("No Data available"));
            return;
        }
        panelMemory.setLayout(new GridLayout(2, 1));

        MemoryChartComponent mmc = new MemoryChartComponent(memoryDataList);
        panelMemory.add(new JBScrollPane(mmc));

        ThreadDumpsTree threadDumpsTree = new ThreadDumpsTree(threadDumps.values());
        panelMemory.add(new JBScrollPane(threadDumpsTree));

        mmc.addMouseListener(new ChartMouseListenter(threadDumpsTree, minTime, maxTime));
    }

    private void initSampingTreePanel(Collection<ThreadDump> threadDumps) {
        if (threadDumps == null) {
            panelSamples.add(new JLabel("No data available"));
            return;
        }
        panelSamples.setLayout(new BorderLayout());
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new StackTraceTree("", 0));
        for (ThreadDump threadDump : threadDumps) {
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
        for (StackTraceTree subtree : children) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(subtree);
            treeNode.add(newNode);
            buildSamplingTree(newNode, subtree.getChildren());

        }
    }
}
