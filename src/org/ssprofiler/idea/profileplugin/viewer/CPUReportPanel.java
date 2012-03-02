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

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import org.ssprofiler.idea.profileplugin.projectcontext.ProjectContext;
import org.ssprofiler.model.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * User: Ivan Serduk
 * Date: 14.05.11
 */
public class CPUReportPanel extends JPanel {
    private static final long NANO = 1000000000;

    private static final String SAMPLING_TREE_COLUMN_HEADER = "Sampling Tree";
    private static final String SAMPLES_COLUMN_HEADER = "Samples count";
    private static final String CPU_COLUMN_HEADER = "CPU time usage";

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
    
    private ProjectContext projectContext;

    public CPUReportPanel(ProjectContext projectContext) {
        super(new BorderLayout());

        this.projectContext = projectContext;

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
        panelThreadDumps.init(threadDumps.values(), minTime, maxTime, projectContext);
        initMemoryChartPanel();
        initSamplingTreePanel(threadDumps.values());
        initSamplingSummaryTree(threadDumps.values(), methodSummaries);
    }

    private void initMemoryChartPanel() {
        if (memoryDataList == null) {
            panelMemory.add(new JLabel("No Data available"));
            return;
        }
        panelMemory.removeAll();

        panelMemory.setLayout(new GridLayout(2, 1));

        MemoryChartComponent mmc = new MemoryChartComponent(memoryDataList);
        panelMemory.add(new JBScrollPane(mmc));

        ThreadDumpsTree threadDumpsTree = new ThreadDumpsTree(threadDumps.values(), projectContext);
        panelMemory.add(new JBScrollPane(threadDumpsTree));

        mmc.addMouseListener(new ChartMouseListenter(threadDumpsTree, minTime, maxTime));
    }

    private void initSamplingTreePanel(Collection<ThreadDump> threadDumps) {
        if (threadDumps == null) {
            panelSamples.add(new JLabel("No data available"));
            return;
        }

        panelSamples.removeAll();

        panelSamples.setLayout(new BorderLayout());
        final TreeNodeWithSortableChildren root = new TreeNodeWithSortableChildren("Threads");
        for (ThreadDump threadDump : threadDumps) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(threadDump.getStackTraceTree());
            root.add(newNode);
            buildSamplingTree(newNode, threadDump.getStackTraceTree().getChildren());
        }
        
        final ColumnInfo[] columns = initSamplingTreeTableColumns();
        ListTreeTableModelOnColumns model = new ListTreeTableModelOnColumns(root, columns);

        final TreeTable treeTable = new TreeTable(model);
        TreeTableRowSorter rowSorter = new TreeTableRowSorter(treeTable.getModel(), model, columns);
        treeTable.setRowSorter(rowSorter);

        TreeTablePopupTriggerMouseListener treeTablePopupTriggerMouseListener = new TreeTablePopupTriggerMouseListener(treeTable, projectContext);
        treeTable.addMouseListener(treeTablePopupTriggerMouseListener);

        panelSamples.add(new JBScrollPane(treeTable));
    }

    private void initSamplingSummaryTree(Collection<ThreadDump> threadDumps, List<MethodSummary> methodSummaries) {
        if (methodSummaries == null) {
            panelSamplingSummary.add(new JLabel("No data available"));
            return;
        }

        ((SamplingSummaryPanel)panelSamplingSummary).init(methodSummaries, threadDumps, preferredSize, projectContext);

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
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) o;
                if (!treeNode.isRoot()) {
                    StackTraceTree stackTraceTree = (StackTraceTree) treeNode.getUserObject();
                    return stackTraceTree.getCount();
                } else {
                    return null;
                }
            }
        };
        ColumnInfo cpuColumn = new ColumnInfo(CPU_COLUMN_HEADER) {

            @Override
            public Object valueOf(Object o) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) o;
                if (!treeNode.isRoot()) {
                    StackTraceTree stackTraceTree = (StackTraceTree) treeNode.getUserObject();
                    return stackTraceTree.getCpuTimeTotal() / NANO;
                } else {
                    return null;
                }
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
    
    private class TreeNodeWithSortableChildren extends DefaultMutableTreeNode {
        private TreeNodeWithSortableChildren(Object userObject) {
            super(userObject);
        }
        
        public void sortChildren(Comparator<DefaultMutableTreeNode> comparator) {
            Collections.sort(children, comparator);
        }
    }

    private class TreeTableRowSorter extends RowSorter<TableModel> {
        private TableModel tableModel;
        private ListTreeTableModelOnColumns listTreeTableModel;
        private TreeNodeWithSortableChildren root;
        private ColumnInfo[] columns;
        
        private SortKey currentSortKey = null;

        private TreeTableRowSorter(TableModel tableModel, ListTreeTableModelOnColumns listTreeTableModel, ColumnInfo[] columns) {
            this.tableModel = tableModel;
            this.listTreeTableModel = listTreeTableModel;
            this.root = (TreeNodeWithSortableChildren) listTreeTableModel.getRoot();
            this.columns = columns;
        }

        @Override
        public TableModel getModel() {
            return tableModel;
        }

        @Override
        public void toggleSortOrder(final int column) {
            SortOrder sortOrder = SortOrder.ASCENDING;
            if ((currentSortKey != null) && (currentSortKey.getColumn() == column) && (currentSortKey.getSortOrder() == SortOrder.ASCENDING)) {
                sortOrder = SortOrder.DESCENDING;
            }
            currentSortKey = new SortKey(column, sortOrder);
            final int sortKoef = (sortOrder == SortOrder.ASCENDING) ? -1 : 1;
            root.sortChildren(new Comparator<DefaultMutableTreeNode>() {
                public int compare(DefaultMutableTreeNode treeNode1, DefaultMutableTreeNode treeNode2) {
                    Comparable value1 = (Comparable) columns[column].valueOf(treeNode1);
                    Object value2 = columns[column].valueOf(treeNode2);
                    return sortKoef * value1.compareTo(value2);
                }
            });
            listTreeTableModel.nodeStructureChanged(root);
        }

        @Override
        public int convertRowIndexToModel(int index) {
            return index;
        }

        @Override
        public int convertRowIndexToView(int index) {
            return index;
        }

        @Override
        public void setSortKeys(List<? extends SortKey> keys) {
        }

        @Override
        public List<? extends SortKey> getSortKeys() {
            if (currentSortKey != null) {
                return Collections.singletonList(currentSortKey);
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public int getViewRowCount() {
            return tableModel.getRowCount();
        }

        @Override
        public int getModelRowCount() {
            return tableModel.getRowCount();
        }

        @Override
        public void modelStructureChanged() {
        }

        @Override
        public void allRowsChanged() {
        }

        @Override
        public void rowsInserted(int firstRow, int endRow) {
        }

        @Override
        public void rowsDeleted(int firstRow, int endRow) {
        }

        @Override
        public void rowsUpdated(int firstRow, int endRow) {
        }

        @Override
        public void rowsUpdated(int firstRow, int endRow, int column) {
        }
    }
}
