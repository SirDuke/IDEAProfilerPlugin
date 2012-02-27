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

package org.ssprofiler.idea.profileplugin.viewer.toolwindow;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.ssprofiler.idea.profileplugin.projectcontext.ProjectContext;
import org.ssprofiler.idea.profileplugin.viewer.CPUReportPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * User: Ivan Serduk
 * Date: 17.02.12
 */
public class ToolWindowContent extends JPanel {
    private CPUReportPanel cpuReportPanel;

    public ToolWindowContent(ProjectContext projectContext) {
        super(new BorderLayout());
        createCpuReportPanel(projectContext);
        createNorthPanel();
    }

    private void createCpuReportPanel(ProjectContext projectContext) {
        cpuReportPanel = new CPUReportPanel(projectContext);
        this.add(cpuReportPanel);
    }
    
    private void createNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout());
        this.add(northPanel, BorderLayout.NORTH);
        
        JButton jButtonOpenFile = new JButton("Open File");
        northPanel.add(jButtonOpenFile, BorderLayout.WEST);

        jButtonOpenFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showFileChooserDialogAndLoadSelectedData();
            }
        });
    }

    private void showFileChooserDialogAndLoadSelectedData() {
        VirtualFile dirUserHome = LocalFileSystem.getInstance().findFileByPath("user.home");
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        fileChooserDescriptor.setTitle("Select CPU report file");
        VirtualFile file = FileChooser.chooseFile(JOptionPane.getRootFrame(), fileChooserDescriptor, dirUserHome);

        if (file != null) {
            String filename = file.getPath();
            try {

                cpuReportPanel.loadDataFromFile(filename);
            } catch (IOException e) {
                e.printStackTrace();
                Messages.showMessageDialog("Cannot parse the file: " + filename, "Error", null);
            }
        }
    }
}
