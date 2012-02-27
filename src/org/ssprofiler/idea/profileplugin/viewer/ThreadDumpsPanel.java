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

import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBScrollPane;
import org.ssprofiler.idea.profileplugin.projectcontext.ProjectContext;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public class ThreadDumpsPanel extends Splitter {
    ThreadDumpsPanel() {
        super(true, 0.5f, 0.05f, 0.9f);
    }

    void init(Collection<ThreadDump> threadDumps, long minTime, long maxTime, ProjectContext projectContext) {
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

        ThreadDumpsTree threadDumpsTree = new ThreadDumpsTree(threadDumps, projectContext);
        filterThreadsDialog.addThreadFilterListener(threadDumpsTree);
        panelThreadDumps.add(new JBScrollPane(threadDumpsTree));

        tcc.addChartMouseListener(new ChartMouseListenter(threadDumpsTree, minTime, maxTime));
    }
}
