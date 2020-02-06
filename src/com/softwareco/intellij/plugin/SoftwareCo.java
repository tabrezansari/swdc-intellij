/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.softwareco.intellij.plugin.event.EventManager;
import com.softwareco.intellij.plugin.sessiondata.SessionDataManager;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;

import java.util.logging.Logger;


/**
 * Intellij Plugin Application
 * ....
 */
public class SoftwareCo implements ApplicationComponent {

    public static JsonParser jsonParser = new JsonParser();
    public static final Logger log = Logger.getLogger("SoftwareCo");
    public static Gson gson;

    public static MessageBusConnection connection;

    private SoftwareCoSessionManager sessionMgr = SoftwareCoSessionManager.getInstance();
    private SoftwareCoEventManager eventMgr = SoftwareCoEventManager.getInstance();
    private AsyncManager asyncManager = AsyncManager.getInstance();

    private static int retry_counter = 0;
    private static long check_online_interval_ms = 1000 * 60 * 10;

    public SoftwareCo() {
    }

    public static String getVersion() {
        if (SoftwareCoUtils.VERSION == null) {
            IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(PluginId.getId("com.softwareco.intellij.plugin"));

            SoftwareCoUtils.VERSION = pluginDescriptor.getVersion();
        }
        return SoftwareCoUtils.VERSION;
    }

    public static String getPluginName() {
        if (SoftwareCoUtils.pluginName == null) {
            IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(PluginId.getId("com.softwareco.intellij.plugin"));

            SoftwareCoUtils.pluginName = pluginDescriptor.getName();
        }
        return SoftwareCoUtils.pluginName;
    }

    public void initComponent() {
        boolean serverIsOnline = SoftwareCoSessionManager.isServerOnline();
        boolean sessionFileExists = SoftwareCoSessionManager.softwareSessionFileExists();
        boolean jwtExists = SoftwareCoSessionManager.jwtExists();
        if (!sessionFileExists || !jwtExists) {
            if (!serverIsOnline) {
                // server isn't online, check again in 10 min
                if (retry_counter == 0) {
                    SoftwareCoUtils.showOfflinePrompt(true);
                }
                new Thread(() -> {
                    try {
                        Thread.sleep(check_online_interval_ms);
                        initComponent();
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }).start();
            } else {
                getPluginName();
                // create the anon user
                String jwt = SoftwareCoUtils.createAnonymousUser(serverIsOnline);
                if (jwt == null) {
                    // it failed, try again later
                    if (retry_counter == 0) {
                        SoftwareCoUtils.showOfflinePrompt(true);
                    }
                    new Thread(() -> {
                        try {
                            Thread.sleep(check_online_interval_ms);
                            initComponent();
                        } catch (Exception e) {
                            System.err.println(e);
                        }
                    }).start();
                } else {
                    initializePlugin(true);
                }
            }
        } else {
            // session json already exists, continue with plugin init
            initializePlugin(false);
        }
    }

    protected void initializePlugin(boolean initializedUser) {
        String plugName = getPluginName();

        log.info(plugName + ": Loaded v" + getVersion());

        gson = new Gson();

        setupEventListeners();

        log.info(plugName + ": Finished initializing SoftwareCo plugin");

        // store the activate event
        EventManager.createCodeTimeEvent("resource", "load", "EditorActivate");

        final Runnable hourlyRunner = () -> this.processHourlyJobs();
        asyncManager.scheduleService(
                hourlyRunner, "hourlyJobsRunner", 45, 60 * 60);

        final Runnable userStatusRunner = () -> SoftwareCoUtils.getUserStatus();
        asyncManager.scheduleService(
                userStatusRunner, "userStatusRunner", 60, 60 * 3);

        // every 30 minutes
        final Runnable sendOfflineDataRunner = () -> this.sendOfflineDataRunner();
        asyncManager.scheduleService(sendOfflineDataRunner, "offlineDataRunner", 2, 60 * 30);

        eventMgr.setAppIsReady(true);

        initializeUserInfoWhenProjectsReady(initializedUser);

    }

    private void initializeUserInfoWhenProjectsReady(boolean initializedUser) {
        Project p = SoftwareCoUtils.getOpenProject();
        if (p == null) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    initializeUserInfoWhenProjectsReady(initializedUser);
                } catch (Exception e) {
                    System.err.println(e);
                }
            }).start();
        } else {
            initializeUserInfo(initializedUser);
        }
    }

    private void sendOfflineDataRunner() {
        new Thread(() -> {
            try {
                SoftwareCoSessionManager.getInstance().sendOfflineData();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }

    private void processHourlyJobs() {
        SoftwareCoUtils.sendHeartbeat("HOURLY");

        SoftwareCoRepoManager repoMgr = SoftwareCoRepoManager.getInstance();
        new Thread(() -> {
            try {
                Thread.sleep(60000);
                repoMgr.getHistoricalCommits(getRootPath());
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }

    private void initializeUserInfo(boolean initializedUser) {

        SoftwareCoUtils.getUserStatus();

        if (initializedUser) {
            // send an initial plugin payload
            this.sendInstallPayload();

            // ask the user to login one time only
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    sessionMgr.showLoginPrompt();
                }
                catch (Exception e){
                    System.err.println(e);
                }
            }).start();
        }

        new Thread(() -> {
            try {
                SessionDataManager.fetchSessionSummary(true);
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();

        // start the wallclock
        WallClockManager.getInstance();
    }

    protected void sendInstallPayload() {
        KeystrokeManager keystrokeManager = KeystrokeManager.getInstance();
        String fileName = "Untitled";
        eventMgr.initializeKeystrokeObjectGraph(fileName, "Unnamed", "");
        KeystrokeCount.FileInfo fileInfo = keystrokeManager.getKeystrokeCount().getSourceByFileName(fileName);
        fileInfo.add = fileInfo.add + 1;
        fileInfo.netkeys = fileInfo.add - fileInfo.delete;
        keystrokeManager.getKeystrokeCount().setKeystrokes(1);
        keystrokeManager.getKeystrokeCount().processKeystrokes();
    }

    protected String getRootPath() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects != null && projects.length > 0) {
            return projects[0].getBasePath();
        }
        return null;
    }

    public static Project getActiveProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects != null && projects.length > 0) {
            return projects[0];
        }
        return null;
    }

    private void setupEventListeners() {
        ApplicationManager.getApplication().invokeLater(() -> {

            // save file
            MessageBus bus = ApplicationManager.getApplication().getMessageBus();
            connection = bus.connect();
            connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new SoftwareCoFileEditorListener());

            // edit document
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new SoftwareCoDocumentListener());
        });
    }


    public void disposeComponent() {
        // store the activate event
        EventManager.createCodeTimeEvent("resource", "unload", "EditorDeactivate");

        try {
            if (connection != null) {
                connection.disconnect();
            }
        } catch(Exception e) {
            log.info("Error disconnecting the software.com plugin, reason: " + e.toString());
        }

        asyncManager.destroyServices();

        // process one last time
        // this will ensure we process the latest keystroke updates
        KeystrokeManager keystrokeManager = KeystrokeManager.getInstance();
        if (keystrokeManager.getKeystrokeCount() != null) {
            keystrokeManager.getKeystrokeCount().processKeystrokes();
        }
    }


}