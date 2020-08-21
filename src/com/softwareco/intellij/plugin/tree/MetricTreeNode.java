package com.softwareco.intellij.plugin.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class MetricTreeNode extends DefaultMutableTreeNode {

    protected DefaultTreeModel model;

    private String id;
    private String iconName;
    private Object data;

    public MetricTreeNode(String nodeName, String id) {
        super(nodeName);
        this.model = null;
        this.id = id;
    }

    public void setModel(DefaultTreeModel model) {
        this.model = model;
    }

    public void add(MutableTreeNode node) {
        super.add(node);
        nodeWasAdded(this, getChildCount() - 1);
    }

    protected void nodeWasAdded(TreeNode node, int index) {
        if (model == null) {
            ((MetricTreeNode)node.getParent()).nodeWasAdded(node, index);
        }
        else {
            int[] childIndices = new int[1];
            childIndices[0] = index;
            model.nodesWereInserted(node, childIndices);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public Object getData() { return data; }

    public void setData(Object obj) {
        this.data = obj;
    }
}
