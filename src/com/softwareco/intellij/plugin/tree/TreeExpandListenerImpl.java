package com.softwareco.intellij.plugin.tree;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;

public class TreeExpandListenerImpl implements TreeExpansionListener {

    private String id = "";
    public TreeExpandListenerImpl(String id) {
        this.id = id;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        CodeTimeToolWindow.updateExpandState(id, true);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        CodeTimeToolWindow.updateExpandState(id, true);
    }
}
