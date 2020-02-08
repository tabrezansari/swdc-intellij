package com.softwareco.intellij.plugin.tree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBList;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.aggdata.FileAggregateDataManager;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.CommitChangeStats;
import com.softwareco.intellij.plugin.models.FileChangeInfo;
import com.softwareco.intellij.plugin.models.SessionSummary;
import com.softwareco.intellij.plugin.repo.GitUtil;
import com.softwareco.intellij.plugin.sessiondata.SessionDataManager;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class TreeItemBuilder {

    private static SessionSummary sessionSummary = new SessionSummary();
    private static Map<String, FileChangeInfo> fileChangeInfoMap = new HashMap<>();

    public static void initializeSessionSummary() {
        sessionSummary = SessionDataManager.getSessionSummaryData();
        fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
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
                    FileManager.getInstance().openReadmeFile();
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
                JBList<JLabel> lst = (JBList<JLabel>) e.getSource();
                int row = lst.locationToIndex(e.getPoint());
                lst.setSelectedIndex(row);
            }
        });
        jbList.updateUI();

        return jbList;
    }

    private static JLabel buildWebDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/paw.png");
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

    public static MetricTree buildEditorTimeTree() {
        String min = SoftwareCoUtils.humanizeMinutes(WallClockManager.getInstance().getWcTimeInSeconds() / 60);
        MetricTreeNode todayNode = buildChildNode("Today: " + min, "rocket.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode);
        return buildTreeItem("Editor time", nodes);
    }

    public static MetricTree buildCodeTimeTree() {
        // build the code time nodes
        String min = SoftwareCoUtils.humanizeMinutes(sessionSummary.getCurrentDayMinutes());
        String avg = SoftwareCoUtils.humanizeMinutes(sessionSummary.getAverageDailyMinutes());
        String globalAvg = SoftwareCoUtils.humanizeMinutes(sessionSummary.getGlobalAverageDailyMinutes());
        MetricTreeNode todayNode = buildChildNode("Today: " + min, "rocket.svg");
        String avgIconName = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avg, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalAvg, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Code time", nodes);
    }

    public static MetricTree buildLinesAddedTree() {
        // create the lines added nodes
        String linesAdded = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayLinesAdded());
        String avgLinesAdded = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageLinesAdded());
        String globalAvgLinesAdded = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
        MetricTreeNode todayNode = buildChildNode("Today: " + linesAdded, "rocket.svg");
        String avgIconName = sessionSummary.getAverageLinesAdded() < sessionSummary.getCurrentDayLinesAdded() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avgLinesAdded, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalAvgLinesAdded, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Lines added", nodes);
    }

    public static MetricTree buildLinesRemovedTree() {
        // create the lines removed nodes
        String linesRemoved = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayLinesRemoved());
        String avgLinesRemoved = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageLinesRemoved());
        String globalAvgLinesRemoved = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
        MetricTreeNode todayNode = buildChildNode("Today: " + linesRemoved, "rocket.svg");
        String avgIconName = sessionSummary.getAverageLinesRemoved() < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avgLinesRemoved, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalAvgLinesRemoved, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Lines removed", nodes);
    }

    public static MetricTree buildKeystrokesTree() {
        // create the keystrokes nodes
        String keystrokes = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayKeystrokes());
        String avgKeystrokes = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageDailyKeystrokes());
        String globalKeystrokes = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageDailyKeystrokes());
        MetricTreeNode todayNode = buildChildNode("Today: " + keystrokes, "rocket.svg");
        String avgIconName = sessionSummary.getAverageDailyKeystrokes() < sessionSummary.getCurrentDayKeystrokes() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average: " + avgKeystrokes, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average: " + globalKeystrokes, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Keystrokes", nodes);
    }

    public static MetricTree buildMostEditedFileTree() {
        if (fileChangeInfoMap.size() == 0) {
            return null;
        }
        // build the most edited files nodes
        List<MetricTreeNode> nodes = new ArrayList<>();
        // sort the fileChangeInfoMap based on keystrokes
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());

        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {
                        Long a = new Long(entryA.getValue().keystrokes);
                        Long b = new Long(entryB.getValue().keystrokes);
                        return a.compareTo(b);
                    }
                }
        );

        int count = 0;
        // go from the end
        for (int i = entryList.size() - 1; i > 0; i--) {
            if (count >= 3) {
                break;
            }
            Map.Entry<String, FileChangeInfo> fileChangeInfoEntry = entryList.get(i);
            String name = fileChangeInfoEntry.getValue().name;
            if (name == null || name.length() == 0) {
                Path path = Paths.get(fileChangeInfoEntry.getKey());
                if (path != null) {
                    Path fileName = path.getFileName();
                    name = fileName.toString();
                }
            }
            String label = name + " | " + SoftwareCoUtils.humanizeLongNumbers(fileChangeInfoEntry.getValue().keystrokes);
            MetricTreeNode editedFileNode = buildChildNode(label, "files.svg");
            editedFileNode.setData(fileChangeInfoEntry.getValue());
            nodes.add(editedFileNode);
            count++;
        }

        MetricTree topKeystrokesTree = buildTreeItem("Top files by keystrokes", nodes);
        topKeystrokesTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                updateSelectionState(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                MetricTree mTree = (MetricTree)e.getSource();
                mTree.clearSelection();
            }
        });
        return topKeystrokesTree;
    }

    public static MetricTree buildOpenGitChanges() {
        String id = "open-changes";
        List<CodeTimeToolWindow.ExpandState> expandStates = CodeTimeToolWindow.getExpandStates(id);
        MetricTreeNode openChangesNode = new MetricTreeNode("Open changes", id);
        openChangesNode.setModel(new DefaultTreeModel(openChangesNode));

        ProjectManager pm = ProjectManager.getInstance();
        if (pm != null && pm.getOpenProjects() != null && pm.getOpenProjects().length > 0) {
            for (Project p : pm.getOpenProjects()) {
                CommitChangeStats commitChangeStats = GitUtil.getUncommitedChanges(p.getBasePath());
                MetricTreeNode node = buildGitChangeNode(p, commitChangeStats);

                openChangesNode.add(node);
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(openChangesNode);
        MetricTree tree = new MetricTree(model);

        tree.setCellRenderer(new MetricTreeRenderer());
        MetricTreeRenderer renderer = (MetricTreeRenderer) tree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,0));
        renderer.setBorderSelectionColor(new Color(0,0,0,0));

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                updateSelectionState(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                MetricTree mTree = (MetricTree)e.getSource();
                mTree.clearSelection();
            }
        });

        tree.setBackground((Color)null);
        if (expandStates != null && expandStates.size() > 0) {
            for (CodeTimeToolWindow.ExpandState expandState : expandStates) {
                if (expandState.expand) {
                    tree.expandRow(expandState.path.getPathCount() - 1);
                }
            }
        } else {
            tree.setExpandedState(new TreePath(model.getPathToRoot(openChangesNode)), false);
        }
        return tree;
    }

    public static MetricTree buildCommittedGitChanges() {
        String id = "committed-today";
        List<CodeTimeToolWindow.ExpandState> expandStates = CodeTimeToolWindow.getExpandStates(id);

        MetricTreeNode committedTodayNode = new MetricTreeNode("Committed today", id);
        committedTodayNode.setModel(new DefaultTreeModel(committedTodayNode));

        ProjectManager pm = ProjectManager.getInstance();
        if (pm != null && pm.getOpenProjects() != null && pm.getOpenProjects().length > 0) {
            for (Project p : pm.getOpenProjects()) {
                CommitChangeStats commitChangeStats = GitUtil.getTodaysCommits(p.getBasePath());
                MetricTreeNode node = buildGitChangeNode(p, commitChangeStats);

                committedTodayNode.add(node);
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(committedTodayNode);
        MetricTree tree = new MetricTree(model);

        tree.setCellRenderer(new MetricTreeRenderer());
        MetricTreeRenderer renderer = (MetricTreeRenderer) tree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,0));
        renderer.setBorderSelectionColor(new Color(0,0,0,0));

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                updateSelectionState(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                MetricTree mTree = (MetricTree)e.getSource();
                mTree.clearSelection();
            }
        });

        tree.setBackground((Color)null);
        if (expandStates != null && expandStates.size() > 0) {
            for (CodeTimeToolWindow.ExpandState expandState : expandStates) {
                if (expandState.expand) {
                    tree.expandRow(expandState.path.getPathCount() - 1);
                }
            }
        } else {
            tree.setExpandedState(new TreePath(model.getPathToRoot(committedTodayNode)), false);
        }
        return tree;
    }

    private static MetricTreeNode buildGitChangeNode(Project p, CommitChangeStats commitChangeStats) {
        MetricTreeNode parentNode = new MetricTreeNode(p.getName(), p.getName());
        DefaultTreeModel parentNodeModel = new DefaultTreeModel(parentNode);
        parentNode.setModel(parentNodeModel);

        // add the change stat children (insertions and deletions)
        String insertions = "insertion(s): " + SoftwareCoUtils.humanizeLongNumbers(commitChangeStats.getInsertions());
        MetricTreeNode insertionsNode = new MetricTreeNode(insertions, "insertions-" + p.getName());
        insertionsNode.setIconName("insertion.svg");
        parentNode.add(insertionsNode);

        String deletions = "deletion(s): " + SoftwareCoUtils.humanizeLongNumbers(commitChangeStats.getDeletions());
        MetricTreeNode deletionNode = new MetricTreeNode(deletions, "deletions-" + p.getName());
        deletionNode.setIconName("deletion.svg");
        parentNode.add(deletionNode);

        if (commitChangeStats.isCommitted()) {
            // add the change stat children (commits and files changed)
            String commits = "commit(s): " + SoftwareCoUtils.humanizeLongNumbers(commitChangeStats.getCommitCount());
            MetricTreeNode commitsNode = new MetricTreeNode(commits, "commits-" + p.getName());
            commitsNode.setIconName("commit.svg");
            parentNode.add(commitsNode);

            String filesChanged = "Files changed: " + SoftwareCoUtils.humanizeLongNumbers(commitChangeStats.getFileCount());
            MetricTreeNode filesChangedNode = new MetricTreeNode(filesChanged, "filecount-" + p.getName());
            filesChangedNode.setIconName("files.svg");
            parentNode.add(filesChangedNode);
        }

        return parentNode;
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

    private static MetricTree buildTreeItem(String parentName, List<MetricTreeNode> nodes) {

        String id = parentName.replaceAll("\\s+", "");
        List<CodeTimeToolWindow.ExpandState> expandStates = CodeTimeToolWindow.getExpandStates(id);

        MetricTreeNode node = buildParentNode(parentName);
        for (MetricTreeNode mtNode : nodes) {
            node.add(mtNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(node);
        MetricTree tree = new MetricTree(model);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // update the tree expand state and clear the selection
                updateSelectionState(e);
            }

        });

        tree.setCellRenderer(new MetricTreeRenderer());

        MetricTreeRenderer renderer = (MetricTreeRenderer) tree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,0));
        renderer.setBorderSelectionColor(new Color(0,0,0,0));

        tree.setBackground((Color)null);
        if (expandStates != null && expandStates.size() > 0) {
            for (CodeTimeToolWindow.ExpandState expandState : expandStates) {
                if (expandState.expand) {
                    tree.expandRow(expandState.path.getPathCount() - 1);
                }
            }
        } else {
            tree.setExpandedState(new TreePath(model.getPathToRoot(node)), false);
        }
        return tree;
    }

    private static void updateSelectionState(MouseEvent e) {
        MetricTree mTree = (MetricTree)e.getSource();
        DefaultTreeModel dfModel = (DefaultTreeModel)mTree.getModel();
        MetricTreeNode mtNode = (MetricTreeNode)dfModel.getRoot();
        TreePath treePath = new TreePath(dfModel.getPathToRoot(mtNode));
        String id = mtNode.getId();
        Enumeration<TreePath> expandedDescendants = mTree.getExpandedDescendants(treePath);
        if (expandedDescendants != null) {
            while (expandedDescendants.hasMoreElements()) {
                TreePath p = expandedDescendants.nextElement();

                CodeTimeToolWindow.ExpandState expandState =
                        new CodeTimeToolWindow.ExpandState(mTree.expandState, p);
                CodeTimeToolWindow.updateExpandState(id, expandState);
            }
        }
        if (mTree.getLeadSelectionPath() != null) {
            MetricTreeNode selectedNode = (MetricTreeNode)mTree.getLeadSelectionPath().getLastPathComponent();
            if (selectedNode != null) {
                if (selectedNode.getData() != null && selectedNode.getData() instanceof FileChangeInfo) {
                    String fsPath = ((FileChangeInfo)selectedNode.getData()).fsPath;
                    SoftwareCoUtils.launchFile(fsPath);
                }
            }
        }

        mTree.clearSelection();
    }

}
