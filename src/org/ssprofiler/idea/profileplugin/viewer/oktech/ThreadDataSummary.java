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

import hu.oktech.profiler.core.data.ThreadData;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 11.05.11
 */
public class ThreadDataSummary {
    private List<ThreadData> datas = new LinkedList<ThreadData>();
    private long totalCpuTime = 0;
    private Comparator<ThreadData> threadDataComparator = new ThreadDataComparator();



    public StackTraceElement[] getStackTrace(long time) {
        if (time < datas.get(0).getSystemTime()) {
            return null;
        }
        int i = 0;
        while ((i < datas.size()) && (datas.get(i).getSystemTime() < time)) {
            i++;
        }
        if (i == datas.size()) {
            return null;
        }
        return datas.get(i).getStackTrace();
    }

    public Thread.State getStatus(long time) {
        int i = 0;
        while (datas.get(i).getSystemTime() < time) {
            i++;
            if (i == datas.size()) {
                i--;
                break;
            }
        }
        return datas.get(i).getThreadState();

    }

    public long getTotalCpuTime() {
        if ((totalCpuTime == 0) && (datas.size() > 0)) {
            long initialCpuTime = datas.get(0).getCpuTime();
            long lastCputTime = datas.get(datas.size() - 1).getCpuTime();
            totalCpuTime = lastCputTime - initialCpuTime;
        }
        return totalCpuTime;
    }

    int kk = 0;
    public void add(ThreadData data) {
        kk++;
        if (kk != 5) {
            //ignore 4 stackraces out of 5 to avoid OutOfMemory
            return;
        }
        kk=0;
        if (datas.size() == 0) {
            datas.add(data);
        }  else {
            int i = 0;
            while ((i < datas.size()) && (threadDataComparator.compare(datas.get(i), data) <= 0)) {
                i++;
            }
            if (i == datas.size()) {
                datas.add(data);
            } else {
                datas.add(i, data);
            }
        }
    }

    public String getThreadName() {
        if (datas.size() == 0) {
            return null;
        }
        return datas.get(0).getThreadName();
    }

    public ThreadData[] getThreadData() {
        return datas.toArray(new ThreadData[datas.size()]);
    }

    static class ThreadDataComparator implements Comparator<ThreadData> {
        public int compare(ThreadData o1, ThreadData o2) {
            return (int)(o1.getSystemTime() - o2.getSystemTime());
        }
    }
}
