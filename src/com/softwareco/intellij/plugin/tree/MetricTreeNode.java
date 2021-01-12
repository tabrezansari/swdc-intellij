package com.softwareco.intellij.plugin.tree;

import javax.swing.tree.*;

public class MetricTreeNode extends DefaultMutableTreeNode {

    protected DefaultTreeModel model;

    private String id;
    private String iconName;
    private Object data;
    private boolean expanded = false;
    private String label;

    public MetricTreeNode(boolean isSeparator) {
        if (isSeparator) {
            this.init("-------------------------------------------", "", "separator");
        } else {
            this.init("", "", "");
        }
    }

    public MetricTreeNode(String label, String iconName, String id) {
        this.init(label, iconName, id);
    }

    private void init(String label, String iconName, String nodeId) {
        this.allowsChildren = true;
        this.label = label;
        this.id = nodeId == null ? "" : nodeId;
        this.iconName = iconName;
        this.initModel();
    }

    public void updateLabel(String label) {
        this.label = label;
    }

    public void updateIconName(String iconName) {
        this.iconName = iconName;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    private void initModel() {
        DefaultTreeModel parentNodeModel = new DefaultTreeModel(this);
        this.setModel(parentNodeModel);
    }

    public DefaultTreeModel getModel() {
        return this.model;
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
            ((MetricTreeNode) node.getParent()).nodeWasAdded(node, index);
        } else {
            int[] childIndices = new int[1];
            childIndices[0] = index;
            model.nodesWereInserted(node, childIndices);
        }
    }

    public String getId() {
        return id;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getIconName() {
        return iconName;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object obj) {
        this.data = obj;
    }

    public TreePath getNodeTreePath() {
        TreePath p = new TreePath(model.getPathToRoot(this));
        return p;
    }

    @Override
    public String toString() {
        return label;
    }
}
