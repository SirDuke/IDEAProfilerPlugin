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
import org.ssprofiler.model.StackTraceTree;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Collection;

/**
 * User: Ivan Serduk
 * Date: 03.03.12
 */
public class SamplesPanel extends JPanel {
    private static final long NANO = 1000000000;

    private static final String SAMPLING_TREE_COLUMN_HEADER = "Sampling Tree";
    private static final String SAMPLES_COLUMN_HEADER = "Samples count";
    private static final String CPU_COLUMN_HEADER = "CPU time usage";
    
    public SamplesPanel() {
        super(new BorderLayout());
        setNoData();
    }
    
    public void init(Collection<ThreadDump> threadDumps, ProjectContext projectContext)  {
        clear();
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

        add(new JBScrollPane(treeTable));

    }

    private void buildSamplingTree(DefaultMutableTreeNode treeNode, Collection<StackTraceTree> children) {
        for (StackTraceTree subtree : children) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(subtree);
            treeNode.add(newNode);
            buildSamplingTree(newNode, subtree.getChildren());

        }
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

    public void clear() {
        removeAll();
    }
    
    private void setNoData() {
        add(new JLabel("No data available"));
    }
}
