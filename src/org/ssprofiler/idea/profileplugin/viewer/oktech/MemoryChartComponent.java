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

import hu.oktech.profiler.core.data.MemoryData;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 13.05.11
 */
public class MemoryChartComponent extends JComponent implements TimeIntervalAxis{
    private static int CELL_WIDTH = 10;
    private static int CHART_HEIGHT = 300;
    private static int GAP_NORTH = 40;
    private static int GAP_WEST = 30;
    private static int GAP_SOUTH = 30;
    private static int GAP_EAST = 30;

    private List<MemoryData> memoryData;
    private Image bufferImage;
    private int startX, endX, selectedX=-1;
    private int width, height;

    public MemoryChartComponent(List<MemoryData> memoryData) {
        this.memoryData = memoryData;
        width = GAP_WEST + GAP_EAST + (int)(CELL_WIDTH * (memoryData.get(memoryData.size() - 1).getSystemTime() - memoryData.get(0).getSystemTime()) / 1000);
        if (width < GAP_WEST + GAP_EAST + 400) { //leave space for legend
            width = GAP_WEST + GAP_EAST + 400;
        }
        height = CHART_HEIGHT + GAP_NORTH + GAP_SOUTH;
        setPreferredSize(new Dimension(width, height));
    }

    public void init() {
        long maxValue = 0;

        for (Iterator<MemoryData> iterator = memoryData.iterator(); iterator.hasNext();) {
            MemoryData data = iterator.next();
            if (data.getUsedHeap() > maxValue) maxValue = data.getUsedHeap();
            if (data.getUsedNonHeap() > maxValue) maxValue = data.getUsedNonHeap();
        }

        float koef = (float) CHART_HEIGHT / maxValue;
        MemoryData data = memoryData.get(0);
        long startTime = data.getSystemTime();
        int x = GAP_EAST;
        startX = x;
        int yUsedHeap = CHART_HEIGHT - Math.round(koef * data.getUsedHeap()) + GAP_NORTH;
        int yUsedNonHeap = CHART_HEIGHT - Math.round(koef * data.getUsedNonHeap()) + GAP_NORTH;


        bufferImage = createImage(width, height);
        Graphics g = bufferImage.getGraphics();

        Color colorUsedHeap = Color.red;
        Color colorUsedNonHeap = Color.blue;
        for (int i = 1; i < memoryData.size(); i++) {
            data = memoryData.get(i);
            int x1 = GAP_EAST + (int) (CELL_WIDTH * (data.getSystemTime() - startTime)/1000);
            int yUsedHeap1 = CHART_HEIGHT - Math.round(koef * data.getUsedHeap()) + GAP_NORTH;
            int yUsedNonHeap1 = CHART_HEIGHT - Math.round(koef * data.getUsedNonHeap()) + GAP_NORTH;
            g.setColor(colorUsedHeap);
            g.drawLine(x, yUsedHeap, x1, yUsedHeap1);
            g.setColor(colorUsedNonHeap);
            g.drawLine(x, yUsedNonHeap, x1, yUsedNonHeap1);

            x = x1;
            yUsedHeap = yUsedHeap1;
            yUsedNonHeap = yUsedNonHeap1;
        }
        endX = x;
        drawAxis(g, maxValue);
        drawLegend(g);
    }

    private void drawAxis(Graphics g, long maxMemory) {
        g.setColor(Color.BLACK);

        //vertical axis
        g.drawLine(GAP_EAST, GAP_NORTH + CHART_HEIGHT, GAP_EAST, GAP_NORTH);
        g.drawLine(GAP_EAST, GAP_NORTH, GAP_EAST + 5, GAP_NORTH);
        g.drawString(Float.toString(((float)maxMemory / 1024 / 1024) ) + " Mb", GAP_EAST + 7, GAP_NORTH);

        //horizontal axis
        g.drawLine(GAP_EAST, GAP_NORTH + CHART_HEIGHT, endX, GAP_NORTH + CHART_HEIGHT);
        g.drawLine(endX, GAP_NORTH + CHART_HEIGHT, endX - 5, GAP_NORTH + CHART_HEIGHT - 3);
        g.drawLine(endX, GAP_NORTH + CHART_HEIGHT, endX - 5, GAP_NORTH + CHART_HEIGHT + 3);

    }

    private void drawLegend(Graphics g) {
        int x = GAP_EAST;
        int y = 10;
        g.setColor(Color.BLACK);
        g.drawString("Legend:", x, y + 5);

        x+= 50;
        g.setColor(Color.RED);
        g.drawLine(x, y, x + 20, y);
        x+=22;
        g.setColor(Color.BLACK);
        g.drawString("Used Heap memory", x, y + 5);

        x+= 170;
        g.setColor(Color.BLUE);
        g.drawLine(x, y, x + 20, y);
        x+=22;
        g.setColor(Color.BLACK);
        g.drawString("Used Non-Heap memory", x, y+5);
    }

    @Override
    public void paint(Graphics g) {
        if (bufferImage == null) {
            init();
        }
        g.drawImage(bufferImage, 0, 0, null);
        if (selectedX != -1) {
            g.setColor(Color.BLACK);
            g.drawLine(selectedX, GAP_NORTH, selectedX, CHART_HEIGHT);
        }
    }

    public int getStartX() {
        return startX;
    }

    public int getEndX() {
        return endX;
    }

    public void setSelectedX(int x) {
        selectedX = x;
    }
}
