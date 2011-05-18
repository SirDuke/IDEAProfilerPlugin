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

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 11.05.11
 */
public class ThreadChartComponent extends JComponent implements TimeIntervalAxis{
    private final int CELL_HEIGHT = 12;
    private final int CELL_WIDTH = 12;
    private final int NAME_LENGTH = 300;

    private Map<Long, ThreadDataSummary> threadsData;
    private long startTime, endTime;
    private Image offscreen;
    private Rectangle chartBounds;
    private int selectedX = -1;

    public ThreadChartComponent(Map<Long, ThreadDataSummary> threadsData, long startTime, long endTime) {
        this.threadsData = threadsData;
        this.startTime = startTime;
        this.endTime = endTime;

        int chartHeight = 2*CELL_HEIGHT + threadsData.size() * CELL_HEIGHT;
        int chartWidth = NAME_LENGTH + (CELL_WIDTH * (int)((endTime - startTime)/1000)) + 500;
        setPreferredSize(new Dimension(chartWidth, chartHeight));
        setSize(chartWidth, chartHeight);
    }

    public void init() {
        int h = CELL_HEIGHT + 3;
        int chartHeight = h + threadsData.size() * CELL_HEIGHT + 5;
        float lengthPerMsec = (float)CELL_WIDTH / 1000;
        int chartWidth = Math.round(lengthPerMsec * (endTime - startTime));
        if (chartWidth < 400) { // min space for Legend
            chartWidth = 400;
        }
        int imageWidth = NAME_LENGTH + chartWidth + 100;
        offscreen = createImage(imageWidth, chartHeight);
        //offscreen = new BufferedImage(chartWidth, chartHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = offscreen.getGraphics();
        drawLegend(g);
        Iterator<Map.Entry<Long, ThreadDataSummary>> iter = threadsData.entrySet().iterator();

        int startX = NAME_LENGTH;

        long totalTime = endTime - startTime;
        int endX = startX;

        while (iter.hasNext()) {
            ThreadDataSummary tds = iter.next().getValue();
            g.setColor(Color.BLACK);
            g.drawString(tds.getThreadName(), 0, h+CELL_HEIGHT);


            ThreadData[] data = tds.getThreadData();
            int x = 0;
            if (data.length > 0) {
                long time = data[0].getSystemTime();
                x = startX + Math.round (((time - startTime) * lengthPerMsec));
                g.setColor(Color.blue);
                g.drawLine(x, h, x, h + CELL_HEIGHT);
                for (int i = 1; i < data.length; i++) {
                    long newTime = data[i].getSystemTime();
                    int newLength = startX + Math.round ((newTime - startTime) * lengthPerMsec) - x;
                    Color c = getColorForState(data[i].getThreadState());
                    g.setColor(c);
                    g.fillRect(x, h, newLength, CELL_HEIGHT);
                    x+= newLength;
                    time = newTime;
                }
                if (x > endX) endX = x;
            }

            g.setColor(Color.BLACK);
            g.drawString(Float.toString(((float)tds.getTotalCpuTime())/1000000000), NAME_LENGTH + chartWidth + 1 , h+CELL_HEIGHT);
            h += CELL_HEIGHT;
        }

        g.drawString("CPU Usage Time", NAME_LENGTH + chartWidth + 1, CELL_HEIGHT);

        chartBounds = new Rectangle(startX, 0, endX - startX, h - CELL_HEIGHT);
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
        if (offscreen == null) {
            init();
        }
        g.drawImage(offscreen, 0, 0, null);
        if (selectedX != -1) {
            g.setColor(Color.BLACK);
            g.drawLine(selectedX, CELL_HEIGHT + 3, selectedX, chartBounds.y + chartBounds.height);
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
