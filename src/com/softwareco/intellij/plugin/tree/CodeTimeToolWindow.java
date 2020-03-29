package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CodeTimeToolWindow {

    public static final Logger LOG = Logger.getLogger("CodeTimeToolWindow");

    private JPanel codetimeWindowContent;
    private JScrollPane scrollPane;
    private JPanel dataPanel;

    private static Map<String, List<ExpandState>> expandStateMap = new HashMap<>();

    private static CodeTimeToolWindow win;
    private static boolean refreshing = false;

    public CodeTimeToolWindow(ToolWindow toolWindow) {
        codetimeWindowContent.setFocusable(true);

        System.out.println("initializing the tool window");

        this.rebuildTreeView(true);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        codetimeWindowContent.setBackground((Color) null);

        win = this;
    }

    public static class ExpandState {
        public boolean expand = false;
        public TreePath path = null;

        public ExpandState(boolean expand, TreePath path) {
            this.expand = expand;
            this.path = path;
        }
    }

    public static void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;

        if (win != null) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    win.rebuildTreeView(false);
                }
            });
        }
        refreshing = false;
    }

    public static void updateExpandState(String id, TreePath path, boolean expanded) {
        ExpandState state = new ExpandState(expanded, path);
        List<ExpandState> existingStates = expandStateMap.get(id);
        if (existingStates == null) {
            existingStates = new ArrayList<>();
            existingStates.add(state);
        } else {
            boolean foundExisting = false;
            for (ExpandState s : existingStates) {
                String pathStr = s.path.toString();
                String tPathStr = path.toString();
                if (pathStr.equals(tPathStr)) {
                    s.expand = expanded;
                    foundExisting = true;
                    break;
                }
            }
            if (!foundExisting) {
                existingStates.add(state);
            }
        }

        expandStateMap.put(id, existingStates);
    }

    public static List<ExpandState> getExpandState(String id) {
        return expandStateMap.get(id);
    }

    /**
     * Build the Tree
     */
    private synchronized void rebuildTreeView(boolean initializing) {
        TreeItemBuilder.initializeSessionSummary(initializing);

        // get vspace component to add at the end
        Component component = dataPanel.getComponent(dataPanel.getComponentCount() - 1);

        dataPanel.removeAll();
        dataPanel.setBackground((Color) null);

        JBList<JLabel> signupLabels = TreeItemBuilder.buildSignupLabels();
        dataPanel.add(signupLabels, gridConstraints(dataPanel.getComponentCount(), 1, 2, 0, 3, 0));

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

        MetricTree topKpmFiles = TreeItemBuilder.buildTopKpmFilesTree();
        if (topKpmFiles != null) {
            dataPanel.add(topKpmFiles, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));
        }

        MetricTree topKeystrokesFiles = TreeItemBuilder.buildTopKeystrokesFilesTree();
        if (topKeystrokesFiles != null) {
            dataPanel.add(topKeystrokesFiles, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));
        }

        MetricTree topCodetimeFiles = TreeItemBuilder.buildTopCodeTimeFilesTree();
        if (topCodetimeFiles != null) {
            dataPanel.add(topCodetimeFiles, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));
        }

        // add the commit info
        MetricTree openChangesTree = TreeItemBuilder.buildGitTree("Open changes", "uncommitted");
        MetricTree committedChangesTree = TreeItemBuilder.buildGitTree("Committed today", "committed");

        dataPanel.add(openChangesTree, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));
        dataPanel.add(committedChangesTree, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));

        // add a separator
        separator = TreeItemBuilder.getSeparator();
        // add a separator
        dataPanel.add(separator, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 1, 0));

        JBList<JLabel> contributorTitle = TreeItemBuilder.buildContributorTitle();
        if (contributorTitle != null) {
            dataPanel.add(contributorTitle, gridConstraints(dataPanel.getComponentCount(), 1, 2, 0, 3, 0));
            // get the contributors
            List<MetricTree> mTreeItems = TreeItemBuilder.buildContributorUsers();
            if (mTreeItems != null && mTreeItems.size() > 0) {
                for (MetricTree mTree : mTreeItems) {
                    dataPanel.add(mTree, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 3, 0));
                }
            }
        }

        // Add VSpacer at last
        dataPanel.add(component, gridConstraints(dataPanel.getComponentCount(), 6, 1, 0, 2, 0));

        dataPanel.updateUI();
        dataPanel.setVisible(true);

        scrollPane.setVisible(true);

        codetimeWindowContent.updateUI();
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
