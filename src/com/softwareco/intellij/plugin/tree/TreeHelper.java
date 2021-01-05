package com.softwareco.intellij.plugin.tree;

import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.managers.AuthPromptManager;
import com.softwareco.intellij.plugin.models.FileChangeInfo;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.*;
import swdc.java.ops.model.Integration;
import swdc.java.ops.model.MetricLabel;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TreeHelper {

    public static final String SIGN_UP_ID = "signup";
    public static final String LOG_IN_ID = "login";
    public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHUB_SIGNUP_ID = "github";
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

    public static final String SWITCH_ACCOUNT_ID = "switch_account";

    public static final String SLACK_WORKSPACES_NODE_ID = "slack_workspaces_node";
    public static final String SWITCH_OFF_DARK_MODE_ID = "switch_off_dark_mode";
    public static final String SWITCH_ON_DARK_MODE_ID = "switch_ON_dark_mode";
    public static final String TOGGLE_DOCK_POSITION_ID = "toggle_dock_position";
    public static final String SWITCH_OFF_DND_ID = "switch_off_dnd";
    public static final String SWITCH_ON_DND_ID = "switch_on_dnd";
    public static final String CONNECT_SLACK_ID = "connect_slack";
    public static final String ADD_WORKSPACE_ID = "add_workspace";
    public static final String SET_PRESENCE_AWAY_ID = "set_presence_away";
    public static final String SET_PRESENCE_ACTIVE_ID = "set_presence_active";

    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");

    public static List<MetricTreeNode> buildSignupNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        String name = FileUtilManager.getItem("name");
        if (StringUtils.isBlank(name)) {
            list.add(new MetricTreeNode("Sign up", "signup.svg", SIGN_UP_ID));
            list.add(new MetricTreeNode("Log in", "paw.svg", LOG_IN_ID));
        } else {
            list.add(buildLoggedInNode());
        }
        return list;
    }

    private static MetricTreeNode buildSignupNode(String type) {
        String iconName = "email.svg";
        String text = "Sign up with email";
        String id = EMAIL_SIGNUP_ID;
        if (type.equals("google")) {
            iconName = "google.svg";
            text = "Sign up with Google";
            id = GOOGLE_SIGNUP_ID;
        } else if (type.equals("github")) {
            iconName = "github.svg";
            text = "Sign up with GitHub";
            id = GITHUB_SIGNUP_ID;
        }
        MetricTreeNode node = new MetricTreeNode(text, iconName, id);
        return node;
    }

    public static MetricTreeNode buildLoggedInNode() {
        String authType = FileUtilManager.getItem("authType");
        String name = FileUtilManager.getItem("name");
        String iconName = "email.svg";
        if ("google".equals(authType)) {
            iconName = "google.svg";
        } else if ("github".equals(authType)) {
            iconName = "github.svg";
        }

        MetricTreeNode node = new MetricTreeNode(name, iconName, LOGGED_IN_ID);
        node.add(new MetricTreeNode("Switch account", "paw.svg", SWITCH_ACCOUNT_ID));
        return node;
    }

    public static List<MetricTreeNode> buildMenuNodes() {
        List<MetricTreeNode> list = new ArrayList<>();

        String toggleText = "Hide status bar metrics";
        if (!SoftwareCoUtils.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }

        list.add(new MetricTreeNode("Learn more", "readme.svg", LEARN_MORE_ID));
        list.add(new MetricTreeNode("Submit feedback", "message.svg", SEND_FEEDBACK_ID));
        list.add(new MetricTreeNode(toggleText, "visible.svg", TOGGLE_METRICS_ID));

        list.add(buildSlackWorkspacesNode());

        return list;
    }

    public static MetricTreeNode buildSummaryButton() {
        return new MetricTreeNode("Dashboard", "dashboard.svg", VIEW_SUMMARY_ID);
    }

    public static MetricTreeNode buildViewWebDashboardButton() {
        return new MetricTreeNode("More data at Software.com", "paw-grey.svg", ADVANCED_METRICS_ID);
    }

    public static List<MetricTreeNode> buildTreeFlowNodes() {
        List<MetricTreeNode> list = new ArrayList<>();

        SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();

        // snooze node
        if (slackDndInfo.snooze_enabled) {
            list.add(getUnPausenotificationsNode(slackDndInfo));
        } else {
            list.add(getPauseNotificationsNode());
        }
        // presence toggle
        SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();
        if (slackUserPresence != null && slackUserPresence.presence.equals("active")) {
            list.add(getSetAwayPresenceNode());
        } else {
            list.add(getSetActivePresenceNode());
        }

        if (UtilManager.isMac()) {
            if (AppleScriptManager.isDarkMode()) {
                list.add(getSwitchOffDarkModeNode());
            } else {
                list.add(getSwitchOnDarkModeNode());
            }

            list.add(new MetricTreeNode("Toggle dock position", "position.svg", TOGGLE_DOCK_POSITION_ID));
        }

        return list;
    }

    public static MetricTreeNode getSwitchOffDarkModeNode() {
        return new MetricTreeNode("Turn off dark mode", "light-mode.svg", SWITCH_OFF_DARK_MODE_ID);
    }

    public static MetricTreeNode getSwitchOnDarkModeNode() {
        return new MetricTreeNode("Turn on dark mode", "dark-mode.svg", SWITCH_ON_DARK_MODE_ID);
    }

    public static MetricTreeNode getPauseNotificationsNode() {
        return new MetricTreeNode("Pause notifications", "notifications-on.svg", SWITCH_OFF_DND_ID);
    }

    public static MetricTreeNode getUnPausenotificationsNode(SlackDndInfo slackDndInfo) {
        String endTimeOfDay = UtilManager.getTimeOfDay(UtilManager.getJavaDateFromSeconds(slackDndInfo.snooze_endtime));
        return new MetricTreeNode("Turn on notifications (ends at " + endTimeOfDay + ")", "notifications-off.svg", SWITCH_ON_DND_ID);
    }

    public static MetricTreeNode getSetAwayPresenceNode() {
        return new MetricTreeNode("Set presence to away", "presence.svg", SET_PRESENCE_AWAY_ID);
    }

    public static MetricTreeNode getSetActivePresenceNode() {
        return new MetricTreeNode("Set presence to active", "presence.svg", SET_PRESENCE_ACTIVE_ID);
    }

    public static MetricTreeNode buildSlackWorkspacesNode() {
        MetricTreeNode node = new MetricTreeNode("Slack workspaces", null, SLACK_WORKSPACES_NODE_ID);
        List<Integration> workspaces = SlackManager.getSlackWorkspaces();
        workspaces.forEach(workspace -> {
            node.add(new MetricTreeNode(workspace.team_domain, "slack.svg", workspace.authId));
        });
        // add the add new workspace button
        node.add(getAddSlackWorkspaceNode());
        return node;
    }

    public static MetricTreeNode getAddSlackWorkspaceNode() {
        return new MetricTreeNode("Add workspace", "add.svg", ADD_WORKSPACE_ID);
    }

    public static MetricTreeNode buildActiveCodeTimeTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.activeCodeTime, mLabels.activeCodeTimeAvgIcon, ACTIVE_CODETIME_TODAY_ID);
    }

    public static MetricTreeNode buildCodeTimeTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.codeTime, "rocket.svg", CODETIME_TODAY_ID);
    }

    public static MetricTreeNode buildLinesAddedTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.linesAdded, mLabels.linesAddedAvgIcon, LINES_ADDED_TODAY_ID);
    }

    public static MetricTreeNode buildLinesRemovedTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.linesRemoved, mLabels.linesRemovedAvgIcon, LINES_DELETED_TODAY_ID);
    }

    public static MetricTreeNode buildKeystrokesTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.keystrokes, mLabels.keystrokesAvgIcon, KEYSTROKES_TODAY_ID);
    }


    private static void launchFileClick(MetricTreeNode selectedNode) {
        if (selectedNode != null) {
            if (selectedNode.getData() != null && selectedNode.getData() instanceof FileChangeInfo) {
                String fsPath = ((FileChangeInfo) selectedNode.getData()).fsPath;
                SoftwareCoUtils.launchFile(fsPath);
            } else if (selectedNode.getPath() != null && selectedNode.getData() instanceof String && String.valueOf(selectedNode.getData()).contains("http")) {
                // launch the commit url
                String url = String.valueOf(selectedNode.getData());

                UtilManager.launchUrl(url);
            }
        }
    }

    public static void handleClickEvent(MetricTreeNode node) {
        switch (node.getId()) {
            case SIGN_UP_ID:
                AuthPromptManager.initiateSignupFlow();
                break;
            case LOG_IN_ID:
                AuthPromptManager.initiateLoginFlow();
                break;
            case GOOGLE_SIGNUP_ID:
                SoftwareCoSessionManager.launchLogin("google", UIInteractionType.click, false);
                break;
            case GITHUB_SIGNUP_ID:
                SoftwareCoSessionManager.launchLogin("github", UIInteractionType.click, false);
                break;
            case EMAIL_SIGNUP_ID:
                SoftwareCoSessionManager.launchLogin("email", UIInteractionType.click, false);
                break;
            case LOGGED_IN_ID:
                CodeTimeToolWindow.expandCollapse(LOGGED_IN_ID);
                break;
            case SWITCH_ACCOUNT_ID:
                AuthPromptManager.initiateSwitchAccountFlow();
                break;
            case VIEW_SUMMARY_ID:
                SoftwareCoUtils.launchCodeTimeMetricsDashboard(UIInteractionType.click);
                break;
            case TOGGLE_METRICS_ID:
                SoftwareCoUtils.toggleStatusBar(UIInteractionType.click);
                CodeTimeToolWindow.updateMetrics(null, null);
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
            case CONNECT_SLACK_ID:
            case ADD_WORKSPACE_ID:
                SlackManager.connectSlackWorkspace(() -> {CodeTimeToolWindow.rebuildTree();});
                break;
            case SWITCH_OFF_DARK_MODE_ID:
            case SWITCH_ON_DARK_MODE_ID:
                AppleScriptManager.toggleDarkMode(() -> {CodeTimeToolWindow.rebuildTree();});
                break;
            case SWITCH_OFF_DND_ID:
                SlackManager.pauseSlackNotifications(() -> {CodeTimeToolWindow.rebuildTree();});
                break;
            case SWITCH_ON_DND_ID:
                SlackManager.enableSlackNotifications(() -> {CodeTimeToolWindow.rebuildTree();});
                break;
            case SET_PRESENCE_ACTIVE_ID:
                SlackManager.toggleSlackPresence("auto", () -> {CodeTimeToolWindow.rebuildTree();});
                break;
            case SET_PRESENCE_AWAY_ID:
                SlackManager.toggleSlackPresence("away", () -> {CodeTimeToolWindow.rebuildTree();});
                break;
            case TOGGLE_DOCK_POSITION_ID:
                AppleScriptManager.toggleDock();
                break;
        }
    }

    public static JSeparator getSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentY(0.0f);
        separator.setForeground(new Color(58, 86, 187));
        return separator;
    }

}