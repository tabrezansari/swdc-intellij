/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.softwareco.intellij.plugin.managers.EventTrackerManager;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.managers.SessionDataManager;
import com.softwareco.intellij.plugin.managers.TimeDataManager;
import com.softwareco.intellij.plugin.models.TimeData;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindowFactory;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;

import java.io.*;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

public class SoftwareCoSessionManager {

    private static SoftwareCoSessionManager instance = null;
    public static final Logger log = Logger.getLogger("SoftwareCoSessionManager");
    private static long lastAppAvailableCheck = 0;

    public static SoftwareCoSessionManager getInstance() {
        if (instance == null) {
            instance = new SoftwareCoSessionManager();
        }
        return instance;
    }

    public static String getCodeTimeDashboardFile() {
        String dashboardFile = getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            dashboardFile += "\\CodeTime.txt";
        } else {
            dashboardFile += "/CodeTime.txt";
        }
        return dashboardFile;
    }

    public static String getReadmeFile() {
        String file = getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\jetbrainsCt_README.txt";
        } else {
            file += "/jetbrainsCt_README.txt";
        }
        return file;
    }

    public static String getSoftwareDir(boolean autoCreate) {
        String softwareDataDir = SoftwareCoUtils.getUserHomeDir();
        if (SoftwareCoUtils.isWindows()) {
            softwareDataDir += "\\.software";
        } else {
            softwareDataDir += "/.software";
        }

        File f = new File(softwareDataDir);
        if (!f.exists()) {
            // make the directory
            f.mkdirs();
        }

        return softwareDataDir;
    }

    public static String getSummaryInfoFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\SummaryInfo.txt";
        } else {
            file += "/SummaryInfo.txt";
        }
        return file;
    }

    public synchronized static boolean isServerOnline() {
        SoftwareResponse resp = SoftwareCoUtils.makeApiCall("/ping", HttpGet.METHOD_NAME, null);
        if (resp != null && resp.isOk()) {
            return true;
        }
        return false;
    }

    private Project getCurrentProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects != null && projects.length > 0) {
            return projects[0];
        }
        return null;
    }

    public void statusBarClickHandler() {
        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = "ct_status_bar_metrics_btn";
        elementEntity.element_location = "ct_status_bar";
        elementEntity.color = null;
        elementEntity.cta_text = "status bar metrics";
        elementEntity.icon_name = "clock";
        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
        CodeTimeToolWindowFactory.openToolWindow();
    }

    protected static void lazilyFetchUserStatus(int retryCount) {
        boolean establishedUser = SoftwareCoUtils.getUserLoginState();

        if (!establishedUser) {
            if (retryCount > 0) {
                final int newRetryCount = retryCount - 1;

                final Runnable service = () -> lazilyFetchUserStatus(newRetryCount);
                AsyncManager.getInstance().executeOnceInSeconds(service, 8);
            } else {
                // clear the auth callback state
                FileManager.setBooleanItem("switching_account", false);
                FileManager.setAuthCallbackState(null);
            }
        } else {
            // clear the auth callback state
            FileManager.setBooleanItem("switching_account", false);
            FileManager.setAuthCallbackState(null);

            SessionDataManager.clearSessionSummaryData();
            TimeDataManager.clearTimeDataSummary();

            // prompt they've completed the setup
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    // ask to download the PM
                    Messages.showInfoMessage("Successfully logged onto Code Time", "Code Time Setup Complete");

                    // this will fetch the session summary data and refresh the tree
                    SessionDataManager.treeDataUpdateCheck(true);
                }
            });
        }
    }

    public static void launchLogin(String loginType, UIInteractionType interactionType, boolean switching_account) {

        String auth_callback_state = UUID.randomUUID().toString();
        FileManager.setAuthCallbackState(auth_callback_state);

        FileManager.setBooleanItem("switching_account", switching_account);

        String plugin_uuid = FileManager.getPluginUuid();

        JsonObject obj = new JsonObject();
        obj.addProperty("plugin", "codetime");
        obj.addProperty("plugin_uuid", plugin_uuid);
        obj.addProperty("pluginVersion", SoftwareCoUtils.getVersion());
        obj.addProperty("plugin_id", SoftwareCoUtils.pluginId);
        obj.addProperty("auth_callback_state", auth_callback_state);
        obj.addProperty("redirect", SoftwareCoUtils.launch_url);

        String url = "";
        String element_name = "ct_sign_up_google_btn";
        String icon_name = "google";
        String cta_text = "Sign up with Google";
        String icon_color = null;
        if (loginType == null || loginType.equals("software") || loginType.equals("email")) {
            element_name = "ct_sign_up_email_btn";
            cta_text = "Sign up with email";
            icon_name = "envelope";
            icon_color = "gray";
            url = SoftwareCoUtils.launch_url + "/email-signup";
        } else if (loginType.equals("google")) {
            url = SoftwareCoUtils.api_endpoint + "/auth/google";
        } else if (loginType.equals("github")) {
            element_name = "ct_sign_up_github_btn";
            cta_text = "Sign up with GitHub";
            icon_name = "github";
            url = SoftwareCoUtils.api_endpoint + "/auth/github";
        }

        StringBuffer sb = new StringBuffer();
        Iterator<String> keys = obj.keySet().iterator();
        while(keys.hasNext()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            String key = keys.next();
            String val = obj.get(key).getAsString();
            try {
                val = URLEncoder.encode(val, "UTF-8");
            } catch (Exception e) {
                log.info("Unable to url encode value, error: " + e.getMessage());
            }
            sb.append(key).append("=").append(val);
        }
        url += "?" + sb.toString();

        FileManager.setItem("authType", loginType);

        System.out.println("URL: " + url);

        BrowserUtil.browse(url);

        // max of 5.3 minutes
        final Runnable service = () -> lazilyFetchUserStatus(40);
        AsyncManager.getInstance().executeOnceInSeconds(service, 8);

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = element_name;
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = icon_color;
        elementEntity.cta_text = cta_text;
        elementEntity.icon_name = icon_name;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }

    public static void launchWebDashboard(UIInteractionType interactionType) {
        String url = SoftwareCoUtils.launch_url + "/login";
        BrowserUtil.browse(url);

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_web_metrics_btn" : "ct_web_metrics_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = interactionType == UIInteractionType.click ? "gray" : null;
        elementEntity.cta_text = "See advanced metrics";
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "paw" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }
}
