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

import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.util.ui.ColumnInfo;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collections;
import java.util.Comparator;

/**
 * Row sorter for Tree table. Sort child elements of root element from the TreeTable.
 * Changes underlined ListTreeTableModelOnColumns model during sorting
 *
 * User: Ivan Serduk
 * Date: 03.03.12
 */
public class TreeTableRowSorter extends RowSorter<TableModel> {
    private TableModel tableModel;
    private ListTreeTableModelOnColumns listTreeTableModel;
    private TreeNodeWithSortableChildren root;
    private ColumnInfo[] columns;

    private SortKey currentSortKey = null;

    public TreeTableRowSorter(TableModel tableModel, ListTreeTableModelOnColumns listTreeTableModel, ColumnInfo[] columns) {
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
    public void setSortKeys(java.util.List<? extends SortKey> keys) {
    }

    @Override
    public java.util.List<? extends SortKey> getSortKeys() {
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
