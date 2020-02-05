package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.util.IconLoader;

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

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, true);

        Icon icon = null;
        if (value instanceof MetricTreeNode) {
            String iconName = ((MetricTreeNode)value).getIconName();
            if (iconName != null) {
                icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/" + iconName);
            }
        }

        if (icon != null) {
            setIcon(icon);
        }

        tree.requestFocusInWindow();

        return this;
    }

}
