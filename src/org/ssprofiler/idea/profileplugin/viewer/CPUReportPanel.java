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
import org.ssprofiler.idea.profileplugin.projectcontext.ProjectContext;
import org.ssprofiler.model.MemoryInfo;
import org.ssprofiler.model.MethodSummary;
import org.ssprofiler.model.ProfilerData;
import org.ssprofiler.model.ThreadDump;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: Ivan Serduk
 * Date: 14.05.11
 */
public class CPUReportPanel extends JPanel {
    private static final int PREFERRED_WIDTH = 1000;
    private static final int PREFERRED_HEIGHT = 600;
    private static final Dimension PREFERRED_SIZE = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);

    private ThreadDumpsPanel panelThreadDumps;
    private SamplesPanel panelSamples;
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
        panelSamples = new SamplesPanel();
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
        panelSamples.init(threadDumps, projectContext);
    }

    private void initSamplingSummaryTree(Collection<ThreadDump> threadDumps, List<MethodSummary> methodSummaries) {
        if (methodSummaries == null) {
            panelSamplingSummary.add(new JLabel("No data available"));
            return;
        }

        ((SamplingSummaryPanel)panelSamplingSummary).init(methodSummaries, threadDumps, preferredSize, projectContext);

    }
}
