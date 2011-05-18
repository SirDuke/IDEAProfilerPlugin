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
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 09.05.11
 */
public class OktechCPUReportViewerImpl implements CPUReportViewer {
    private long minTime=Long.MAX_VALUE, maxTime=0;
    private Map<Long, ThreadDataSummary> threadDataMap;
    private List<MemoryData> memoryDataList;

    public void view(String filename) {
        Properties props = createOktechBuilderProperties(filename);
        readData(props);

        Tree samplingTree = null;
        try {
            TreeBuilder builder = new TreeBuilder();
            builder.process(props);
            samplingTree = builder.getSamplingTree();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*JFrame frame = new JFrame();
        OktechCPUReportPanel cpuReportPanel = new OktechCPUReportPanel();
        cpuReportPanel.init(threadDataMap, memoryDataList, samplingTree, minTime, maxTime);
        frame.setContentPane(cpuReportPanel.getMainPanel());
        frame.setSize(600, 600);
        frame.setVisible(true);*/

        OktechCPUReportPanel cpuReportPanel = new OktechCPUReportPanel(false);
        cpuReportPanel.init(threadDataMap, memoryDataList, samplingTree, minTime, maxTime);
        cpuReportPanel.getPeer().setTitle("CPU Report");
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
        new OktechCPUReportViewerImpl().view(System.getProperty("user.home") + File.separator + "cpu_184723_11472011.cpu");


        /*OktechCPUReportView view = new OktechCPUReportView(null, false);
        view.getPeer().setTitle("CPU Report");
        view.show();*/
        /*JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        //frame.setLayout(new GridLayout(3,1));

        OktechCPUReportViewerImpl ok = new OktechCPUReportViewerImpl();
        Properties props = ok.createOktechBuilderProperties("D:\\Users\\wpqb76\\cpu_184723_11472011.cpu");

        TreeBuilder builder = new TreeBuilder();
        try {
            builder.process(props);
            final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new StackTraceElementInfo("", 0, 0));
            ok.buildSamplingTree(root, builder.getSamplingTree());
            ColumnInfo treeColumn = new ColumnInfo("Sampling Tree") {
                @Override
                public Object valueOf(Object o) {
                    StackTraceElementInfo stei = (StackTraceElementInfo) ((DefaultMutableTreeNode)o).getUserObject();
                    return o;
                }

                @Override
                public Class getColumnClass() {
                    return TreeTableModel.class;
                }
            };
            ColumnInfo countColumn = new ColumnInfo("Count") {

                @Override
                public Object valueOf(Object o) {
                    StackTraceElementInfo stei = (StackTraceElementInfo) ((DefaultMutableTreeNode)o).getUserObject();
                    return Long.toString(stei.getCount());
                }
            };
            ColumnInfo cpuColumn = new ColumnInfo("CPU") {

                @Override
                public Object valueOf(Object o) {
                    StackTraceElementInfo stei = (StackTraceElementInfo) ((DefaultMutableTreeNode)o).getUserObject();
                    return Double.toString(stei.getCpuTime() / 1000000000);
                }
            };
            //TreeTable treeTable = new TreeTable(new ListTreeTableModelOnColumns(root, new ColumnInfo[] {treeColumn, countColumn, cpuColumn}));

            final ColumnInfo[] columns = new ColumnInfo[] {treeColumn, countColumn, cpuColumn};
            TreeTableModel model = new ListTreeTableModel(root, columns);

            final TreeTable treeTable = new TreeTable(model);
            //JTree jTree = new com.intellij.ui.treeStructure.Tree(new DefaultTreeModel(root));
            treeTable.setPreferredSize(new Dimension(600, 600));

            frame.add(new JBScrollPane(treeTable));
            frame.setSize(600, 600);
            frame.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XMLStreamException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

        }*/

        /*ok.readData(props);
        Map<Long, ThreadDataSummary> map = ok.threadDataMap;

        ThreadChartComponent tcc = new ThreadChartComponent(map, ok.minTime, ok.maxTime);
        frame.add(new JBScrollPane(tcc));

        MemoryChartComponent mcc = new MemoryChartComponent(ok.memoryDataList);
        frame.add(new JBScrollPane(mcc));

        frame.setVisible(true);
        tcc.init();
        mcc.init();

        //JTextArea jTextArea = new JTextArea();

        JTree jTree = new com.intellij.ui.treeStructure.Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
       // JTree jTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));

        tcc.addMouseListener(new ChartMouseListenter(jTree, map, ok.minTime, ok.maxTime));
        frame.add(new JBScrollPane(jTree));*/


    }
}
