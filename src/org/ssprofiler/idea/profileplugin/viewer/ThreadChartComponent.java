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

import com.intellij.ui.components.JBScrollPane;
import org.ssprofiler.model.ThreadDump;
import org.ssprofiler.model.TimeInterval;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 11.05.11
 */
public class ThreadChartComponent extends JPanel implements ThreadFilterListener {
    private final long NANO = 1000000000;

    private static final int CELL_HEIGHT = 12;
    private static final int CELL_WIDTH = 12;

    private Collection<ThreadDump> threadsDumps;
    private long startTime;
    private Dimension chartSize;
    private ChartComponent chart;
    private RowHeaderComponent rowHeader;
    private JBScrollPane jbScrollPane;

    public ThreadChartComponent(Collection<ThreadDump> threadsDumps, long startTime, long endTime) {
        super(new BorderLayout());
        chart = new ChartComponent();

        this.threadsDumps = threadsDumps;
        this.startTime = startTime;

        int chartHeight = threadsDumps.size() * CELL_HEIGHT;
        int chartWidth = Math.round((endTime - startTime) * CELL_WIDTH / NANO);

        chartSize = new Dimension(chartWidth, chartHeight);
        chart.setPreferredSize(chartSize);
        jbScrollPane = new JBScrollPane(chart);
        add(jbScrollPane);
        rowHeader = new RowHeaderComponent();
        rowHeader.setPreferredSize(new Dimension(rowHeader.estimateMaxThreadNameWidth(), chartHeight));
        jbScrollPane.setRowHeaderView(rowHeader);

        ColumnHeaderViewport columnHeader = new ColumnHeaderViewport();
        columnHeader.setView(new ColumnHeaderComponent(chartWidth));
        columnHeader.setPreferredSize(new Dimension(chartWidth, 2 * CELL_HEIGHT + 2));
        jbScrollPane.setColumnHeader(columnHeader);
    }

    public void selectionChanged(Collection<ThreadDump> selectedThreads) {
        threadsDumps = selectedThreads;
        chartSize = new Dimension(chartSize.width, threadsDumps.size() * CELL_HEIGHT);
        chart.setPreferredSize(chartSize);
        Dimension rowHeaderSize = new Dimension(rowHeader.estimateMaxThreadNameWidth(), chartSize.height);
        rowHeader.setPreferredSize(rowHeaderSize);
        jbScrollPane.getRowHeader().setViewSize(rowHeaderSize);
        jbScrollPane.revalidate();
        repaint();
    }

    public void addChartMouseListener(MouseListener listener) {
        chart.addMouseListener(listener);
    }

