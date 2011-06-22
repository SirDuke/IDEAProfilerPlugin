package org.ssprofiler.idea.profileplugin.viewer;

import org.ssprofiler.model.ThreadDump;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public interface ThreadFilterListener {
    public void selectionChanged(Collection<ThreadDump> selectedThreads);
}
