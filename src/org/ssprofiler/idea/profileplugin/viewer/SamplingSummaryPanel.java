package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import org.ssprofiler.model.MethodSummary;
import org.ssprofiler.model.StackTraceTree;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 24.05.11
 */
public class SamplingSummaryPanel extends JPanel {
    private static final String[] COLUMN_NAMES = {"Method", "Count", "Cpu time", "Cpu time(own)", "Time", "Time(own)"};
    private static final int NANO = 1000000000;
    private static final DecimalFormat doubleFormat = new DecimalFormat("#.###");

    private List<MethodSummary> methodSummaries;
    private JTable tableMethods;
    private TreeTable treeTableMethodUsages;
    private MethodUsageTreeNode root;

    public SamplingSummaryPanel() {
    }

    void init(final List<MethodSummary> methodSummaries, final Collection<ThreadDump> threadDumps,  Dimension preferredSize) {
        this.methodSummaries = methodSummaries;

        setPreferredSize(preferredSize);
        setLayout(new GridLayout(2,1));

        tableMethods = new JBTable(new MethodsInfoTableModel());
        tableMethods.setDefaultRenderer(Double.class, new DoubleTableCellRenderer());
        tableMethods.setAutoscrolls(true);
        tableMethods.setAutoCreateRowSorter(true);

        root = new MethodUsageTreeNode("Select method in the table above");
        final ListTreeTableModel treeTablemodel = new ListTreeTableModel(root, createColumns());
        treeTableMethodUsages = new TreeTable(treeTablemodel);
        treeTableMethodUsages.setDefaultRenderer(Double.class, new DoubleTableCellRenderer());

        int columnCount = COLUMN_NAMES.length;
        setColumnSizes(tableMethods.getColumnModel(), preferredSize.width);
        setColumnSizes(treeTableMethodUsages.getColumnModel(), preferredSize.width);

        tableMethods.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int selectedIndex = tableMethods.getSelectedRow();
                if (selectedIndex != -1) {
                    int modelSelectedIndex = tableMethods.getRowSorter().convertRowIndexToModel(selectedIndex);
                    MethodSummary methodSummary = methodSummaries.get(modelSelectedIndex);
                    StackTraceTree tree = methodSummary.getCallers(threadDumps);
                    root.removeAllChildren();
                    root.setTree(tree);
                    updateTreeMethodUsages(tree.getChildren(), root);
                    treeTablemodel.nodeStructureChanged(root);

                }
            }
        });

        add(new JBScrollPane(tableMethods));
        add(new JBScrollPane(treeTableMethodUsages));
    }

    private void updateTreeMethodUsages(Collection<StackTraceTree> subTrees, DefaultMutableTreeNode curRoot) {
        if (subTrees != null) {
            for (Iterator<StackTraceTree> iterator = subTrees.iterator(); iterator.hasNext();) {
                StackTraceTree tree = iterator.next();
                DefaultMutableTreeNode node = new MethodUsageTreeNode(tree);
                curRoot.add(node);
                updateTreeMethodUsages(tree.getChildren(), node);
            }
        }
    }

    private void setColumnSizes(TableColumnModel columnModel, int tablePreferredWidth) {
        int columnCount = columnModel.getColumnCount();
        TableColumn column = columnModel.getColumn(0);
        column.setPreferredWidth((int) (tablePreferredWidth * 0.6));
        double k = 0.4 / (columnCount - 1);
        for (int i = 1; i < columnCount; i++) {
            column = columnModel.getColumn(i);
            column.setPreferredWidth((int) (tablePreferredWidth * k));
        }

    }

    private ColumnInfo[] createColumns() {
            ColumnInfo treeColumn = new ColumnInfo(COLUMN_NAMES[0]) {
            @Override
            public Object valueOf(Object o) {
                return ((MethodUsageTreeNode)o).getUserObject();
            }

            @Override
            public Class getColumnClass() {
                return TreeTableModel.class;
            }


            };

            ColumnInfo countColumn = new ColumnInfo(COLUMN_NAMES[1]) {
            @Override
            public Object valueOf(Object o) {
                return ((MethodUsageTreeNode)o).getCounter();
            }

                @Override
                public Class getColumnClass() {
                    return Long.class;
                }
            };
        ColumnInfo cpuTotalColumn = new ColumnInfo(COLUMN_NAMES[2]) {
            @Override
            public Object valueOf(Object o) {
                return ((MethodUsageTreeNode)o).getCpuTotal();
            }

            @Override
            public Class getColumnClass() {
                return Double.class;
            }
        };
        ColumnInfo cpuRealColumn = new ColumnInfo(COLUMN_NAMES[3]) {
            @Override
            public Object valueOf(Object o) {
                return ((MethodUsageTreeNode)o).getCpuReal();
            }

            @Override
            public Class getColumnClass() {
                return Double.class;
            }
        };
        ColumnInfo sysTotalColumn = new ColumnInfo(COLUMN_NAMES[4]) {
            @Override
            public Object valueOf(Object o) {
                return ((MethodUsageTreeNode)o).getSystemTotal();
            }

            @Override
            public Class getColumnClass() {
                return Double.class;
            }
        };
        ColumnInfo sysRealColumn = new ColumnInfo(COLUMN_NAMES[5]) {
            @Override
            public Object valueOf(Object o) {
                return ((MethodUsageTreeNode)o).getSystemReal();
            }

            @Override
            public Class getColumnClass() {
                return Double.class;
            }
        };

        return new ColumnInfo[]{treeColumn, countColumn, cpuTotalColumn, cpuRealColumn, sysTotalColumn, sysRealColumn};
        }

    class MethodsInfoTableModel extends AbstractTableModel {
        public int getRowCount() {
            return methodSummaries.size();
        }

        public int getColumnCount() {
            return 6;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            MethodSummary methodSummary = methodSummaries.get(rowIndex);
            switch (columnIndex) {
                case 0: return methodSummary.getMethodName();
                case 1: return methodSummary.getCount();
                case 2: return (double) methodSummary.getCpuTimeTotal() / NANO;
                case 3: return (double) methodSummary.getCpuTimeOwn() / NANO;
                case 4: return (double) methodSummary.getSystemTimeTotal() / NANO;
                case 5: return (double) methodSummary.getSystemTimeOwn() / NANO;

            }
            return -1;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 1: return Long.class;
                case 2:
                case 3:
                case 4:
                case 5: return Double.class;
            }
            return super.getColumnClass(columnIndex);
        }
    }

    class DoubleTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            setHorizontalAlignment(JLabel.RIGHT);
             setText((value != null) ?doubleFormat.format(value) : "");
        }
    }

    class MethodUsageTreeNode extends DefaultMutableTreeNode {
        private StackTraceTree tree;

        MethodUsageTreeNode(StackTraceTree tree) {
            this.tree = tree;
            setUserObject(tree.getMethodName());
        }

        MethodUsageTreeNode(String s) {
            setUserObject(s);
        }

        void setTree(StackTraceTree tree) {
            this.tree = tree;
            setUserObject(tree.getMethodName());
        }

        Integer getCounter() {
            return (tree != null) ? tree.getCount() : null;
        }

        Double getCpuTotal() {
            return (tree != null) ? (double)tree.getCpuTimeTotal() / NANO : null;
        }

        Double getCpuReal() {
            return (tree != null) ? (double)tree.getCpuTimeOwn() / NANO : null;
        }

        Double getSystemTotal() {
            return (tree != null) ? (double)tree.getSystemTimeTotal() / NANO : null;
        }

        Double getSystemReal() {
            return (tree != null) ? (double)tree.getSystemTimeOwn() / NANO : null;
        }
    }
}

