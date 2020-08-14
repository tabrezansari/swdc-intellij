package com.softwareco.intellij.plugin.tree;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBList;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.managers.*;
import com.softwareco.intellij.plugin.models.*;
import com.softwareco.intellij.plugin.repo.GitUtil;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TreeItemBuilder {

    private static SessionSummary sessionSummary = new SessionSummary();
    private static Map<String, FileChangeInfo> fileChangeInfoMap = new HashMap<>();
    private static String dayStr = "";

    private static boolean expandInitNodes = false;
    private static String selectedNodeName = null;

    public static void initializeSessionSummary(boolean initializing) {
        sessionSummary = SessionDataManager.getSessionSummaryData();
        fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        expandInitNodes = initializing;

        SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
        dayStr = formatDay.format(new Date());
    }

    public static JBList<JLabel> buildContributorTitle() {
        Project p = SoftwareCoUtils.getFirstActiveProject();
        if (p != null) {
            ResourceInfo resourceInfo = GitUtil.getResourceInfo(p.getBasePath());
            if (resourceInfo != null && StringUtils.isNotBlank(resourceInfo.getIdentifier())) {
                DefaultListModel listModel = new DefaultListModel();
                JLabel label = new JLabel();

                Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/icons8-github.svg");
                label.setIcon(icon);
                label.setText(resourceInfo.getIdentifier());
                label.setName("contributor_title");
                listModel.add(0, label);

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
                        if (lbl != null) {
                            if (lbl.getName().equals("contributor_title")) {
                                ReportManager.displayProjectContributorSummaryDashboard(lbl.getText());

                                UIElementEntity elementEntity = new UIElementEntity();
                                elementEntity.element_name = "ct_contributor_repo_identifier_btn";
                                elementEntity.element_location = "ct_contributors_tree";
                                elementEntity.color = null;
                                elementEntity.cta_text = "redacted";
                                elementEntity.icon_name = "repo";
                                EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
                            }
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
        }
        return null;
    }

    public static JBList<JLabel> buildSignupLabels() {
        DefaultListModel listModel = new DefaultListModel();

        JBList<JLabel> jbList = new JBList<>(listModel);
        jbList.setCellRenderer(new ListRenderer());
        jbList.setVisibleRowCount(1);
        jbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        String name = FileManager.getItem("name");
        if (name == null || name.equals("")) {
            listModel.add(0, buildSignupLabel("google"));
            listModel.add(1, buildSignupLabel("github"));
            listModel.add(2, buildSignupLabel("email"));

            jbList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);

                    JBList<JLabel> lst = (JBList<JLabel>) e.getSource();
                    JLabel lbl = lst.getSelectedValue();
                    if (lbl != null) {
                        if (lbl.getName().equals("signup-google")) {
                            SoftwareCoSessionManager.launchLogin("google", UIInteractionType.click);
                        } else if (lbl.getName().equals("signup-github")) {
                            SoftwareCoSessionManager.launchLogin("github", UIInteractionType.click);
                        } else if (lbl.getName().equals("signup-email")) {
                            SoftwareCoSessionManager.launchLogin("software", UIInteractionType.click);
                        }
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
        } else {
            listModel.add(0, buildLoggedInLabel());
        }
        jbList.updateUI();

        return jbList;
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
                if (lbl != null) {
                    if (lbl.getName().equals("webdashboard")) {
                        // if they're not logged in, launch the onboarding
                        boolean isLoggedIn = (!SoftwareCoUtils.isLoggedIn()) ? false : true;
                        if (!isLoggedIn) {
                            SoftwareCoSessionManager.launchLogin("software", UIInteractionType.click);
                        } else {
                            SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.click);
                        }
                    } else if (lbl.getName().equals("editordashboard")) {
                        SoftwareCoUtils.launchCodeTimeMetricsDashboard(UIInteractionType.click);
                    } else if (lbl.getName().equals("submitfeedback")) {
                        SoftwareCoUtils.submitFeedback(UIInteractionType.click);
                    } else if (lbl.getName().equals("learnmore")) {
                        FileManager.openReadmeFile(UIInteractionType.click);
                    } else if (lbl.getName().equals("togglestatus")) {
                        SoftwareCoUtils.toggleStatusBar(UIInteractionType.click);
                    }
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

    private static JLabel buildSignupLabel(String type) {
        JLabel label = new JLabel();
        String iconName = "envelope.svg";
        String text = "Sign up with email";
        if (type.equals("google")) {
            iconName = "icons8-google.svg";
            text = "Sign up with Google";
        } else if (type.equals("github")) {
            iconName = "icons8-github.svg";
            text = "Sign up with GitHub";
        }
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/" + iconName);
        label.setIcon(icon);
        label.setText(text);
        label.setName("signup-" + type);
        return label;
    }

    private static JLabel buildLoggedInLabel() {
        JLabel label = new JLabel();
        String authType = FileManager.getItem("authType");
        String name = FileManager.getItem("name");
        String tooltip = name != null ? "Connected as " + name : "";
        String iconName = "envelope.svg";
        String text = "Connected using email";
        if (authType != null && authType.equals("google")) {
            iconName = "icons8-google.svg";
            text = "Connected using Google";
        } else if (authType != null && authType.equals("github")) {
            iconName = "icons8-github.svg";
            text = "Connected using GitHub";
        }
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/" + iconName);
        label.setIcon(icon);
        label.setText(text);
        label.setName("connected-" + authType);
        return label;
    }

    private static JLabel buildWebDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/paw-grey.png");
        label.setIcon(icon);
        label.setText("See advanced metrics");
        label.setName("webdashboard");
        return label;
    }

    private static JLabel buildGenerateDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("View summary");
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

    public static MetricTree buildEditorTimeTree(CodeTimeSummary codeTimeSummary) {
        String min = SoftwareCoUtils.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        MetricTreeNode todayNode = buildChildNode("Today: " + min, "rocket.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode);
        return buildTreeItem("Code time", nodes, true);
    }

    public static MetricTree buildCodeTimeTree(CodeTimeSummary codeTimeSummary) {
        // build the code time nodes
        String min = SoftwareCoUtils.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes);
        String avg = SoftwareCoUtils.humanizeMinutes(sessionSummary.getAverageDailyMinutes());
        String globalAvg = SoftwareCoUtils.humanizeMinutes(sessionSummary.getGlobalAverageDailyMinutes());
        MetricTreeNode todayNode = buildChildNode("Today: " + min, "rocket.svg");
        String avgIconName = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average (" + dayStr + "): " + avg, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average (" + dayStr + "): " + globalAvg, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Active code time", nodes, true);
    }

    public static MetricTree buildLinesAddedTree() {
        // create the lines added nodes
        String linesAdded = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayLinesAdded());
        String avgLinesAdded = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageLinesAdded());
        String globalAvgLinesAdded = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
        MetricTreeNode todayNode = buildChildNode("Today: " + linesAdded, "rocket.svg");
        String avgIconName = sessionSummary.getAverageLinesAdded() < sessionSummary.getCurrentDayLinesAdded() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average (" + dayStr + "): " + avgLinesAdded, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average (" + dayStr + "): " + globalAvgLinesAdded, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Lines added", nodes, false);
    }

    public static MetricTree buildLinesRemovedTree() {
        // create the lines removed nodes
        String linesRemoved = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayLinesRemoved());
        String avgLinesRemoved = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageLinesRemoved());
        String globalAvgLinesRemoved = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
        MetricTreeNode todayNode = buildChildNode("Today: " + linesRemoved, "rocket.svg");
        String avgIconName = sessionSummary.getAverageLinesRemoved() < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average (" + dayStr + "): " + avgLinesRemoved, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average (" + dayStr + "): " + globalAvgLinesRemoved, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Lines removed", nodes, false);
    }

    public static MetricTree buildKeystrokesTree() {
        // create the keystrokes nodes
        String keystrokes = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayKeystrokes());
        String avgKeystrokes = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageDailyKeystrokes());
        String globalKeystrokes = SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageDailyKeystrokes());
        MetricTreeNode todayNode = buildChildNode("Today: " + keystrokes, "rocket.svg");
        String avgIconName = sessionSummary.getAverageDailyKeystrokes() < sessionSummary.getCurrentDayKeystrokes() ? "bolt.svg" : "bolt-grey.svg";
        MetricTreeNode avgNode = buildChildNode("Your average (" + dayStr + "): " + avgKeystrokes, avgIconName);
        MetricTreeNode globalNode = buildChildNode("Global average (" + dayStr + "): " + globalKeystrokes, "global-grey.svg");
        List<MetricTreeNode> nodes = Arrays.asList(todayNode, avgNode, globalNode);
        return buildTreeItem("Keystrokes", nodes, false);
    }

    public static MetricTree buildTopKeystrokesFilesTree() {
        return buildTopFilesTree("Top files by keystrokes", "keystrokes");
    }

    public static MetricTree buildTopKpmFilesTree() {
        return buildTopFilesTree("Top files by KPM", "kpm");
    }

    public static MetricTree buildTopCodeTimeFilesTree() {
        return buildTopFilesTree("Top files by code time", "codetime");
    }

    private static MetricTree buildTopFilesTree(String parentName, String sortBy) {
        if (fileChangeInfoMap.size() == 0) {
            return null;
        }
        // build the most edited files nodes
        List<MetricTreeNode> nodes = new ArrayList<>();
        // sort the fileChangeInfoMap based on keystrokes
        List<Map.Entry<String, FileChangeInfo>> entryList = null;

        if (sortBy.equals("kpm")) {
            entryList = sortByKpm();
        } else if (sortBy.equals("keystrokes")) {
            entryList = sortByKeystrokes();
        } else if (sortBy.equals("codetime")) {
            entryList = sortByFileSeconds();
        }

        int count = 0;
        // go from the end
        for (int i = entryList.size() - 1; i >= 0; i--) {
            if (count >= 3) {
                break;
            }
            Map.Entry<String, FileChangeInfo> fileChangeInfoEntry = entryList.get(i);
            String name = fileChangeInfoEntry.getValue().name;
            if (StringUtils.isBlank(name)) {
                Path path = Paths.get(fileChangeInfoEntry.getKey());
                if (path != null) {
                    Path fileName = path.getFileName();
                    if (fileName != null) {
                        name = fileName.toString();
                    } else {
                        name = "Untitled";
                    }
                }
            }

            String val = "";
            if (sortBy.equals("kpm")) {
                val = SoftwareCoUtils.humanizeLongNumbers(fileChangeInfoEntry.getValue().kpm);
            } else if (sortBy.equals("keystrokes")) {
                val = SoftwareCoUtils.humanizeLongNumbers(fileChangeInfoEntry.getValue().keystrokes);
            } else if (sortBy.equals("codetime")) {
                val = SoftwareCoUtils.humanizeMinutes(fileChangeInfoEntry.getValue().duration_seconds / 60);
            }

            String label = name + " | " + val;
            MetricTreeNode editedFileNode = buildChildNode(label, "files.svg");
            editedFileNode.setData(fileChangeInfoEntry.getValue());
            nodes.add(editedFileNode);
            count++;
        }

        return buildTreeItem(parentName, nodes, false);
    }

    public static List<MetricTree> buildContributorUsers() {
        Project p = SoftwareCoUtils.getFirstActiveProject();
        if (p != null) {
            String repoUrl = GitUtil.getRepoUrlLink(p.getBasePath());

            ResourceInfo resourceInfo = GitUtil.getResourceInfo(p.getBasePath());
            List<MetricTree> contributors = new ArrayList<>();
            if (resourceInfo != null && resourceInfo.getMembers().size() > 0) {
                for (TeamMember member: resourceInfo.getMembers()) {
                    // get the user's last commit
                    CommitInfo commitInfo = GitUtil.getLastCommitInfo(p.getBasePath(), member.getEmail());
                    String commitUrl = repoUrl + "/commit/" + commitInfo.getCommitId();

                    MetricTreeNode commitUrlNode = buildChildNode(commitInfo.getComment(), "commit.svg");
                    commitUrlNode.setData(commitUrl);
                    List<MetricTreeNode> nodes = Arrays.asList(commitUrlNode);
                    MetricTree mTree = buildTreeItem(member.getName(), nodes, false);
                    contributors.add(mTree);
                }
            }
            return contributors;
        }
        return null;
    }

    public static MetricTree buildGitTree(String name, String filterBy) {
        String id = name.replaceAll("\\s+", "");
        List<CodeTimeToolWindow.ExpandState> expandStates = CodeTimeToolWindow.getExpandState(id);
        MetricTreeNode openChangesNode = new MetricTreeNode(name, id);
        openChangesNode.setModel(new DefaultTreeModel(openChangesNode));

        ProjectManager pm = ProjectManager.getInstance();
        if (pm != null && pm.getOpenProjects() != null && pm.getOpenProjects().length > 0) {
            for (Project p : pm.getOpenProjects()) {
                CommitChangeStats commitChangeStats = null;

                if (filterBy.equals("uncommitted")) {
                    commitChangeStats = GitUtil.getUncommitedChanges(p.getBasePath());
                } else {
                    commitChangeStats = GitUtil.getTodaysCommits(p.getBasePath(), null);
                }
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

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent e) {
                selectedNodeName = getSelectedNodeName(e);
                updateExpandState(e, true);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
                selectedNodeName = getSelectedNodeName(e);
                updateExpandState(e, false);
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                MetricTree mTree = (MetricTree)e.getSource();
                mTree.clearSelection();
            }
        });

        tree.setBackground((Color)null);
        TreePath p = new TreePath(model.getPathToRoot(openChangesNode));

        // set the expansion
        if (expandStates != null && expandStates.size() > 0) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath tp = tree.getPathForRow(i);
                boolean expandIt = false;
                for (CodeTimeToolWindow.ExpandState state : expandStates) {
                    if (state.path.toString().equals(tp.toString())) {
                        expandIt = state.expand;
                        break;
                    }
                }
                if (expandIt) {
                    tree.expandRow(i);
                } else {
                    tree.collapseRow(i);
                }
            }
        } else {
            tree.setExpandedState(p, false);
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

    private static MetricTree buildTreeItem(String parentName, List<MetricTreeNode> nodes, boolean isDefaultOpen) {

        String id = parentName.replaceAll("\\s+", "");
        List<CodeTimeToolWindow.ExpandState> expandStates = CodeTimeToolWindow.getExpandState(id);

        MetricTreeNode node = buildParentNode(parentName);
        for (MetricTreeNode mtNode : nodes) {
            node.add(mtNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(node);
        MetricTree tree = new MetricTree(model);

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent e) {
                selectedNodeName = getSelectedNodeName(e);
                updateExpandState(e, true);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
                selectedNodeName = getSelectedNodeName(e);
                updateExpandState(e, false);
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                launchFileClick(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                MetricTree mTree = (MetricTree)e.getSource();
                mTree.clearSelection();
            }
        });

        tree.setCellRenderer(new MetricTreeRenderer());

        MetricTreeRenderer renderer = (MetricTreeRenderer) tree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,0));
        renderer.setBorderSelectionColor(new Color(0,0,0,0));

        tree.setBackground((Color)null);
        TreePath p = new TreePath(model.getPathToRoot(node));

        // set the expansion
        if (expandStates != null && expandStates.size() > 0) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath tp = tree.getPathForRow(i);
                boolean expandIt = false;
                for (CodeTimeToolWindow.ExpandState state : expandStates) {
                    if (state.path.toString().equals(tp.toString())) {
                        expandIt = state.expand;
                        break;
                    }
                }
                if (expandIt) {
                    tree.expandRow(i);
                } else {
                    tree.collapseRow(i);
                }
            }
        } else if (expandInitNodes && isDefaultOpen) {
            tree.setExpandedState(p, true);
            CodeTimeToolWindow.updateExpandState(id, p, true);
        } else {
            tree.setExpandedState(p, false);
        }

        return tree;
    }

    private static List<String> toggleItems = Arrays.asList("ct_codetime_toggle_node",
            "ct_active_codetime_toggle_node",
            "ct_lines_added_toggle_node",
            "ct_lines_removed_toggle_node",
            "ct_keystrokes_toggle_node",
            "ct_files_changed_toggle_node",
            "ct_top_files_by_kpm_toggle_node",
            "ct_top_files_by_keystrokes_toggle_node",
            "ct_top_files_by_codetime_toggle_node",
            "ct_open_changes_toggle_node",
            "ct_committed_today_toggle_node");

    private static String getToggleItem(String normalizedLabel) {
        for (String toggleItem : toggleItems) {
            // strip off "ct_" and "_toggle_node" and replace the "_" with ""
            String normalizedToggleItem = toggleItem.replace("ct_", "").replace("_toggle_node", "").replaceAll("_", "");
            if (normalizedLabel.toLowerCase().indexOf(normalizedToggleItem) != -1) {
                return toggleItem;
            }
        }
        return null;
    }

    private static String getSelectedNodeName(TreeExpansionEvent e) {
        MetricTree mTree = (MetricTree)e.getSource();
        DefaultTreeModel dfModel = (DefaultTreeModel)mTree.getModel();
        MetricTreeNode mtNode = (MetricTreeNode)dfModel.getRoot();
        if (mtNode != null && mtNode.getUserObject() != null) {
            return mtNode.getUserObject().toString();
        }
        return null;
    }

    private static void updateExpandState(TreeExpansionEvent e, boolean expanded) {
        TreePath path = e.getPath();

        MetricTree mTree = (MetricTree)e.getSource();
        DefaultTreeModel dfModel = (DefaultTreeModel)mTree.getModel();
        MetricTreeNode mtNode = (MetricTreeNode)dfModel.getRoot();
        String id = mtNode.getId();
        String nodeName = getSelectedNodeName(e);

        // this gets called when the tree is initialized, prevent sending these events during that time
        List<CodeTimeToolWindow.ExpandState> nodeStates = CodeTimeToolWindow.getExpandState(id);
        // only update if the nodeStates is not null and the state doesn't match the one coming in
        boolean updateState = false;
        if (nodeStates != null) {
            for (CodeTimeToolWindow.ExpandState existingState : nodeStates) {
                if (existingState.path.toString().equals(path.toString())) {
                    if (existingState.expand != expanded) {
                        updateState = true;
                    }
                    break;
                }
            }
        }
        if (updateState) {
            // label will look like "Linesadded" or ct_lines_added_toggle_node
            String toggleItemName = getToggleItem(id);
            if (toggleItemName != null) {
                UIElementEntity uiElementEntity = new UIElementEntity();
                uiElementEntity.element_location = "ct_metrics_tree";
                uiElementEntity.element_name = toggleItemName;
                uiElementEntity.cta_text = nodeName;
                EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, uiElementEntity);
            }
        }

        CodeTimeToolWindow.updateExpandState(id, path, expanded);
    }

    private static void launchFileClick(MouseEvent e) {
        MetricTree mTree = (MetricTree)e.getSource();
        if (mTree.getLeadSelectionPath() != null) {
            MetricTreeNode selectedNode = (MetricTreeNode) mTree.getLeadSelectionPath().getLastPathComponent();
            if (selectedNode != null) {
                if (selectedNode.getData() != null && selectedNode.getData() instanceof FileChangeInfo) {
                    String fsPath = ((FileChangeInfo) selectedNode.getData()).fsPath;
                    SoftwareCoUtils.launchFile(fsPath);
                } else if (selectedNode.getPath() != null && selectedNode.getData() instanceof String &&
                    String.valueOf(selectedNode.getData()).indexOf("http") != -1) {
                    // launch the commit url
                    String url = String.valueOf(selectedNode.getData());
                    BrowserUtil.browse(url);
                }
            }
        }
    }

    private static List<Map.Entry<String, FileChangeInfo>> sortByKpm() {
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());
        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {

                        Long a = entryA.getValue().kpm;
                        Long b = entryB.getValue().kpm;
                        return a.compareTo(b);
                    }
                }
        );
        return entryList;
    }

    private static List<Map.Entry<String, FileChangeInfo>> sortByKeystrokes() {
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());
        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {

                        Long a = entryA.getValue().keystrokes;
                        Long b = entryB.getValue().keystrokes;
                        return a.compareTo(b);
                    }
                }
        );
        return entryList;
    }

    private static List<Map.Entry<String, FileChangeInfo>> sortByFileSeconds() {
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());
        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {
                        Long a = entryA.getValue().duration_seconds;
                        Long b = entryB.getValue().duration_seconds;
                        return a.compareTo(b);
                    }
                }
        );
        return entryList;
    }

}
