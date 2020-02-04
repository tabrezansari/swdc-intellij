package com.softwareco.intellij.plugin.tree;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBList;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.SessionSummary;
import com.softwareco.intellij.plugin.sessiondata.SessionDataManager;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;

public class TreeItemBuilder {

    private static SessionSummary sessionSummary = new SessionSummary();

    public static void initializeSessionSummary() {
        sessionSummary = SessionDataManager.getSessionSummaryData();
    }

    public static JBList<JLabel> buildCodeTimeLabels() {
        DefaultListModel listModel = new DefaultListModel();

        // add the code time labels
        listModel.add(0, buildWebDashboardLabel());
        listModel.add(1, buildGenerateDashboardLabel());
        listModel.add(2, buildToggleStatusTextLabel());
        listModel.add(3, buildLearnMoreLabel());
        listModel.add(4, buildSubmitFeedbackLabel());

        JBList<JLabel> jbList = new JBList<>(listModel);
        jbList.setCellRenderer(new ListRenderer());
        jbList.setVisibleRowCount(1);
        jbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jbList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                JBList<JLabel> lst = (JBList<JLabel>) e.getSource();
                JLabel lbl = lst.getSelectedValue();
                if (lbl.getName().equals("webdashboard")) {
                    // if they're not logged in, launch the onboarding
                    boolean isLoggedIn = (!SoftwareCoUtils.isLoggedIn()) ? false : true;
                    if (!isLoggedIn) {
                        SoftwareCoSessionManager.launchLogin();
                    } else {
                        SoftwareCoSessionManager.launchWebDashboard();
                    }
                } else if (lbl.getName().equals("editordashboard")) {
                    SoftwareCoUtils.launchCodeTimeMetricsDashboard();
                } else if (lbl.getName().equals("submitfeedback")) {
                    SoftwareCoUtils.submitFeedback();
                } else if (lbl.getName().equals("learnmore")) {
                    FileManager.openReadmeFile();
                } else if (lbl.getName().equals("togglestatus")) {
                    SoftwareCoUtils.toggleStatusBar();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                JBList<JLabel> lst = (JBList<JLabel>) e.getSource();
                lst.clearSelection();
            }
        });
        jbList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                int row = jbList.locationToIndex(e.getPoint());
                jbList.setSelectedIndex(row);
            }
        });
        jbList.updateUI();

        return jbList;
    }

    private static JLabel buildWebDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/paw.svg");
        label.setIcon(icon);
        label.setText("See advanced metrics");
        label.setName("webdashboard");
        return label;
    }

    private static JLabel buildGenerateDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("Generate dashboard");
        label.setName("editordashboard");
        return label;
    }

    private static JLabel buildToggleStatusTextLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/visible.svg");
        label.setIcon(icon);
        if (SoftwareCoUtils.showingStatusText()) {
            label.setText("Hide status bar metrics");
        } else {
            label.setText("Show status bar metrics");
        }
        label.setName("togglestatus");
        return label;
    }

    private static JLabel buildLearnMoreLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.findIcon("/com/softwareco/intellij/plugin/assets/readme.svg");
        label.setIcon(icon);
        label.setText("Learn more");
        label.setName("learnmore");
        return label;
    }

    private static JLabel buildSubmitFeedbackLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/message.svg");
        label.setIcon(icon);
        label.setText("Submit feedback");
        label.setName("submitfeedback");
        return label;
    }

    public static JSeparator getSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentY(0.0f);
        separator.setForeground(new Color(58, 86, 187));
        return separator;
    }

    private static MetricTree buildTreeItem(String parentName, MetricTreeNode todayNode, MetricTreeNode avgNode, MetricTreeNode globalNode) {
        MetricTreeNode node = buildParentNode(parentName);
        node.add(todayNode);
        if (avgNode != null) {
            node.add(avgNode);
        }
        if (globalNode != null) {
            node.add(globalNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(node);
        MetricTree tree = new MetricTree(model);

        tree.setCellRenderer(new MetricTreeRenderer());
        MetricTreeRenderer renderer = (MetricTreeRenderer) tree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,0));
        renderer.setBorderSelectionColor(new Color(0,0,0,0));
        tree.setBackground((Color)null);
        tree.requestFocus();
        return tree;
    }

    public static MetricTree buildEditorTimeTree() {
        String min = SoftwareCoUtils.humanizeMinutes(WallClockManager.getWcTimeInSeconds() / 60);
        MetricTreeNode todayNode = buildChildNode("Today: " + min, "rocket.svg");
        return buildTreeItem("Editor time", todayNode, null, null);
    }

    public static MetricTree buildCodeTimeTree() {
        String min = SoftwareCoUtils.humanizeMinutes(sessionSummary.getCurrentDayMinutes());
        String avg = SoftwareCoUtils.humanizeMinutes(sessionSummary.getAverageDailyMinutes());
        String globalAvg = SoftwareCoUtils.humanizeMinutes(sessionSummary.getGlobalAverageDailyMinutes());
        MetricTreeNode todayNode = buildChildNode("Today: " + min, "rocket.svg");
        String avgIconName = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avg, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalAvg, "global-grey.svg");
        return buildTreeItem("Code time", todayNode, avgNode, globalNode);
    }

    public static MetricTree buildLinesAddedTree() {
        // create the editor time parent
        MetricTreeNode node = buildParentNode("Lines added");
        long linesAdded = sessionSummary.getCurrentDayLinesAdded();
        long avgLinesAdded = sessionSummary.getAverageLinesAdded();
        long globalAvgLinesAdded = sessionSummary.getGlobalAverageLinesAdded();
        MetricTreeNode todayNode = buildChildNode("Today: " + linesAdded, "rocket.svg");
        String avgIconName = sessionSummary.getAverageLinesAdded() < sessionSummary.getCurrentDayLinesAdded() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avgLinesAdded, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalAvgLinesAdded, "global-grey.svg");
        return buildTreeItem("Lines added", todayNode, avgNode, globalNode);
    }

    public static MetricTree buildLinesRemovedTree() {
        // create the editor time parent
        MetricTreeNode node = buildParentNode("Lines removed");
        long linesRemoved = sessionSummary.getCurrentDayLinesRemoved();
        long avgLinesRemoved = sessionSummary.getAverageLinesRemoved();
        long globalAvgLinesRemoved = sessionSummary.getGlobalAverageLinesAdded();
        MetricTreeNode todayNode = buildChildNode("Today: " + linesRemoved, "rocket.svg");
        String avgIconName = sessionSummary.getAverageLinesRemoved() < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avgLinesRemoved, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalAvgLinesRemoved, "global-grey.svg");
        return buildTreeItem("Lines removed", todayNode, avgNode, globalNode);
    }

    public static MetricTree buildKeystrokesTree() {
        // create the editor time parent
        MetricTreeNode node = buildParentNode("Keystrokes");
        long keystrokes = sessionSummary.getCurrentDayKeystrokes();
        long avgKeystrokes = sessionSummary.getAverageDailyKeystrokes();
        long globalKeystrokes = sessionSummary.getGlobalAverageDailyKeystrokes();
        MetricTreeNode todayNode = buildChildNode("Today: " + keystrokes, "rocket.svg");
        String avgIconName = sessionSummary.getAverageDailyKeystrokes() < sessionSummary.getCurrentDayKeystrokes() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avgKeystrokes, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalKeystrokes, "global-grey.svg");
        return buildTreeItem("Editor Time", todayNode, avgNode, globalNode);
    }

    private static MetricTreeNode buildParentNode(String name) {
        String id = name.replaceAll("\\s+", "");
        MetricTreeNode parentNode = new MetricTreeNode(name, id);
        DefaultTreeModel parentNodeModel = new DefaultTreeModel(parentNode);
        parentNode.setModel(parentNodeModel);
        return parentNode;
    }

    private static MetricTreeNode buildChildNode(String name, String iconName) {
        String id = name.replaceAll("\\s+", "");
        MetricTreeNode childNode = new MetricTreeNode(name, id);
        childNode.setIconName(iconName);
        return childNode;
    }

    private static void addChildToParent(String childName, MetricTreeNode parentNode) {
        String id = childName.replaceAll("\\s+", "");
        MetricTreeNode childNode = new MetricTreeNode(childName, id);
        parentNode.add(childNode);
    }

}
