package org.ssprofiler.model;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Serduk
 * Date: 29.05.11
 */
public interface StackTraceTreeVisitor {
    /**
     * returns true if need to continue visiting children of current node
     * @param stackTraceTree
     * @return  true if need to continue visiting children of current node
     */
    public boolean visit(StackTraceTree stackTraceTree);
}
