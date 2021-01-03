package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.managers.SessionDataManager;
import com.softwareco.intellij.plugin.managers.TimeDataManager;
import com.softwareco.intellij.plugin.models.CodeTimeSummary;
import com.softwareco.intellij.plugin.models.SessionSummary;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;

import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CodeTimeToolWindow {

    public static final Logger LOG = Logger.getLogger("CodeTimeToolWindow");

    private JPanel codetimeWindowContent;
    private JScrollPane scrollPane;
    private static MetricTree metricTree;
    private JPanel dataPanel;
    private static boolean refreshingTree = false;

    private static CodeTimeToolWindow win;

    private static boolean expandInitialized = false;
    private static Map<String, MetricTreeNode> nodeMap = new HashMap<>();

    public CodeTimeToolWindow(ToolWindow toolWindow) {
        codetimeWindowContent.setFocusable(true);

        System.out.println("initializing the tool window");

        this.init();

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        codetimeWindowContent.setBackground(null);

        win = this;

        // check if we need to refresh the tree data based on a new day
        SessionDataManager.treeDataUpdateCheck(false);
    }

    public static void refresh() {
        if (win != null && !refreshingTree) {
            refreshingTree = true;
            try {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            win.rebuildTreeView();
                        } catch (Exception e) {
                            //
                        } finally {
                            refreshingTree = false;
                        }
                    }
                });
            } catch (Exception e) {
                refreshingTree = false;
            }
        }
    }

    public static void rebuildTree() {
        if (win != null && !refreshingTree) {
            refreshingTree = true;
            try {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            win.init();
                        } catch (Exception e) {
                            //
                        } finally {
                            refreshingTree = false;
                        }
                    }
                });
            } catch (Exception e) {
                refreshingTree = false;
            }
        }
    }

    /**
     * Build the Tree
     */
    private synchronized void init() {
        metricTree = buildCodeTimeTreeView();

        if (!expandInitialized) {
            int activeCodeTimeParentRow = findParentNodeRowById(TreeHelper.ACTIVE_CODETIME_PARENT_ID);
            if (activeCodeTimeParentRow != -1) {
                metricTree.expandRow(activeCodeTimeParentRow);
            }
            int codeTimeParentRow = findParentNodeRowById(TreeHelper.CODETIME_PARENT_ID);
            if (codeTimeParentRow != -1) {
                metricTree.expandRow(codeTimeParentRow);
            }
            int loggedInParentRow = findParentNodeRowById(TreeHelper.LOGGED_IN_ID);
            if (loggedInParentRow != -1) {
                metricTree.expandRow(loggedInParentRow);
            }
            expandInitialized = true;
        }

        scrollPane.setViewportView(metricTree);
        scrollPane.setVisible(true);

        codetimeWindowContent.updateUI();
        codetimeWindowContent.setVisible(true);
    }

    public static void rebuildTreeView() {
        rebuildMenuNodes();

        rebuildFlowNodes();

        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();

        updateMetrics(codeTimeSummary, sessionSummary);
    }

    public JPanel getContent() {
        return codetimeWindowContent;
    }

    private MetricTree buildCodeTimeTreeView() {
        MetricTree tree = new MetricTree(makeCodetimeTreeModel());

        tree.setCellRenderer(new IconTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        return tree;
    }

    private TreeModel makeCodetimeTreeModel() {
        // "Root" will not be visible
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        List<MetricTreeNode> loginNodes = TreeHelper.buildSignupNodes();
        loginNodes.forEach(node -> {
            root.add(node);
        });

        List<MetricTreeNode> menuNodes = TreeHelper.buildMenuNodes();
        menuNodes.forEach(node -> {
            root.add(node);
        });

        root.add(new MetricTreeNode(true /*isSeparator*/));

        List<MetricTreeNode> flowNodes = TreeHelper.buildTreeFlowNodes();
        flowNodes.forEach(node -> {
            root.add(node);
        });

        root.add(new MetricTreeNode(true /*isSeparator*/));

        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();

        MetricLabels mLabels = new MetricLabels();
        mLabels.updateLabels(codeTimeSummary, sessionSummary);

        root.add(TreeHelper.buildCodeTimeTree(mLabels));
        root.add(TreeHelper.buildActiveCodeTimeTree(mLabels));
        root.add(TreeHelper.buildLinesAddedTree(mLabels));
        root.add(TreeHelper.buildLinesRemovedTree(mLabels));
        root.add(TreeHelper.buildKeystrokesTree(mLabels));

        root.add(TreeHelper.buildSummaryButton());
        root.add(TreeHelper.buildViewWebDashboardButton());

        return new DefaultTreeModel(root);
    }

    public static void updateMetrics(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        if (metricTree != null) {

            MetricLabels mLabels = new MetricLabels();
            mLabels.updateLabels(codeTimeSummary, sessionSummary);

            if (codeTimeSummary != null && sessionSummary != null) {
                updateNodeLabel(findNodeById(TreeHelper.ACTIVE_CODETIME_TODAY_ID), mLabels.activeCodeTime);

                updateNodeLabel(findNodeById(TreeHelper.CODETIME_TODAY_ID), mLabels.codeTime);
            }

            if (sessionSummary != null) {
                // all of the other metrics can be updated
                // LINES DELETED
                updateNodeLabel(findNodeById(TreeHelper.LINES_DELETED_TODAY_ID), mLabels.linesRemoved);

                // LINES ADDED
                updateNodeLabel(findNodeById(TreeHelper.LINES_ADDED_TODAY_ID), mLabels.linesAdded);

                // KEYSTROKES
                updateNodeLabel(findNodeById(TreeHelper.KEYSTROKES_TODAY_ID), mLabels.keystrokes);
            }

            metricTree.updateUI();
        }
    }

    private static void updateNodeLabel(MetricTreeNode node, String label) {
        updateNodeLabel(node, label, null);
    }

    private static void updateNodeLabel(MetricTreeNode node, String label, String iconName) {
        if (node != null) {
            if (iconName != null) {
                node.updateIconName(iconName);
            }
            node.updateLabel(label);
        }
    }

    public static void expandCollapse(String id) {
        int row = 0;
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();

            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            if (!node.isExpanded()) {
                                metricTree.expandRow(row);
                                node.setExpanded(true);
                            } else {
                                metricTree.collapseRow(row);
                                node.setExpanded(false);
                            }
                            break;
                        }
                        row++;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Find node by ID error: {0}", e.toString());
        }
    }

    private static int findParentNodeRowById(String id) {
        int row = 0;
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();

            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            return row;
                        }
                        row++;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Find node by ID error: {0}", e.toString());
        }

        return -1;
    }

    private static void removeNodeById(String id) {
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();

            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            treeNode.remove(node);
                            return;
                        } else if (node != null && node.getChildCount() > 0) {
                            // check its children
                            for (int i = 0; i < node.getChildCount(); i++) {
                                MetricTreeNode childNode = (MetricTreeNode) node.getChildAt(i);
                                if (childNode != null && childNode.getId().equals(id)) {
                                    treeNode.remove(childNode);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Remove node by ID error: {0}", e.toString());
        }
    }

    private static MetricTreeNode findNodeById(String id) {
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();

            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            return node;
                        } else if (node != null && node.getChildCount() > 0) {
                            // check its children
                            for (int i = 0; i < node.getChildCount(); i++) {
                                MetricTreeNode childNode = (MetricTreeNode) node.getChildAt(i);
                                if (childNode != null && childNode.getId().equals(id)) {
                                    return childNode;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Find node by ID error: {0}", e.toString());
        }

        return null;
    }

    private static void rebuildMenuNodes() {
        String name = FileUtilManager.getItem("name");
        // check to see if we need to swap out the signup nodes with the signed up node
        MetricTreeNode loggedInNode = findNodeById(TreeHelper.LOGGED_IN_ID);
        if (StringUtils.isNotBlank(name) && loggedInNode == null) {
            // swap the nodes out
            removeNodeById(TreeHelper.EMAIL_SIGNUP_ID);
            removeNodeById(TreeHelper.GITHIUB_SIGNUP_ID);
            removeNodeById(TreeHelper.GOOGLE_SIGNUP_ID);

            // add the LOGGED_IN_ID node
            loggedInNode = TreeHelper.buildLoggedInNode();
            ((DefaultMutableTreeNode)metricTree.getModel().getRoot()).insert(loggedInNode, 0);
        } else {
            String authType = FileUtilManager.getItem("authType");
            String iconName = "envelope.svg";
            if ("google".equals(authType)) {
                iconName = "icons8-google.svg";
            } else if ("github".equals(authType)) {
                iconName = "icons8-github.svg";
            }

            String email = FileUtilManager.getItem("name");
            // update the logged in node
            updateNodeLabel(findNodeById(TreeHelper.LOGGED_IN_ID), email, iconName);
        }

        // update the toggle node label
        String toggleText = "Hide status bar metrics";
        if (!SoftwareCoUtils.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }

        updateNodeLabel(findNodeById(TreeHelper.TOGGLE_METRICS_ID), toggleText);
    }

    private static void rebuildFlowNodes() {
        MetricTreeNode connectSlackId = findNodeById(TreeHelper.CONNECT_SLACK_ID);
        if (connectSlackId != null) {
            if (SlackManager.hasSlackWorkspaces()) {
                // remove this node
                removeNodeById(TreeHelper.CONNECT_SLACK_ID);
            }
        }
    }

}
