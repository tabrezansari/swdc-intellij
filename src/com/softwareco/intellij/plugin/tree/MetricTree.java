package com.softwareco.intellij.plugin.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricTree extends JTree {

    public static final Logger LOG = Logger.getLogger("MetricTree");

    public String id;
    public boolean expandState = false;

    public MetricTree(TreeModel model ) {
        super(model);

        this.setScrollsOnExpand(true);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e != null && e instanceof MouseEvent && e.getSource() != null && e.getSource() instanceof MetricTree) {
                    try {
                        final MetricTree tree = (MetricTree) e.getSource();
                        if (tree != null) {

                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                            if (node == null) {
                                return;
                            }

                            if (node instanceof MetricTreeNode) {
                                TreeHelper.handleClickEvent((MetricTreeNode) node);
                            }

                            SwingUtilities.invokeLater(() -> {
                                try {
                                    Thread.sleep(750);
                                    if (tree != null) {
                                        tree.clearSelection();
                                    }
                                } catch (Exception ex) {
                                    System.err.println(ex);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Tree mouse click error: {0}", ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        });
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
