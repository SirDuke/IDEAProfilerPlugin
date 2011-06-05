package org.ssprofiler.worker;

import org.ssprofiler.model.MemoryInfo;
import org.ssprofiler.model.ThreadDump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 25.05.11
 */
public class SamplingWorker{
    private static final long MEMORY_SAMPLING_INTERVAL = 1000;
    private volatile boolean stopped;
    private volatile String filename;
    private volatile ObjectOutputStream objectOutputStream;
    private Object writerLock = new Object();

    private Thread threadDumper;
    private Thread memoryDumper;

    public void startSampling(final long samplingInterval, String filename) {
        this.filename = filename;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename));

            threadDumper = new Thread(new Runnable() {
                public void run() {
                    runThreadDumping(samplingInterval);
                }
            });

            memoryDumper = new Thread(new Runnable() {
                public void run() {
                    runMemoryDumping(MEMORY_SAMPLING_INTERVAL);
                }
            });
            threadDumper.start();
            memoryDumper.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopSampling() {
        stopped = true;

        try {
            memoryDumper.join();
            threadDumper.join();
            memoryDumper = null;
            threadDumper = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runThreadDumping(long samplingInterval) {
        Map<Long, ThreadDump> threads = new HashMap<Long, ThreadDump>();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadCpuTimeEnabled(true);
        while (!stopped) {
            long time = System.nanoTime();
            ThreadInfo[] threadInfo = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), Integer.MAX_VALUE);
            for (int i = 0; i < threadInfo.length; i++) {
                ThreadInfo info = threadInfo[i];
                if (info != null) {
                    long cpuTime = threadMXBean.getThreadCpuTime(info.getThreadId());
                    ThreadDump threadDump = threads.get(info.getThreadId());
                    if (threadDump == null) {
                        threadDump = new ThreadDump(info, time, cpuTime);
                        threads.put(info.getThreadId(), threadDump);
                    }

                    threadDump.addThreadDump(info, time, cpuTime);
                }
            }
            try {
                Thread.sleep(samplingInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        synchronized (writerLock) {
            try {
                objectOutputStream.writeObject(threads);
                objectOutputStream.flush();
                objectOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runMemoryDumping(long memorySampingInterval) {
        synchronized (writerLock) {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            try {
                while (!stopped) {
                    long systemTime = System.nanoTime();
                    long usedHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
                    long usedNonHeap = memoryMXBean.getNonHeapMemoryUsage().getUsed();
                    MemoryInfo  memoryInfo = new MemoryInfo(systemTime, usedHeap, usedNonHeap);

                    objectOutputStream.writeObject(memoryInfo);
                    objectOutputStream.flush();
                    try {
                        Thread.sleep(memorySampingInterval - (System.nanoTime() - systemTime)/1000000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SamplingWorker worker = new SamplingWorker();
        worker.startSampling(50, System.getProperty("user.home") + File.separator + "cpu.cpu");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        worker.stopSampling();
    }
}
