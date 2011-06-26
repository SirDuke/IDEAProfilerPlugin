package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBScrollPane;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public class ThreadDumpsPanel extends Splitter {
    ThreadDumpsPanel() {
        super(true, 0.5f, 0.05f, 0.9f);
    }

    void init(Collection<ThreadDump> threadDumps, long minTime, long maxTime) {
        if (threadDumps == null) {
            this.setFirstComponent(new JLabel("No Data available"));
            return;
        }

        ThreadChartComponent tcc = new ThreadChartComponent(threadDumps, minTime, maxTime);
        this.setFirstComponent(tcc);

        JPanel panelThreadDumps = new JPanel(new BorderLayout());
        this.setSecondComponent(panelThreadDumps);

        JPanel panelNorth = new JPanel(new BorderLayout());
        panelThreadDumps.add(panelNorth, BorderLayout.NORTH);
        JButton buttonFilterThreads = new JButton("Filter Threads");
        panelNorth.add(buttonFilterThreads, BorderLayout.WEST);
        panelNorth.add(new JLabel("       Hint: Select time at the chart above to view stacktraces"));

        final FilterThreadsDialog filterThreadsDialog = new FilterThreadsDialog(this, threadDumps);
        filterThreadsDialog.addThreadFilterListener(tcc);
        buttonFilterThreads.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filterThreadsDialog.setVisible(true);
            }
        });

        ThreadDumpsTree threadDumpsTree = new ThreadDumpsTree(threadDumps);
        filterThreadsDialog.addThreadFilterListener(threadDumpsTree);
        panelThreadDumps.add(new JBScrollPane(threadDumpsTree));

        tcc.addChartMouseListener(new ChartMouseListenter(threadDumpsTree, minTime, maxTime));
    }
}
