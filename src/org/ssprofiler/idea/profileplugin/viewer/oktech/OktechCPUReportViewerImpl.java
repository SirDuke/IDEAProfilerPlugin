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

import hu.oktech.profiler.analyzer.tree.Tree;
import hu.oktech.profiler.analyzer.tree.TreeBuilder;
import hu.oktech.profiler.core.data.MemoryData;
import hu.oktech.profiler.core.data.ThreadData;
import hu.oktech.profiler.core.stream.StreamFilter;
import hu.oktech.profiler.core.stream.StreamInput;
import hu.oktech.profiler.core.stream.StreamProcessor;
import org.ssprofiler.idea.profileplugin.viewer.CPUReportViewer;

import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 09.05.11
 */
public class OktechCPUReportViewerImpl implements CPUReportViewer {
    private static final int PREFERRED_WIDTH = 1000;
    private static final int PREFERRED_HEIGHT = 600;
    private static final Dimension PREFERRED_SIZE = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);

    private long minTime=Long.MAX_VALUE, maxTime=0;
    private Map<Long, ThreadDataSummary> threadDataMap;
    private List<MemoryData> memoryDataList;

    public void view(String filename) {
        Properties props = createOktechBuilderProperties(filename);
        readData(props);

        Tree samplingTree = null;
        Tree samplingSummaryTree = null;
        try {
            TreeBuilder builder = new TreeBuilder();
            builder.process(props);
            samplingTree = builder.getSamplingTree();
            samplingSummaryTree = builder.getSamplingSummary();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*JFrame frame = new JFrame();
        OktechCPUReportPanel cpuReportPanel = new OktechCPUReportPanel(false);
        cpuReportPanel.init(PREFERRED_SIZE, threadDataMap, memoryDataList, samplingTree, samplingSummaryTree,  minTime, maxTime);
        frame.setContentPane(cpuReportPanel.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(PREFERRED_SIZE);
        frame.setVisible(true);*/
        OktechCPUReportPanel cpuReportPanel = new OktechCPUReportPanel(false);
        cpuReportPanel.setSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        cpuReportPanel.init(PREFERRED_SIZE, threadDataMap, memoryDataList, samplingTree, samplingSummaryTree,  minTime, maxTime);
        cpuReportPanel.getPeer().setTitle("CPU Report " + filename);
        cpuReportPanel.show();
    }

    private Properties createOktechBuilderProperties(String inputFileName) {
        Properties props = new Properties();
        props.setProperty("report.sampling.tree", "true");
        props.setProperty("report.sampling.summary", "true");
        props.setProperty("report.instr.tree", "false");
        props.setProperty("report.instr.summary", "false");

        props.setProperty("input", inputFileName);

        return props;
    }

    private void readData(Properties props) {
        threadDataMap = new HashMap<Long, ThreadDataSummary>();
        memoryDataList = new ArrayList<MemoryData>();
        try {
            StreamFilter streamFilter = new StreamFilter(new StreamProcessor() {
                @Override
                protected void processThread(ThreadData thread) {
                    ThreadDataSummary tds = threadDataMap.get(thread.getThreadId());
                    if (tds == null) {
                        tds = new ThreadDataSummary();
                        threadDataMap.put(thread.getThreadId(), tds);
                    }
                    tds.add(thread);
                    long time = thread.getSystemTime();
                    if (time < minTime) minTime = time;
                    if (time > maxTime) maxTime = time;
                }

                @Override
                protected void processMemory(MemoryData memory) {
                    memoryDataList.add(memory);
                }
            }, props);

            String filename = props.getProperty("input");
            StreamInput in = new StreamInput();
            in.setInputFile(filename);
            in.read(streamFilter);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XMLStreamException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }





    public static void main(String[] args) {
        new OktechCPUReportViewerImpl().view(System.getProperty("user.home") + File.separator + "cpu_153845_25052011.cpu");
    }
}
