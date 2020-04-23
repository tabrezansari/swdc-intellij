/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.softwareco.intellij.plugin.managers.EventManager;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindowFactory;
import org.apache.http.client.methods.HttpGet;

import java.awt.event.MouseEvent;
import java.io.*;
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

    public static boolean softwareSessionFileExists() {
        // don't auto create the file
        String file = getSoftwareSessionFile(false);
        // check if it exists
        File f = new File(file);
        return f.exists();
    }

    public static boolean jwtExists() {
        String jwt = FileManager.getItem("jwt");
        return (jwt != null && !jwt.equals("")) ? true : false;
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
            file += "\\jetbrainsCt_README.md";
        } else {
            file += "/jetbrainsCt_README.md";
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
    };

    public static String getSoftwareSessionFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\session.json";
        } else {
            file += "/session.json";
        }
        return file;
    }

    public synchronized static boolean isServerOnline() {
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        // 5 min threshold
        boolean pastThreshold = (nowInSec - lastAppAvailableCheck > (60 * 5)) ? true : false;
        if (pastThreshold) {
            SoftwareResponse resp = SoftwareCoUtils.makeApiCall("/ping", HttpGet.METHOD_NAME, null);
            SoftwareCoUtils.updateServerStatus(resp.isOk());
            lastAppAvailableCheck = nowInSec;
        }
        return SoftwareCoUtils.isAppAvailable();
    }

    private Project getCurrentProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects != null && projects.length > 0) {
            return projects[0];
        }
        return null;
    }

    public void statusBarClickHandler(MouseEvent mouseEvent, String id) {
        EventManager.createCodeTimeEvent(
                "mouse",
                "click",
                "ShowTreeView"
        );
        CodeTimeToolWindowFactory.openToolWindow();
    }

    protected static void lazilyFetchUserStatus(int retryCount) {
        boolean loggedIn = SoftwareCoUtils.getLoggedInStatus();

        if (!loggedIn && retryCount > 0) {
            final int newRetryCount = retryCount - 1;

            final Runnable service = () -> lazilyFetchUserStatus(newRetryCount);
            AsyncManager.getInstance().executeOnceInSeconds(service, 10);
        } else {
            // prompt they've completed the setup
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    // ask to download the PM
                    Messages.showInfoMessage("Successfully logged onto Code Time", "Code Time Setup Complete");

                    FileManager.sendOfflineData();
                }
            });
        }
    }

    public static void launchLogin(String loginType) {
        String jwt = FileManager.getItem("jwt");

        String url = "";
        if (loginType == null || loginType.equals("software") || loginType.equals("email")) {
            url = SoftwareCoUtils.launch_url + "/email-signup?token=" + jwt + "&plugin=codetime&auth=software";
        } else if (loginType.equals("google")) {
            url = SoftwareCoUtils.api_endpoint + "/auth/google?token=" + jwt + "&plugin=codetime&redirect=" + SoftwareCoUtils.launch_url;
        } else if (loginType.equals("github")) {
            url = SoftwareCoUtils.api_endpoint + "/auth/github?token=" + jwt + "&plugin=codetime&redirect=" + SoftwareCoUtils.launch_url;
        }

        FileManager.setItem("authType", loginType);

        BrowserUtil.browse(url);

        final Runnable service = () -> lazilyFetchUserStatus(20);
        AsyncManager.getInstance().executeOnceInSeconds(service, 10);
    }

    public static void launchWebDashboard() {
        String url = SoftwareCoUtils.launch_url + "/login";
        BrowserUtil.browse(url);

        EventManager.createCodeTimeEvent(
                "mouse",
                "click",
                "LaunchWebDashboard"
        );
    }
}
