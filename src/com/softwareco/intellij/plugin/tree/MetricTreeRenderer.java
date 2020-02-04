package com.softwareco.intellij.plugin.tree;


import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class MetricTreeRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {
        tree.requestFocusInWindow();

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                true);

        if (leaf) {
            // setIcon(<leaf icon>);
        } else {
            // setIcon(<parent icon>);
        }
        tree.requestFocusInWindow();

        return this;
    }

}
