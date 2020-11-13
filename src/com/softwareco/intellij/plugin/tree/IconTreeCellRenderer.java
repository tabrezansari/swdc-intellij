package com.softwareco.intellij.plugin.tree;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;


public class IconTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, true);

        this.setBorderSelectionColor(null);

        Icon icon = null;
        if (value instanceof MetricTreeNode) {
            MetricTreeNode node = (MetricTreeNode)value;

            String iconName = node.getIconName();
            if (iconName != null) {
                icon = createImageIcon(iconName);
            }
        }

        if (icon != null) {
            setIcon(icon);
        }

        return comp;
    }

    public Icon createImageIcon(String iconName) {
        try {
            return IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/" + iconName);
        } catch (Exception e) {
            System.err.println("Couldn't find file: /com/softwareco/intellij/plugin/assets/" + iconName);
            return null;
        }
    }
}
