package org.ssprofiler.idea.profileplugin.viewer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 22.06.11
 */
public class ChartMouseListenter extends MouseAdapter {
    private long minTime, maxTime;
    private ThreadDumpsTree threadDumpsTree;

    ChartMouseListenter(ThreadDumpsTree threadDumpsTree, long minTime, long maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.threadDumpsTree = threadDumpsTree;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point point = e.getPoint();
        TimeIntervalAxis tia = (TimeIntervalAxis) e.getComponent();
        int x = point.x;
        if ((tia.getStartX() <= x) && (x <= tia.getEndX())) {
            tia.setSelectedX(x);
            e.getComponent().repaint();
            double k = ((double)x - tia.getStartX()) / ((double)tia.getEndX() - tia.getStartX());
            threadDumpsTree.setTimeStamp(minTime + (long) (k * (maxTime - minTime)));

        }

    }
}
