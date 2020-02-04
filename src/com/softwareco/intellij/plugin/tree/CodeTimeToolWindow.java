package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class CodeTimeToolWindow {

    public static final Logger LOG = Logger.getLogger("CodeTimeToolWindow");

    private JPanel codetimeWindowContent;
    private JScrollPane scrollPane;
    private JPanel dataPanel;

    private static CodeTimeToolWindow win;

    public static void refresh() {
        if (win != null) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    win.rebuildTreeView();
                }
            });
        }
    }

    public CodeTimeToolWindow(ToolWindow toolWindow) {
        codetimeWindowContent.setFocusable(true);

        this.rebuildTreeView();

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        codetimeWindowContent.setBackground((Color) null);

        win = this;
    }

    /**
     * Build the Tree
     */
    private synchronized void rebuildTreeView() {
        TreeItemBuilder.initializeSessionSummary();

        dataPanel.removeAll();
        dataPanel.setBackground((Color) null);
        dataPanel.setFocusable(true);

        JBList<JLabel> labels = TreeItemBuilder.buildCodeTimeLabels();

        // add the top labels
        dataPanel.add(labels, gridConstraints(dataPanel.getComponentCount(), 1, 2, 0, 3, 0));

        JSeparator separator = TreeItemBuilder.getSeparator();
        // add a separator
        dataPanel.add(separator, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 1, 0));

        // add the metric nodes
        MetricTree editorTime = TreeItemBuilder.buildEditorTimeTree();
        dataPanel.add(editorTime, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        MetricTree codeTime = TreeItemBuilder.buildCodeTimeTree();
        dataPanel.add(codeTime, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        MetricTree linesAdded = TreeItemBuilder.buildLinesAddedTree();
        dataPanel.add(linesAdded, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        MetricTree linesRemoved = TreeItemBuilder.buildLinesRemovedTree();
        dataPanel.add(linesRemoved, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        MetricTree keystrokes = TreeItemBuilder.buildKeystrokesTree();
        dataPanel.add(keystrokes, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        dataPanel.updateUI();
        dataPanel.setVisible(true);

        scrollPane.setFocusable(true);
        scrollPane.setVisible(true);

        codetimeWindowContent.updateUI();
        codetimeWindowContent.setFocusable(true);
        codetimeWindowContent.setVisible(true);
    }

    private GridConstraints gridConstraints(int row, int vSize, int hSize, int anchor, int fill, int indent) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(0);
        constraints.setRowSpan(1);
        constraints.setColSpan(1);
        constraints.setVSizePolicy(vSize);
        constraints.setHSizePolicy(hSize);
        constraints.setAnchor(anchor);
        constraints.setFill(fill);
        constraints.setIndent(indent);
        constraints.setUseParentLayout(false);

        return constraints;
    }

    public JPanel getContent() {
        return codetimeWindowContent;
    }

}
