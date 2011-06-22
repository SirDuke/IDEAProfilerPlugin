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

import org.ssprofiler.model.ThreadDump;
import org.ssprofiler.model.TimeInterval;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 11.05.11
 */
public class ThreadChartComponent extends JComponent implements TimeIntervalAxis, ThreadFilterListener {
    private final long NANO = 1000000000;

    private static final int CELL_HEIGHT = 12;
    private static final int CELL_WIDTH = 12;
    private static final int NAME_LENGTH = 300;
    private static final int CPU_TIME_LENGTH = 50;
    private static final int GAP_SOUTH = 3;
    private static final int MIN_LEGEND_LENGTH = 400;

    private Collection<ThreadDump> threadsDumps;
    private long startTime, endTime;
    private Image offscreen;
    private Rectangle chartBounds;
    private Dimension totalSize;
    private int selectedX = -1;

    public ThreadChartComponent(Collection<ThreadDump> threadsDumps, long startTime, long endTime) {
        this.threadsDumps = threadsDumps;
        this.startTime = startTime;
        this.endTime = endTime;

        int h = CELL_HEIGHT + 3;
        int chartHeight = threadsDumps.size() * CELL_HEIGHT;
        int chartWidth = Math.round((endTime - startTime) * CELL_WIDTH / NANO);
        if (chartWidth < MIN_LEGEND_LENGTH) { // min space for Legend
            chartWidth = MIN_LEGEND_LENGTH;
        }
        chartBounds = new Rectangle(NAME_LENGTH, h, chartWidth, chartHeight);

        totalSize = new Dimension(NAME_LENGTH + chartWidth + CPU_TIME_LENGTH, h + chartHeight + GAP_SOUTH);
        setPreferredSize(totalSize);
    }

    public void init(){
        offscreen = createImage(totalSize.width, totalSize.height + GAP_SOUTH);
        Graphics g = offscreen.getGraphics();
        drawChart(g);
    }

    public void selectionChanged(Collection<ThreadDump> selectedThreads) {
        threadsDumps = selectedThreads;
        repaint();
    }

    private void drawChart(Graphics g) {
        float lengthPerNanosec = (float)CELL_WIDTH / NANO;

        drawLegend(g);
        Iterator<ThreadDump> iter = threadsDumps.iterator();

        int startX = NAME_LENGTH;
        int h = CELL_HEIGHT + 3;
        Thread.State[] THREAD_STATE = Thread.State.values();

        while (iter.hasNext()) {
            ThreadDump threadDump = iter.next();
            g.setColor(Color.BLACK);
            g.drawString(threadDump.getName(), 0, h+CELL_HEIGHT);

            for (int i = 0; i < THREAD_STATE.length; i++) {
                java.util.List<TimeInterval> intervals = threadDump.getTimeIntevalsForState(THREAD_STATE[i]);
                if (!intervals.isEmpty()) {
                    Color color = getColorForState(THREAD_STATE[i]);
                    g.setColor(color);
                    for (Iterator<TimeInterval> iterator = intervals.iterator(); iterator.hasNext();) {
                        TimeInterval interval = iterator.next();
                        int x0 = startX + Math.round((interval.getStartTime() - startTime) * lengthPerNanosec);
                        int x1 = startX + Math.round((interval.getEndTime() - startTime) * lengthPerNanosec);
                        g.fillRect(x0, h, x1 - x0, CELL_HEIGHT - 1);
                    }
                }
            }

            g.setColor(Color.BLACK);
            g.drawString(Long.toString(threadDump.getStackTraceTree().getCpuTimeTotal()), NAME_LENGTH + chartBounds.width + 1 , h+CELL_HEIGHT);
            h += CELL_HEIGHT;
        }

        g.drawString("CPU Usage Time", NAME_LENGTH + chartBounds.width + 1, CELL_HEIGHT);
    }

    private void drawLegend(Graphics g) {
        int x = NAME_LENGTH;
        g.setColor(Color.BLACK);
        g.drawString("Legend:", x, CELL_HEIGHT);

        x += 50;
        g.setColor(getColorForState(Thread.State.RUNNABLE));
        g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
        g.setColor(Color.BLACK);
        x += CELL_WIDTH + 2;
        g.drawString("-Runnable", x, CELL_HEIGHT);

        x += 62;
        g.setColor(getColorForState(Thread.State.WAITING));
        g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
        g.setColor(Color.BLACK);
        x += CELL_WIDTH + 2;
        g.drawString("-Waiting", x, CELL_HEIGHT);

        x += 50;
        g.setColor(getColorForState(Thread.State.TIMED_WAITING));
        g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
        g.setColor(Color.BLACK);
        x += CELL_WIDTH + 2;
        g.drawString("-Timed_Waiting", x, CELL_HEIGHT);

        x += 93;
        g.setColor(getColorForState(Thread.State.BLOCKED));
        g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
        g.setColor(Color.BLACK);
        x += CELL_WIDTH + 2;
        g.drawString("-Blocked", x, CELL_HEIGHT);
    }

    private Color getColorForState(Thread.State state) {
        if (state == Thread.State.RUNNABLE) {
            return Color.green;
        } else if (state == Thread.State.TIMED_WAITING) {
            return Color.orange;
        } else if (state == Thread.State.WAITING) {
            return Color.yellow;
        } else if (state == Thread.State.BLOCKED) {
            return Color.red;
        }
        return Color.white;
    }

    @Override
    public void paint(Graphics g) {
        /*if (offscreen == null) {
            init();
        }
        g.drawImage(offscreen, 0, 0, null);*/
        drawChart(g);
        g.setColor(new Color(200, 200, 200));
        for (int i = chartBounds.x; i <= chartBounds.x + chartBounds.width; i+=CELL_WIDTH) {
            g.drawLine(i, chartBounds.y, i, chartBounds.y + chartBounds.height);
        }
        if (selectedX != -1) {
            g.setColor(Color.BLACK);
            g.drawLine(selectedX, chartBounds.y, selectedX, chartBounds.y + chartBounds.height);
        }
    }

    public int getStartX() {
        return chartBounds.x;
    }

    public int getEndX() {
        return chartBounds.x + chartBounds.width;
    }

    public void setSelectedX(int x) {
        selectedX = x;
        repaint();
    }
}
