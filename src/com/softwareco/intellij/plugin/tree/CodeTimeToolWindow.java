package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CodeTimeToolWindow {

    public static final Logger LOG = Logger.getLogger("CodeTimeToolWindow");

    private JPanel codetimeWindowContent;
    private JScrollPane scrollPane;
    private JPanel dataPanel;

    private static Map<String, Boolean> expandStateMap = new HashMap<>();

    private static CodeTimeToolWindow win;

    public CodeTimeToolWindow(ToolWindow toolWindow) {
        codetimeWindowContent.setFocusable(true);

        this.rebuildTreeView();

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        codetimeWindowContent.setBackground((Color) null);

        win = this;
    }

    public static void refresh() {
        if (win != null) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    win.rebuildTreeView();
                }
            });
        }
    }

    public static void updateExpandState(String id, Boolean expandState) {
        expandStateMap.put(id, expandState);
    }

    public static boolean getExpandState(String id) {
        Boolean expandState = expandStateMap.get(id);
        if (expandState == null) {
            return true;
        }
        return expandState.booleanValue();
    }

    /**
     * Build the Tree
     */
    private synchronized void rebuildTreeView() {
        TreeItemBuilder.initializeSessionSummary();

        // get vspace component to add at the end
        Component component = dataPanel.getComponent(dataPanel.getComponentCount() - 1);

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
        // editor time
        MetricTree editorTime = TreeItemBuilder.buildEditorTimeTree();
        dataPanel.add(editorTime, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // code time
        MetricTree codeTime = TreeItemBuilder.buildCodeTimeTree();
        dataPanel.add(codeTime, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // lines added
        MetricTree linesAdded = TreeItemBuilder.buildLinesAddedTree();
        dataPanel.add(linesAdded, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // lines removed
        MetricTree linesRemoved = TreeItemBuilder.buildLinesRemovedTree();
        dataPanel.add(linesRemoved, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // keystrokes
        MetricTree keystrokes = TreeItemBuilder.buildKeystrokesTree();
        dataPanel.add(keystrokes, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // add a separator
        separator = TreeItemBuilder.getSeparator();
        // add a separator
        dataPanel.add(separator, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 1, 0));

        // add the commit info
        MetricTree openChangesTree = TreeItemBuilder.buildOpenGitChanges();
        MetricTree committedChangesTree = TreeItemBuilder.buildCommittedGitChanges();
        dataPanel.add(openChangesTree, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));
        dataPanel.add(committedChangesTree, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // Add VSpacer at last
        dataPanel.add(component, gridConstraints(dataPanel.getComponentCount(), 6, 1, 0, 2, 0));

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
