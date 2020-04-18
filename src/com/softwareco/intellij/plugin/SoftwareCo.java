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
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.softwareco.intellij.plugin.managers.EventManager;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.managers.WallClockManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


/**
 * Intellij Plugin Application
 */
public class SoftwareCo implements ApplicationComponent {

    public static JsonParser jsonParser = new JsonParser();
    public static final Logger log = Logger.getLogger("SoftwareCo");
    public static Gson gson;

    public static MessageBusConnection connection;

    private SoftwareCoEventManager eventMgr = SoftwareCoEventManager.getInstance();
    private AsyncManager asyncManager = AsyncManager.getInstance();

    private static int retry_counter = 0;

    public SoftwareCo() {
    }

    private static IdeaPluginDescriptor getIdeaPluginDescriptor() {
        IdeaPluginDescriptor[] desriptors = PluginManager.getPlugins();
        if (desriptors != null && desriptors.length > 0) {
            for (int i = 0; i < desriptors.length; i++) {
                IdeaPluginDescriptor descriptor = desriptors[i];
                if (descriptor.getPluginId().getIdString().equals("com.softwareco.intellij.plugin")) {
                    return descriptor;
                }
            }
        }
        return null;
    }

    public static String getVersion() {
        if (SoftwareCoUtils.VERSION == null) {
            IdeaPluginDescriptor pluginDescriptor = getIdeaPluginDescriptor();
            if (pluginDescriptor != null) {
                SoftwareCoUtils.VERSION = pluginDescriptor.getVersion();
            } else {
                return "2.0.1";
            }
        }
        return SoftwareCoUtils.VERSION;
    }

    public static String getPluginName() {
        if (SoftwareCoUtils.pluginName == null) {
            IdeaPluginDescriptor pluginDescriptor = getIdeaPluginDescriptor();
            if (pluginDescriptor != null) {
                SoftwareCoUtils.pluginName = pluginDescriptor.getName();
            } else {
                return "Code Time";
            }
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
                final Runnable service = () -> initComponent();
                AsyncManager.getInstance().executeOnceInSeconds(service, 60 * 10);
            } else {
                getPluginName();
                // create the anon user
                String jwt = SoftwareCoUtils.createAnonymousUser(serverIsOnline);
                if (jwt == null) {
                    // it failed, try again later
                    if (retry_counter == 0) {
                        SoftwareCoUtils.showOfflinePrompt(true);
                    }
                    final Runnable service = () -> initComponent();
                    AsyncManager.getInstance().executeOnceInSeconds(service, 60 * 10);
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

        log.info(plugName + ": Finished initializing SoftwareCo plugin");

        initializeUserInfoWhenProjectsReady(initializedUser);
    }

    private void initializeUserInfoWhenProjectsReady(boolean initializedUser) {
        Project p = SoftwareCoUtils.getOpenProject();
        if (p == null) {
            // try again in 5 seconds
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    initializeUserInfoWhenProjectsReady(initializedUser);
                }
            }, 5000);
        } else {
            // store the activate event
            EventManager.createCodeTimeEvent("resource", "load", "EditorActivate");
            initializeUserInfo(initializedUser);

            setupFileEditorEventListeners(p);
        }
    }

    private void sendOfflineDataRunner() {
        new Thread(() -> {
            try {
                FileManager.sendOfflineData(false);
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }

    private void processRepoTasks() {
        Project p = SoftwareCoUtils.getFirstActiveProject();
        if (p != null) {
            SoftwareCoRepoManager repoMgr = SoftwareCoRepoManager.getInstance();
            repoMgr.getHistoricalCommits(p.getBasePath());

            repoMgr.processRepoMembersInfo(p.getBasePath());
        }

    }

    private void processHourlyTasks() {
        SoftwareCoUtils.sendHeartbeat("HOURLY");
        // send the events data
        EventManager.sendOfflineEvents();
    }

    // The app is ready and has a selected project
    private void initializeUserInfo(boolean initializedUser) {

        if (initializedUser) {
            // send an initial plugin payload
            this.sendInstallPayload();
            FileManager.openReadmeFile();
        }

        // setup the doc listeners
        setupEventListeners();
        // check the logged in status
        SoftwareCoUtils.getLoggedInStatus();

        // every 20 min
        final Runnable repoTaskRunner = () -> this.processRepoTasks();
        asyncManager.scheduleService(
                repoTaskRunner, "repoTaskRunner", 90, 60 * 20);

        // every 15 minutes
        final Runnable sendOfflineDataRunner = () -> this.sendOfflineDataRunner();
        asyncManager.scheduleService(sendOfflineDataRunner, "offlineDataRunner", 2, 60 * 15);

        // every hour
        final Runnable hourlyTaskRunner = () -> this.processHourlyTasks();
        asyncManager.scheduleService(hourlyTaskRunner, "hourlyTaskRunner", 120, 60 * 60);

        // initialize the wallclock manager
        WallClockManager.getInstance();
    }

    protected void sendInstallPayload() {
        KeystrokeManager keystrokeManager = KeystrokeManager.getInstance();
        String fileName = "Untitled";
        // String projectName, String fileName, String projectFilepath
        eventMgr.initializeKeystrokeCount("Unnamed", fileName, "Untitled");
        KeystrokeCount.FileInfo fileInfo = keystrokeManager.getKeystrokeCount().getSourceByFileName(fileName);
        fileInfo.add = fileInfo.add + 1;
        fileInfo.netkeys = fileInfo.add - fileInfo.delete;
        keystrokeManager.getKeystrokeCount().keystrokes = 1;
        keystrokeManager.getKeystrokeCount().processKeystrokes();
    }

    // add the document change event listener
    private void setupEventListeners() {
        ApplicationManager.getApplication().invokeLater(() -> {
            // edit document
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
                    new SoftwareCoDocumentListener(), this::disposeComponent);
        });
    }

    // add the file selection change event listener
    private void setupFileEditorEventListeners(Project p) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // file open,close,selection listener
            p.getMessageBus().connect().subscribe(
                    FileEditorManagerListener.FILE_EDITOR_MANAGER, new SoftwareCoFileEditorListener());
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