package org.ssprofiler.idea.profileplugin.command;

import org.ssprofiler.worker.SamplingWorker;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 25.05.11
 */
public class SSProfilingCommandImpl implements ProfilingCommand {
    private SamplingWorker worker;

    public void start(String filename) {
        worker = new SamplingWorker();
        worker.startSampling(50, filename);
    }

    public void stop() {
        worker.stopSampling();
    }
}