    public void removeChartMouseListener(MouseListener listener) {
        chart.removeMouseListener(listener);
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

    class ChartComponent extends JComponent implements TimeIntervalAxis{
        private int selectedX;

        @Override
        public void paint(Graphics g) {
            drawChart(g);
            if (selectedX != -1) {
                g.setColor(Color.BLACK);
                g.drawLine(selectedX, 0, selectedX, chartSize.height);
            }
        }

        private void drawChart(Graphics g) {
            float lengthPerNanosec = (float)CELL_WIDTH / NANO;

            Iterator<ThreadDump> iter = threadsDumps.iterator();

            int startX = 0;
            int h = 0;
            Thread.State[] THREAD_STATE = Thread.State.values();

            while (iter.hasNext()) {
                ThreadDump threadDump = iter.next();

                for (Thread.State state : THREAD_STATE) {
                    java.util.List<TimeInterval> intervals = threadDump.getTimeIntevalsForState(state);
                    if (!intervals.isEmpty()) {
                        Color color = getColorForState(state);
                        g.setColor(color);
                        for (TimeInterval interval : intervals) {
                            int x0 = startX + Math.round((interval.getStartTime() - startTime) * lengthPerNanosec);
                            int x1 = startX + Math.round((interval.getEndTime() - startTime) * lengthPerNanosec);
                            g.fillRect(x0, h, x1 - x0, CELL_HEIGHT - 1);
                        }
                    }
                }
                h += CELL_HEIGHT;
            }
        }

        public int getStartX() {
            return 0;
        }

        public int getEndX() {
            return chartSize.width;
        }

        public void setSelectedX(int x) {
            selectedX = x;
            repaint();
        }
    }

    class RowHeaderComponent extends JComponent {
        private final int START_X = 5;
        @Override
        public void paint(Graphics g) {
            Iterator<ThreadDump> iter = threadsDumps.iterator();
            int h = 0;
            while (iter.hasNext()) {
                ThreadDump threadDump = iter.next();
                g.setColor(Color.BLACK);
                g.drawString(threadDump.getName(), START_X, h+CELL_HEIGHT);
                h+=CELL_HEIGHT;
            }
        }

            //estimates max thread name width when it is painted on the screen
        int estimateMaxThreadNameWidth() {
            Font font = UIManager.getFont("Panel.font");
            FontMetrics fontMetrics = getFontMetrics(font);
            int maxWidth = 0;
            for (ThreadDump threadDump : threadsDumps) {
                int width = fontMetrics.stringWidth(threadDump.getName());
                if (width > maxWidth) maxWidth = width;
            }
            return maxWidth + 2*START_X; //one "START_X at the start and one after the end, between thread names and chart
        }
    }

    class ColumnHeaderViewport extends JViewport {
        @Override
        public void paint(Graphics g) {
            drawLegend(g);
            g.translate(0, CELL_HEIGHT);
            super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.
        }

        private void drawLegend(Graphics g) {
            Font font = g.getFont();
            g.setFont(font.deriveFont((float) font.getSize() * 0.95f));
            int x = 3;

            g.setColor(getColorForState(Thread.State.RUNNABLE));
            g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
            g.setColor(Color.BLACK);
            x += CELL_WIDTH + 2;
            g.drawString("-Runnable", x, CELL_HEIGHT-2);

            x += 62;
            g.setColor(getColorForState(Thread.State.WAITING));
            g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
            g.setColor(Color.BLACK);
            x += CELL_WIDTH + 2;
            g.drawString("-Waiting", x, CELL_HEIGHT-2);

            x += 50;
            g.setColor(getColorForState(Thread.State.TIMED_WAITING));
            g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
            g.setColor(Color.BLACK);
            x += CELL_WIDTH + 2;
            g.drawString("-Timed_Waiting", x, CELL_HEIGHT-2);

            x += 93;
            g.setColor(getColorForState(Thread.State.BLOCKED));
            g.fillRect(x, 0, CELL_WIDTH, CELL_HEIGHT);
            g.setColor(Color.BLACK);
            x += CELL_WIDTH + 2;
            g.drawString("-Blocked", x, CELL_HEIGHT-2);
        }
    }

    class ColumnHeaderComponent extends JComponent {
        private int width;
        private int height;

        ColumnHeaderComponent(int width) {
            this.width = width;
            this.height = CELL_HEIGHT + 2;
            setPreferredSize(new Dimension(width, height));
        }

        public int getHeight() {
            return height;
        }

        @Override
        public void paint(Graphics g) {
            Font font = g.getFont();
            g.setFont(font.deriveFont((float) font.getSize() * 0.9f));
            FontMetrics fontMetrics = g.getFontMetrics();
            int x = CELL_WIDTH;
            int h = 0;
            int m = 0;
            int s = 0;
            while (x <= width) {
                for (int i = 0; i < 9 && x <=width; i++) {
                    g.drawLine(x, height, x, height - 2);
                    x += CELL_WIDTH;
                    s++;
                }
                s++;
                StringBuilder timeString = new StringBuilder();
                if (s == 60) {
                    m++;
                    s = 0;
                    if (m == 60) {
                        h++;
                        m = 0;
                    }
                    if (h > 0) {
                        timeString.append(h).append('h');
                    }
                    timeString.append(m).append('m');
                }
                if (x < width) {
                    g.drawLine(x, height, x, height - 3);

                    if (s > 0) timeString.append(s).append('s');
                    String ts = timeString.toString();
                    int width = fontMetrics.stringWidth(ts);
                    g.drawString(ts, x - width / 2, height - 4);
                    x += CELL_WIDTH;
                }
            }
        }
    }
}
