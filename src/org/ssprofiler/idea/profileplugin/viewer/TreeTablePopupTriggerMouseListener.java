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

import com.intellij.ui.treeStructure.treetable.TreeTable;
import org.ssprofiler.idea.profileplugin.projectcontext.ProjectContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * User: Ivan Serduk
 * Date: 27.02.12
 */
class TreeTablePopupTriggerMouseListener extends MouseAdapter {
    private JPopupMenu popupMenu;

    TreeTablePopupTriggerMouseListener(TreeTable treeTableOwner, ProjectContext projectContext1) {
        popupMenu = new TreeTablePopupMenu(treeTableOwner, projectContext1);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        showPopupIfPopupTriggerEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        showPopupIfPopupTriggerEvent(e);
    }

    private void showPopupIfPopupTriggerEvent(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }


    class TreeTablePopupMenu extends JPopupMenu {
        TreeTablePopupMenu(final TreeTable treeTableOwner, final ProjectContext projectContext) {
            JMenuItem jMenuItemOpenSource = new JMenuItem("Open source");
            jMenuItemOpenSource.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = treeTableOwner.getSelectedRow();
                    String methodName = treeTableOwner.getModel().getValueAt(row, 0).toString();
                    projectContext.openSourceFile(methodName);
                }
            });

            add(jMenuItemOpenSource);
        }
    }
}
