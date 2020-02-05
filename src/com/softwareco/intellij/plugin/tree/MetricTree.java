package com.softwareco.intellij.plugin.tree;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

public class MetricTree extends JTree {

    public String id;
    public boolean expandState = false;

    public MetricTree(TreeModel newModel) {
        super(newModel);
    }

    @Override
    public void setExpandedState(TreePath path, boolean state) {
        this.expandState = state;
        super.setExpandedState(path, state);
    }

    @Override
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
    }

    public boolean isExpandState() {
        return expandState;
    }

    @Override
    public TreeCellRenderer getCellRenderer() {
        return super.getCellRenderer();
    }

    public Component add(String name, String id) {
        this.id = id;
        Component comp = new Component() {
            @Override
            public void setName(String name) {
                super.setName(name);
            }
        };
        comp.setName(name);
        return super.add(comp);
    }
}
