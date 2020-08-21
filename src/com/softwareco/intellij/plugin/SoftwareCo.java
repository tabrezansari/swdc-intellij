/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.softwareco.intellij.plugin.managers.EventTrackerManager;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.managers.WallClockManager;
import com.swdc.snowplow.tracker.events.UIInteractionType;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


/**
 * Intellij Plugin Application
 */
public class SoftwareCo implements ApplicationComponent {

    public static final Logger log = Logger.getLogger("SoftwareCo");
    public static final Gson gson = new GsonBuilder().create();

    public static MessageBusConnection connection;

    private final SoftwareCoEventManager eventMgr = SoftwareCoEventManager.getInstance();
    private final AsyncManager asyncManager = AsyncManager.getInstance();

    private static final int retry_counter = 0;

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

        // send the activate event
        EventTrackerManager.getInstance().trackEditorAction("editor", "activate");

        initializeUserInfoWhenProjectsReady(initializedUser);

        log.info(plugName + ": Finished initializing SoftwareCo plugin");
    }

    /**
     * This logic waits until the user has selected a project.
     * Once that happens we can continue initializing the plugin.
     * @param initializedUser
     */
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
            // init user info
            initializeUserInfo(initializedUser);

            // setup the doc listeners
            setupFileEditorEventListeners(p);
        }
    }

    private void sendOfflineDataRunner() {
        new Thread(() -> {
            try {
                FileManager.sendOfflineData();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }

    // The app is ready and has a selected project
    private void initializeUserInfo(boolean initializedUser) {

        String readmeDisplayed = FileManager.getItem("intellij_CtReadme");
        if (readmeDisplayed == null || Boolean.valueOf(readmeDisplayed) == false) {
            // send an initial plugin payload
            this.sendInstallPayload();
            FileManager.openReadmeFile(UIInteractionType.keyboard);
            FileManager.setItem("intellij_CtReadme", "true");
        }

        // setup the doc listeners
        setupEventListeners();

        // check the logged in status
        SoftwareCoUtils.getLoggedInStatus();

        // get the last payload into memory
        FileManager.getLastSavedKeystrokeStats();

        // every 5 minutes
        final Runnable sendOfflineDataRunner = () -> this.sendOfflineDataRunner();
        asyncManager.scheduleService(sendOfflineDataRunner, "offlineDataRunner", 30, 60 * 5);

        // initialize the wallclock manager
        WallClockManager.getInstance().updateSessionSummaryFromServer();
    }

    protected void sendInstallPayload() {
        KeystrokeManager keystrokeManager = KeystrokeManager.getInstance();

        // create the keystroke count wrapper
        eventMgr.createKeystrokeCountWrapper("Unnamed", "Untitled");

        String fileName = "Untitled";
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
        EventTrackerManager.getInstance().trackEditorAction("editor", "deactivate");

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