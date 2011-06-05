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

package org.ssprofiler.idea.profileplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.ssprofiler.idea.profileplugin.viewer.ViewerManager;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 09.05.11
 */
public class ViewCPUReportAction extends AnAction {
    public void actionPerformed(AnActionEvent event) {
        /*String dir = System.getProperty("user.home");
        File dirUserHome = new File(dir);
        File[] files = dirUserHome.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                String s = pathname.getName();
                return ((s.startsWith("cpu")) && (s.endsWith("cpu")));
            }
        });*/

        String dir = System.getProperty("user.home");
        VirtualFile dirUserHome = LocalFileSystem.getInstance().findFileByPath(dir);
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        fileChooserDescriptor.setTitle("Select CPU report file");
        VirtualFile[] files = FileChooser.chooseFiles(JOptionPane.getRootFrame(), fileChooserDescriptor, dirUserHome);

        if (files.length > 0) {
            String filename = files[0].getPath();
            try {

                ViewerManager.getCPUReportViewer().view(filename);
            } catch (IOException e) {
                e.printStackTrace();
                Messages.showMessageDialog("Cannot parse the file: " + filename, "Error", null);
            }
        }
    }
}
