package com.softwareco.intellij.plugin.tree;

import com.intellij.ide.BrowserUtil;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.models.FileChangeInfo;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TreeHelper {

    public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHIUB_SIGNUP_ID = "github";
    public static final String EMAIL_SIGNUP_ID = "email";
    public static final String LOGGED_IN_ID = "logged_in";
    public static final String LEARN_MORE_ID = "learn_more";
    public static final String SEND_FEEDBACK_ID = "send_feedback";
    public static final String ADVANCED_METRICS_ID = "advanced_metrics";
    public static final String TOGGLE_METRICS_ID = "toggle_metrics";
    public static final String VIEW_SUMMARY_ID = "view_summary";
    public static final String CODETIME_PARENT_ID = "codetime_parent";
    public static final String CODETIME_TODAY_ID = "codetime_today";
    public static final String ACTIVE_CODETIME_PARENT_ID = "active_codetime_parent";
    public static final String ACTIVE_CODETIME_TODAY_ID = "active_codetime_today";
    public static final String ACTIVE_CODETIME_AVG_TODAY_ID = "active_codetime_avg_today";
    public static final String ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID = "active_codetime_global_avg_today";

    public static final String LINES_ADDED_TODAY_ID = "lines_added_today";
    public static final String LINES_ADDED_AVG_TODAY_ID = "lines_added_avg_today";
    public static final String LINES_ADDED_GLOBAL_AVG_TODAY_ID = "lines_added_global_avg_today";

    public static final String LINES_DELETED_TODAY_ID = "lines_deleted_today";
    public static final String LINES_DELETED_AVG_TODAY_ID = "lines_deleted_avg_today";
    public static final String LINES_DELETED_GLOBAL_AVG_TODAY_ID = "lines_deleted_global_avg_today";

    public static final String KEYSTROKES_TODAY_ID = "keystrokes_today";
    public static final String KEYSTROKES_AVG_TODAY_ID = "keystrokes_avg_today";
    public static final String KEYSTROKES_GLOBAL_AVG_TODAY_ID = "keystrokes_global_avg_today";

    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");

    public static List<MetricTreeNode> buildSignupNodes() {
        List<MetricTreeNode> list = new ArrayList<MetricTreeNode>();
        String name = FileManager.getItem("name");
        if (name == null || name.equals("")) {
            list.add(buildSignupNode("google"));
            list.add(buildSignupNode("github"));
            list.add(buildSignupNode("email"));
        } else {
            list.add(buildLoggedInNode());
        }
        return list;
    }

    private static MetricTreeNode buildSignupNode(String type) {
        String iconName = "envelope.svg";
        String text = "Sign up with email";
        String id = EMAIL_SIGNUP_ID;
        if (type.equals("google")) {
            iconName = "icons8-google.svg";
            text = "Sign up with Google";
            id = GOOGLE_SIGNUP_ID;
        } else if (type.equals("github")) {
            iconName = "icons8-github.svg";
            text = "Sign up with GitHub";
            id = GITHIUB_SIGNUP_ID;
        }
        MetricTreeNode node = new MetricTreeNode(text, iconName, id);
        return node;
    }

    public static MetricTreeNode buildLoggedInNode() {
        String authType = FileManager.getItem("authType");
        String name = FileManager.getItem("name");
        String iconName = "envelope.svg";
        if ("google".equals(authType)) {
            iconName = "icons8-google.svg";
        } else if ("github".equals(authType)) {
            iconName = "icons8-github.svg";
        }

        MetricTreeNode node = new MetricTreeNode(name, iconName, LOGGED_IN_ID);
        return node;
    }

    public static List<MetricTreeNode> buildMenuNodes() {
        List<MetricTreeNode> list = new ArrayList<>();

        String toggleText = "Hide status bar metrics";
        if (!SoftwareCoUtils.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }

        MetricTreeNode toggleNode = new MetricTreeNode(toggleText, "visible.svg", TOGGLE_METRICS_ID);

        list.add(new MetricTreeNode("Learn more", "readme.svg", LEARN_MORE_ID));
        list.add(toggleNode);
        list.add(new MetricTreeNode("Submit feedback", "message.svg", SEND_FEEDBACK_ID));

        // add a separator
        list.add(new MetricTreeNode(true));

        list.add(new MetricTreeNode("See advanced metrics", "paw-grey.svg", ADVANCED_METRICS_ID));
        list.add(new MetricTreeNode("View summary", "dashboard.svg", VIEW_SUMMARY_ID));

        // add a separator
        list.add(new MetricTreeNode(true));

        return list;
    }

    public static MetricTreeNode buildActiveCodeTimeTree(MetricLabels mLabels) {
        MetricTreeNode treeNode = new MetricTreeNode("Active code time", null, ACTIVE_CODETIME_PARENT_ID);
        treeNode.add(new MetricTreeNode(mLabels.activeCodeTime, "rocket.svg", ACTIVE_CODETIME_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.activeCodeTimeAvg, mLabels.activeCodeTimeAvgIcon, ACTIVE_CODETIME_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.activeCodeTimeGlobalAvg, "global-grey.svg", ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID));
        return treeNode;
    }

    public static MetricTreeNode buildCodeTimeTree(MetricLabels mLabels) {
        MetricTreeNode treeNode = new MetricTreeNode("Code time", null, CODETIME_PARENT_ID);
        treeNode.add(new MetricTreeNode(mLabels.codeTime, "rocket.svg", CODETIME_TODAY_ID));
        return treeNode;
    }

    public static MetricTreeNode buildLinesAddedTree(MetricLabels mLabels) {
        // create the lines added nodes
        MetricTreeNode treeNode = new MetricTreeNode("Lines added", null, null);
        treeNode.add(new MetricTreeNode(mLabels.linesAdded, "rocket.svg", LINES_ADDED_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesAddedAvg, mLabels.linesAddedAvgIcon, LINES_ADDED_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesAddedGlobalAvg, "global-grey.svg", LINES_ADDED_GLOBAL_AVG_TODAY_ID));

        return treeNode;
    }

    public static MetricTreeNode buildLinesRemovedTree(MetricLabels mLabels) {
        // create the lines removed nodes
        MetricTreeNode treeNode = new MetricTreeNode("Lines removed", null, null);
        treeNode.add(new MetricTreeNode(mLabels.linesRemoved, "rocket.svg", LINES_DELETED_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesRemovedAvg, mLabels.linesRemovedAvgIcon, LINES_DELETED_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesRemovedGlobalAvg, "global-grey.svg", LINES_DELETED_GLOBAL_AVG_TODAY_ID));
        return treeNode;
    }

    public static MetricTreeNode buildKeystrokesTree(MetricLabels mLabels) {
        // create the keystrokes nodes
        MetricTreeNode treeNode = new MetricTreeNode("Keystrokes", null, null);
        treeNode.add(new MetricTreeNode(mLabels.keystrokes, "rocket.svg", KEYSTROKES_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.keystrokesAvg, mLabels.keystrokesAvgIcon, KEYSTROKES_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.keystrokesGlobalAvg, "global-grey.svg", KEYSTROKES_GLOBAL_AVG_TODAY_ID));

        return treeNode;
    }

    public static MetricTreeNode buildTopKeystrokesFilesTree(Map<String, FileChangeInfo> fileChangeInfoMap) {
        return buildTopFilesTree("Top files by keystrokes", "keystrokes", fileChangeInfoMap);
    }

    public static MetricTreeNode buildTopKpmFilesTree(Map<String, FileChangeInfo> fileChangeInfoMap) {
        return buildTopFilesTree("Top files by KPM", "kpm", fileChangeInfoMap);
    }

    public static MetricTreeNode buildTopCodeTimeFilesTree(Map<String, FileChangeInfo> fileChangeInfoMap) {
        return buildTopFilesTree("Top files by code time", "codetime", fileChangeInfoMap);
    }

    private static MetricTreeNode buildTopFilesTree(String parentName, String sortBy, Map<String, FileChangeInfo> fileChangeInfoMap) {
        MetricTreeNode treeNode = new MetricTreeNode(parentName, null, getTopFileParentId(sortBy));

        addNodesToTopFilesMetricParentTreeNode(treeNode, sortBy, fileChangeInfoMap);

        return treeNode;
    }

    public static void addNodesToTopFilesMetricParentTreeNode(MetricTreeNode treeNode, String sortBy, Map<String, FileChangeInfo> fileChangeInfoMap) {
        // build the most edited files nodes
        // sort the fileChangeInfoMap based on keystrokes
        List<Map.Entry<String, FileChangeInfo>> entryList = null;
        if (!fileChangeInfoMap.isEmpty()) {
            if (sortBy.equals("kpm")) {
                entryList = sortByKpm(fileChangeInfoMap);
            } else if (sortBy.equals("keystrokes")) {
                entryList = sortByKeystrokes(fileChangeInfoMap);
            } else if (sortBy.equals("codetime")) {
                entryList = sortByFileSeconds(fileChangeInfoMap);
            } else {
                entryList = new ArrayList<>(fileChangeInfoMap.entrySet());
            }
        }

        if (entryList != null && entryList.size() > 0) {
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
                    val = SoftwareCoUtils.humanizeMinutes((int) (fileChangeInfoEntry.getValue().duration_seconds / 60));
                }

                String label = name + " | " + val;
                MetricTreeNode editedFileNode = new MetricTreeNode(label, "files.svg", getTopFilesId(name, sortBy));
                editedFileNode.setData(fileChangeInfoEntry.getValue());
                treeNode.add(editedFileNode);
                count++;
            }
        } else {
            MetricTreeNode node = new MetricTreeNode("<empty>", "files.svg", null);
            treeNode.add(node);
        }
    }

    public static String getTopFileParentId(String sortBy) {
        return sortBy.toLowerCase() + "_topfiles_parent";
    }

    public static String getTopFilesId(String name, String sortBy) {
        String id = name.replaceAll("\\s", "_") + "_" + sortBy;
        return id.toLowerCase();
    }

    private static void launchFileClick(MetricTreeNode selectedNode) {
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

    public static void handleClickEvent(MetricTreeNode node) {
        switch (node.getId()) {
            case GOOGLE_SIGNUP_ID:
                SoftwareCoSessionManager.launchLogin("google", UIInteractionType.click);
                break;
            case GITHIUB_SIGNUP_ID:
                SoftwareCoSessionManager.launchLogin("github", UIInteractionType.click);
                break;
            case EMAIL_SIGNUP_ID:
                SoftwareCoSessionManager.launchLogin("email", UIInteractionType.click);
                break;
            case LOGGED_IN_ID:
                break;
            case VIEW_SUMMARY_ID:
                SoftwareCoUtils.launchCodeTimeMetricsDashboard(UIInteractionType.click);
                break;
            case TOGGLE_METRICS_ID:
                SoftwareCoUtils.toggleStatusBar(UIInteractionType.click);
                // CodeTimeToolWindow.updateMetrics(null, null);
                break;
            case ADVANCED_METRICS_ID:
                SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.click);
                break;
            case SEND_FEEDBACK_ID:
                SoftwareCoUtils.submitFeedback(UIInteractionType.click);
                break;
            case LEARN_MORE_ID:
                FileManager.openReadmeFile(UIInteractionType.click);
                break;
            default:
                launchFileClick(node);
                break;
        }
    }

    private static List<Map.Entry<String, FileChangeInfo>> sortByKpm(Map<String, FileChangeInfo> fileChangeInfoMap) {
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

    private static List<Map.Entry<String, FileChangeInfo>> sortByKeystrokes(Map<String, FileChangeInfo> fileChangeInfoMap) {
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

    private static List<Map.Entry<String, FileChangeInfo>> sortByFileSeconds(Map<String, FileChangeInfo> fileChangeInfoMap) {
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

    public static JSeparator getSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentY(0.0f);
        separator.setForeground(new Color(58, 86, 187));
        return separator;
    }

}