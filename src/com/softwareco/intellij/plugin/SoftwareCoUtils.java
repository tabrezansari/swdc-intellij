/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.softwareco.intellij.plugin.managers.*;
import com.softwareco.intellij.plugin.models.FileDetails;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class SoftwareCoUtils {

    // set the api endpoint to use
    public final static String api_endpoint = "https://api.software.com";
    // set the launch url to use
    public final static String launch_url = "https://app.software.com";

    // Unnamed project name
    public final static String unnamed_project_name = "Unnamed";
    // Untitled file name or directory
    public final static String untitled_file_name = "Untitled";

    private static final Gson gson = new Gson();

    // sublime = 1, vs code = 2, eclipse = 3, intellij = 4, visual studio = 6, atom = 7
    public static int pluginId = 4;
    public static String VERSION = null;
    public static String pluginName = null;

    public final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private final static int EOF = -1;
    private static boolean showStatusText = true;
    private static final int DASHBOARD_LABEL_WIDTH = 25;
    private static final int DASHBOARD_VALUE_WIDTH = 25;

    private static final String SERVICE_NOT_AVAIL =
            "Our service is temporarily unavailable.\n\nPlease try again later.\n";
    private static String workspace_name = null;


    public static String getVersion() {
        if (VERSION == null) {
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
        if (pluginName == null) {
            IdeaPluginDescriptor pluginDescriptor = getIdeaPluginDescriptor();
            if (pluginDescriptor != null) {
                SoftwareCoUtils.pluginName = pluginDescriptor.getName();
            } else {
                return "Code Time";
            }
        }
        return SoftwareCoUtils.pluginName;
    }

    private static IdeaPluginDescriptor getIdeaPluginDescriptor() {
        IdeaPluginDescriptor[] descriptors = PluginManager.getPlugins();
        if (descriptors != null && descriptors.length > 0) {
            for (IdeaPluginDescriptor descriptor : descriptors) {
                if (descriptor.getPluginId().getIdString().equals("com.softwareco.intellij.plugin")) {
                    return descriptor;
                }
            }
        }
        return null;
    }

    public static String getWorkspaceName() {
        if (workspace_name == null) {
            workspace_name = generateToken();
        }
        return workspace_name;
    }

    public static String generateToken() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "");
    }

    public static Project getFirstActiveProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects != null && projects.length > 0) {
            return projects[0];
        }
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        if (editors != null && editors.length > 0) {
            return editors[0].getProject();
        }
        return null;
    }

    public static Project getProjectForPath(String path) {
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        if (editors != null && editors.length > 0) {
            for (Editor editor : editors) {
                if (editor != null && editor.getProject() != null) {
                    String basePath = editor.getProject().getBasePath();
                    if (path.indexOf(basePath) != -1) {
                        return editor.getProject();
                    }
                }
            }
        } else {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects != null && projects.length > 0) {
                return projects[0];
            }
        }
        return null;
    }

    public static FileDetails getFileDetails(String fullFileName) {
        FileDetails fileDetails = new FileDetails();
        if (StringUtils.isNotBlank(fullFileName)) {
            fileDetails.full_file_name = fullFileName;
            Project p = getProjectForPath(fullFileName);
            if (p != null) {
                fileDetails.project_directory = p.getBasePath();
                fileDetails.project_name = p.getName();
            }

            File f = new File(fullFileName);

            if (f.exists()) {
                fileDetails.character_count = f.length();
                fileDetails.file_name = f.getName();
                if (StringUtils.isNotBlank(fileDetails.project_directory) && fullFileName.indexOf(fileDetails.project_directory) != -1) {
                    // strip out the project_file_name
                    String[] parts = fullFileName.split(fileDetails.project_directory);
                    if (parts.length > 1) {
                        fileDetails.project_file_name = parts[1];
                    } else {
                        fileDetails.project_file_name = fullFileName;
                    }
                } else {
                    fileDetails.project_file_name = fullFileName;
                }
                if (fileDetails.line_count == 0) {
                    fileDetails.line_count = getLineCount(fullFileName);
                }

                VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(f);
                if (vFile != null) {
                    fileDetails.syntax = vFile.getFileType().getName();
                }
            }
        }

        return fileDetails;
    }

    public static int getLineCount(String fileName) {
        Stream<String> stream = null;
        try {
            Path path = Paths.get(fileName);
            stream = Files.lines(path);
            return (int) stream.count();
        } catch (Exception e) {
            return 0;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    public static boolean showingStatusText() {
        return showStatusText;
    }

    public static void toggleStatusBar(UIInteractionType interactionType) {
        String cta_text = !showStatusText ? "Show status bar metrics" : "Hide status bar metrics";
        showStatusText = !showStatusText;

        WallClockManager.getInstance().dispatchStatusViewUpdate();

        // refresh the tree
        CodeTimeToolWindow.refresh();

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_toggle_status_bar_metrics_btn" : "ct_toggle_status_bar_metrics_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = interactionType == UIInteractionType.click ? "blue" : null;
        elementEntity.cta_text = cta_text;
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "slash-eye" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }

    public static Project getOpenProject() {
        ProjectManager projMgr = ProjectManager.getInstance();
        Project[] projects = projMgr.getOpenProjects();
        if (projects != null && projects.length > 0) {
            return projects[0];
        }
        return null;
    }

    public static void updateStatusBar(final String kpmIcon, final String kpmMsg, final String tooltip) {

        // build the status bar text information
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ProjectManager pm = ProjectManager.getInstance();
                if (pm != null && pm.getOpenProjects() != null && pm.getOpenProjects().length > 0) {
                    try {
                        Project p = pm.getOpenProjects()[0];
                        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(p);

                        String kpmmsgId = SoftwareCoStatusBarKpmTextWidget.KPM_TEXT_ID + "_kpmmsg";
                        String kpmiconId = SoftwareCoStatusBarKpmIconWidget.KPM_ICON_ID + "_kpmicon";

                        String kpmMsgVal = kpmMsg != null ? kpmMsg : getPluginName();

                        String kpmIconVal = kpmIcon;
                        if (!showStatusText) {
                            kpmMsgVal = "";
                            kpmIconVal = "status-clock.svg";
                        }

                        // icon first
                        SoftwareCoStatusBarKpmIconWidget kpmIconWidget = (SoftwareCoStatusBarKpmIconWidget) statusBar.getWidget(kpmiconId);
                        if (kpmIconWidget == null) {
                            kpmIconWidget = buildStatusBarIconWidget(kpmIconVal, tooltip, kpmiconId);
                            statusBar.addWidget(kpmIconWidget, kpmiconId);
                        } else {
                            kpmIconWidget.updateIcon(kpmIconVal);
                            kpmIconWidget.setTooltip(tooltip);
                        }
                        statusBar.updateWidget(kpmiconId);

                        // text next
                        SoftwareCoStatusBarKpmTextWidget kpmMsgWidget = (SoftwareCoStatusBarKpmTextWidget) statusBar.getWidget(kpmmsgId);
                        if (kpmMsgWidget == null) {
                            kpmMsgWidget = buildStatusBarTextWidget(kpmMsgVal, tooltip, kpmmsgId);
                            statusBar.addWidget(kpmMsgWidget, kpmmsgId);
                        } else {
                            kpmMsgWidget.setText(kpmMsgVal);
                            kpmMsgWidget.setTooltip(tooltip);
                        }
                        statusBar.updateWidget(kpmmsgId);

                    } catch(Exception e){
                        //
                    }
                }
            }
        });
    }

    public static SoftwareCoStatusBarKpmTextWidget buildStatusBarTextWidget(String msg, String tooltip, String id) {
        SoftwareCoStatusBarKpmTextWidget textWidget =
                new SoftwareCoStatusBarKpmTextWidget(id);
        textWidget.setText(msg);
        textWidget.setTooltip(tooltip);
        return textWidget;
    }

    public static SoftwareCoStatusBarKpmIconWidget buildStatusBarIconWidget(String iconName, String tooltip, String id) {
        Icon icon = IconLoader.findIcon("/com/softwareco/intellij/plugin/assets/" + iconName);

        SoftwareCoStatusBarKpmIconWidget iconWidget =
                new SoftwareCoStatusBarKpmIconWidget(id);
        iconWidget.setIcon(icon);
        iconWidget.setTooltip(tooltip);
        return iconWidget;
    }

    public static List<String> getResultsForCommandArgs(String[] args, String dir) {
        List<String> results = new ArrayList<>();
        try {
            String result = UtilManager.runCommand(args, dir);
            if (result == null || result.trim().length() == 0) {
                return results;
            }
            String[] contentList = result.split("\n");
            results = Arrays.asList(contentList);
        } catch (Exception e) {
            if (results == null) {
                results = new ArrayList<>();
            }
        }
        return results;
    }

    private static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {

        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void launchSoftwareTopForty() {
        BrowserUtil.browse("http://api.software.com/music/top40");
    }

    public static void submitGitIssue() {
        BrowserUtil.browse("https://github.com/swdotcom/swdc-intellij/issues");
    }

    public static void submitFeedback(UIInteractionType interactionType) {
        BrowserUtil.browse("mailto:cody@software.com");

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_submit_feedback_btn" : "ct_submit_feedback_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = null;
        elementEntity.cta_text = "Submit feedback";
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "text-bubble" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }

    public static void buildCodeTimeMetricsDashboard() {
        String summaryInfoFile = FileUtilManager.getSummaryInfoFile();
        String dashboardFile = FileUtilManager.getCodeTimeDashboardFile();

        Writer writer = null;
        String api = "/dashboard?linux=" + UtilManager.isLinux() + "&showToday=true";
        String dashboardSummary = OpsHttpClient.softwareGet(api, FileUtilManager.getItem("jwt")).getJsonStr();
        if (dashboardSummary == null || dashboardSummary.trim().isEmpty()) {
            dashboardSummary = SERVICE_NOT_AVAIL;
        }

        // write the summary content
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(summaryInfoFile), StandardCharsets.UTF_8));
            writer.write(dashboardSummary);
        } catch (IOException ex) {
            // Report
        } finally {
            try {writer.close();} catch (Exception ex) {/*ignore*/}
        }

        // concat summary info with the dashboard file
        String dashboardContent = "";

        // append the summary content
        String infoFileContent = FileUtilManager.getFileContent(summaryInfoFile);
        if (infoFileContent != null) {
            dashboardContent += infoFileContent;
        }

        // write the dashboard content to the dashboard file
        FileUtilManager.saveFileContent(dashboardFile, dashboardContent);

    }

    public static void launchFile(String fsPath) {
        Project p = getOpenProject();
        if (p == null) {
            return;
        }
        File f = new File(fsPath);
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
        OpenFileDescriptor descriptor = new OpenFileDescriptor(p, vFile);
        FileEditorManager.getInstance(p).openTextEditor(descriptor, true);
    }

    public static void launchCodeTimeMetricsDashboard(UIInteractionType interactionType) {
        buildCodeTimeMetricsDashboard();

        String codeTimeFile = FileUtilManager.getCodeTimeDashboardFile();
        launchFile(codeTimeFile);

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_summary_btn" : "ct_summary_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = interactionType == UIInteractionType.click ? "purple" : null;
        elementEntity.cta_text = "View summary";
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "guage" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }

    public static void showOfflinePrompt(boolean isTenMinuteReconnect) {
        final String reconnectMsg = (isTenMinuteReconnect) ? "in ten minutes.\n" : "soon.\n";
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                String infoMsg = "Our service is temporarily unavailable.\n" +
                        "We will try to reconnect again " + reconnectMsg +
                        "Your status bar will not update at this time.";
                // ask to download the PM
                Messages.showInfoMessage(infoMsg, getPluginName());
            }
        });
    }

    public static String getDashboardRow(String label, String value) {
        String content = getDashboardLabel(label) + " : " + getDashboardValue(value) + "\n";
        return content;
    }

    public static String getSectionHeader(String label) {
        String content = label + "\n";
        // add 3 to account for the " : " between the columns
        int dashLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH + 15;
        for (int i = 0; i < dashLen; i++) {
            content += "-";
        }
        content += "\n";
        return content;
    }

    public static String getDashboardLabel(String label) {
        return getDashboardDataDisplay(DASHBOARD_LABEL_WIDTH, label);
    }

    public static String getDashboardValue(String value) {
        String valueContent = getDashboardDataDisplay(DASHBOARD_VALUE_WIDTH, value);
        String paddedContent = "";
        for (int i = 0; i < 11; i++) {
            paddedContent += " ";
        }
        paddedContent += valueContent;
        return paddedContent;
    }

    public static String getDashboardDataDisplay(int widthLen, String data) {
        int len = widthLen - data.length();
        String content = "";
        for (int i = 0; i < len; i++) {
            content += " ";
        }
        return content + "" + data;
    }

    public static boolean isGitProject(String projectDir) {
        if (projectDir == null || projectDir.equals("")) {
            return false;
        }

        String gitFile = projectDir + File.separator + ".git";
        File f = new File(gitFile);
        return f.exists();
    }

    public static JsonArray readAsJsonArray(String data) {
        try {
            JsonArray jsonArray = gson.fromJson(buildJsonReader(data), JsonArray.class);
            return jsonArray;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject readAsJsonObject(String data) {
        try {
            JsonObject jsonObject = gson.fromJson(buildJsonReader(data), JsonObject.class);
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonElement readAsJsonElement(String data) {
        try {
            JsonElement jsonElement = gson.fromJson(buildJsonReader(data), JsonElement.class);
            return jsonElement;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonReader buildJsonReader(String data) {
        // Clean the data
        data = cleanJsonString(data);
        JsonReader reader = new JsonReader(new StringReader(data));
        reader.setLenient(true);
        return reader;
    }

    /**
     * Replace byte order mark, new lines, and trim
     * @param data
     * @return clean data
     */
    public static String cleanJsonString(String data) {
        data = data.replace("\ufeff", "").replace("/\r\n/g", "").replace("/\n/g", "").trim();

        int braceIdx = data.indexOf("{");
        int bracketIdx = data.indexOf("[");

        // multi editor writes to the data.json file can cause an undefined string before the json object, remove it
        if (braceIdx > 0 && (braceIdx < bracketIdx || bracketIdx == -1)) {
            // there's something before the 1st brace
            data = data.substring(braceIdx);
        } else if (bracketIdx > 0 && (bracketIdx < braceIdx || braceIdx == -1)) {
            // there's something before the 1st bracket
            data = data.substring(bracketIdx);
        }

        return data;
    }

}
