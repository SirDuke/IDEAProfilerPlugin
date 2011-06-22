package org.ssprofiler.idea.profileplugin.viewer;

import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 21.06.11
 */
public class CPUReportDialog extends DialogWrapper {
    private CPUReportPanel cpuReportPanel;

    protected CPUReportDialog(boolean canBeParent) {
        super(canBeParent);
        cpuReportPanel = new CPUReportPanel();
        init();

    }

    CPUReportPanel getCPUReportPanel() {
        return cpuReportPanel;
    }

    @Override
    protected JComponent createCenterPanel() {
        return cpuReportPanel.getMainPanel();
    }
}
