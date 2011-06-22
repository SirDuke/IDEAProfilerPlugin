package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.ui.components.JBScrollPane;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public class FilterThreadsDialog extends JDialog {
    private static final int COLUMNS = 3;

    private ThreadInfo[] threads;
    private String[] selectedThreadNames;
    private ArrayList<ThreadFilterListener> listeners = new ArrayList<ThreadFilterListener>();

    FilterThreadsDialog(Component parentComponent, Collection<ThreadDump> threadDumps) {
        super((Dialog)null, "Filter threads", true);

        threads = new ThreadInfo[threadDumps.size()];
        selectedThreadNames = new String[threadDumps.size()];

        this.getRootPane().setLayout(new BorderLayout());

        JPanel panelCheckBoxes = new JPanel(new GridLayout(threads.length / COLUMNS + 1, COLUMNS));

        Iterator<ThreadDump> iter = threadDumps.iterator();
        int maxWidth = 0;
        int i = 0;
        JCheckBox tempCheckBox = new JCheckBox();
        FontMetrics fontMetrics = tempCheckBox.getFontMetrics(tempCheckBox.getFont());
        while (iter.hasNext()) {
            ThreadDump threadDump = iter.next();
            threads[i] = new ThreadInfo();
            threads[i].checkBox = new JCheckBox(threadDump.getName(), true);
            threads[i].threadDump = threadDump;
            panelCheckBoxes.add(threads[i].checkBox);
            selectedThreadNames[i] = threadDump.getName();
            int width = fontMetrics.stringWidth(threadDump.getName());
            if (width > maxWidth) maxWidth = width;
            i++;
        }
        this.getRootPane().add(new JBScrollPane(panelCheckBoxes));

        JPanel panelSouth = new JPanel(new BorderLayout());
        JPanel panelButtons = new JPanel(new GridLayout(1, 2));
        JButton buttonOk = new JButton("OK");
        panelButtons.add(buttonOk);
        JButton buttonCancel = new JButton("Cancel");
        panelButtons.add(buttonCancel);
        panelSouth.add(panelButtons, BorderLayout.EAST);
        panelSouth.add(new JLabel());
        this.getRootPane().add(panelSouth, BorderLayout.SOUTH);

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateSelectedThreads();
                FilterThreadsDialog.this.setVisible(false);
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetCheckBoxes();
                FilterThreadsDialog.this.setVisible(false);
            }
        });

        JButton buttonSelectAll = new JButton("Select All");
        buttonSelectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectAllCheckboxes(true);
            }
        });
        JButton buttonUnselectAll = new JButton("Unselect All");
        buttonUnselectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectAllCheckboxes(false);
            }
        });
        JPanel panelNorth = new JPanel(new BorderLayout());
        JPanel panelSelectButtons = new JPanel(new GridLayout(1, 2));
        panelSelectButtons.add(buttonSelectAll);
        panelSelectButtons.add(buttonUnselectAll);
        panelNorth.add(panelSelectButtons, BorderLayout.WEST);
        panelNorth.add(new JLabel()); //HSpacer

        this.getRootPane().add(panelNorth, BorderLayout.NORTH);

        this.setPreferredSize(new Dimension((maxWidth + 34) * COLUMNS, 400));
        this.pack();
        setLocationRelativeTo(parentComponent);

    }

    public void addThreadFilterListener(ThreadFilterListener listener) {
        listeners.add(listener);
    }

    public void removeThreadFilterListener(ThreadFilterListener listener) {
        listeners.remove(listener);
    }

    private void selectAllCheckboxes(boolean selected) {
        for (int i = 0; i < threads.length; i++) {
            ThreadInfo thread = threads[i];
            thread.checkBox.setSelected(selected);
        }
    }

    private void notifyListeners(Collection<ThreadDump> selectedThreads) {
        for (Iterator<ThreadFilterListener> iterator = listeners.iterator(); iterator.hasNext();) {
            ThreadFilterListener listener = iterator.next();
            listener.selectionChanged(selectedThreads);
        }
    }

    private void resetCheckBoxes() {
        int currentSelected = 0;
        for (int i = 0; i < threads.length; i++) {
            if ((currentSelected < selectedThreadNames.length) &&
                    (threads[i].checkBox.getText().equals(selectedThreadNames[currentSelected]))) {
                threads[i].checkBox.setSelected(true);
            } else {
                threads[i].checkBox.setSelected(false);
            }
        }
    }

    private void updateSelectedThreads() {
        List<String> selectedThreads = new ArrayList<String>();
        ArrayList<ThreadDump> selectedThreadDumps = new ArrayList<ThreadDump>();
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].checkBox.isSelected()) {
                selectedThreads.add(threads[i].checkBox.getText());
                selectedThreadDumps.add(threads[i].threadDump);
            }
        }
        selectedThreadNames = selectedThreads.toArray(new String[selectedThreads.size()]);
        notifyListeners(Collections.unmodifiableList(selectedThreadDumps));
    }

    static class ThreadInfo {
        JCheckBox checkBox;
        ThreadDump threadDump;
    }
}
