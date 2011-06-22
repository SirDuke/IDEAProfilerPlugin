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

package org.ssprofiler.idea.profileplugin.viewer;

import org.ssprofiler.model.ProfilerData;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 09.05.11
 */
public class CPUReportViewerImpl implements CPUReportViewer {
    private static final int PREFERRED_WIDTH = 1000;
    private static final int PREFERRED_HEIGHT = 600;
    private static final Dimension PREFERRED_SIZE = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);

    public void view(String filename) throws IOException {
        ProfilerData profilerData = new ProfilerData();
        try {
            profilerData.readData(filename);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Cannont parse file " + filename + ". Received exception: " + e.getMessage());
        }

        CPUReportDialog cpuReportDialog = new CPUReportDialog(false);
        CPUReportPanel cpuReportPanel = (CPUReportPanel)cpuReportDialog.getCPUReportPanel();
        cpuReportPanel.init(PREFERRED_SIZE,
                            profilerData.getThreadDumps(),
                            profilerData.getMemoryInfo(),
                            profilerData.getMethodSummaries(),
                            profilerData.getMinTime(),
                            profilerData.getMaxTime());
        cpuReportDialog.setSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        cpuReportDialog.getPeer().setTitle("CPU Report " + filename);
        cpuReportDialog.show();
    }

    public static void main(String[] args) {
       // VirtualFile[] files = FileChooser.chooseFiles(JOptionPane.getRootFrame(), new FileChooserDescriptor(true, false, false, false, false, false));
        String filename = System.getProperty("user.home") + File.separator + "cpu.cpu";
        //String  filename =  files[0].getPath();
        System.out.println(filename);
        try {
        ProfilerData profilerData = new ProfilerData();
        try {
            profilerData.readData(filename);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Cannont parse file " + filename + ". Received exception: " + e.getMessage());
        }

        JFrame frame = new JFrame();
        CPUReportPanel cpuReportPanel = new CPUReportPanel();
        cpuReportPanel.init(PREFERRED_SIZE,
                            profilerData.getThreadDumps(),
                            profilerData.getMemoryInfo(),
                            profilerData.getMethodSummaries(),
                            profilerData.getMinTime(),
                            profilerData.getMaxTime());
        frame.setContentPane(cpuReportPanel.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(PREFERRED_SIZE);
        frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
