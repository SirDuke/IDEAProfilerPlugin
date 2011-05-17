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
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import org.ssprofiler.idea.profileplugin.command.CommandManager;
import org.ssprofiler.idea.profileplugin.command.ProfilingCommand;
import org.ssprofiler.idea.profileplugin.util.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 05.05.11
 */
public class ProfilingAction extends AnAction {
    private static String START_TEXT = "Start CPU profiling";
    private static String STOP_TEXT = "Stop CPU profiling";

    private boolean isStarted = false;
    private ProfilingCommand profilingCommand;
    private String filename;

    public void actionPerformed(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (!isStarted) {
            profilingCommand = CommandManager.getProfilingCommand();
            filename = Utils.createCPUDumpFileName(System.getProperty("user.home"));
            profilingCommand.start(filename);
            isStarted = true;
            presentation.setDescription(STOP_TEXT);
            presentation.setText(STOP_TEXT);
            presentation.setIcon(IconLoader.getIcon("/icons/stop.png"));
        } else {
            profilingCommand.stop();
            isStarted = false;
            Messages.showMessageDialog("Profiling data is saved to " + filename, "CPU Profiler", null);
            presentation.setDescription(START_TEXT);
            presentation.setText(START_TEXT);
            presentation.setIcon(IconLoader.getIcon("/icons/start.png"));
        }
        presentation.setVisible(true);
    }
}
