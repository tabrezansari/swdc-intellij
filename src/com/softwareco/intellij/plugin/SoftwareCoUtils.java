/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.softwareco.intellij.plugin.managers.*;
import com.softwareco.intellij.plugin.models.CodeTimeSummary;
import com.softwareco.intellij.plugin.models.FileDetails;
import com.softwareco.intellij.plugin.models.SessionSummary;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SoftwareCoUtils {

    public static final Logger LOG = Logger.getLogger("SoftwareCoUtils");

    // set the api endpoint to use
    public final static String api_endpoint = "https://api.software.com";
    // set the launch url to use
    public final static String launch_url = "https://app.software.com";

    public static HttpClient httpClient;
    public static HttpClient pingClient;

    private static final Gson gson = new Gson();

    // sublime = 1, vs code = 2, eclipse = 3, intellij = 4, visual studio = 6, atom = 7
    public static int pluginId = 4;
    public static String VERSION = null;
    public static String pluginName = null;

    public final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private final static int EOF = -1;
    private static boolean appAvailable = true;
    private static boolean showStatusText = true;

    private static final int lastDayOfMonth = 0;

    private static final int DASHBOARD_LABEL_WIDTH = 25;
    private static final int DASHBOARD_VALUE_WIDTH = 25;

    private static final String SERVICE_NOT_AVAIL =
            "Our service is temporarily unavailable.\n\nPlease try again later.\n";

    private static final long DAYS_IN_SECONDS = 60 * 60 * 24;

    private static String workspace_name = null;

    static {
        // initialize the HttpClient
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        pingClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        httpClient = HttpClientBuilder.create().build();
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

    public static boolean isLoggedIn() {
        String name = FileManager.getItem("name");
        return name != null && !name.equals("");
    }

    public static String getHostname() {
        List<String> cmd = new ArrayList<String>();
        cmd.add("hostname");
        String hostname = getSingleLineResult(cmd, 1);
        return hostname;
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
                if (StringUtils.isNotBlank(fileDetails.project_directory)) {
                    // strip out the project_file_name
                    fileDetails.project_file_name = fullFileName.split(fileDetails.project_directory)[1];
                } else {
                    fileDetails.project_file_name = fullFileName;
                }
                fileDetails.line_count = getLineCount(fullFileName);

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

    public static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public static String getOs() {
        String osName = SystemUtils.OS_NAME;
        String osVersion = SystemUtils.OS_VERSION;
        String osArch = SystemUtils.OS_ARCH;

        String osInfo = "";
        if (osArch != null) {
            osInfo += osArch;
        }
        if (osInfo.length() > 0) {
            osInfo += "_";
        }
        if (osVersion != null) {
            osInfo += osVersion;
        }
        if (osInfo.length() > 0) {
            osInfo += "_";
        }
        if (osName != null) {
            osInfo += osName;
        }

        return osInfo;
    }

    public static boolean isLinux() {
        return !isWindows() && !isMac();
    }

    public static boolean isWindows() {
        return SystemInfo.isWindows;
    }

    public static boolean isMac() {
        return SystemInfo.isMac;
    }

    public static void updateServerStatus(boolean isOnlineStatus) {
        appAvailable = isOnlineStatus;
    }

    public static boolean isAppAvailable() {
        return appAvailable;
    }

    public static SoftwareResponse makeApiCall(String api, String httpMethodName, String payload) {
        return makeApiCall(api, httpMethodName, payload, null);
    }

    public static SoftwareResponse makeApiCall(String api, String httpMethodName, String payload, String overridingJwt) {

        SoftwareResponse softwareResponse = new SoftwareResponse();

        SoftwareHttpManager httpTask = null;
        if (api.contains("/ping") || api.contains("/sessions") || api.contains("/dashboard") || api.contains("/users/plugin/accounts")) {
            // if the server is having issues, we'll timeout within 5 seconds for these calls
            httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, httpClient);
        } else {
            if (httpMethodName.equals(HttpPost.METHOD_NAME)) {
                // continue, POSTS encapsulated "invokeLater" with a timeout of 5 seconds
                httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, pingClient);
            } else {
                if (!appAvailable) {
                    // bail out
                    softwareResponse.setIsOk(false);
                    return softwareResponse;
                }
                httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, httpClient);
            }
        }
        Future<HttpResponse> response = EXECUTOR_SERVICE.submit(httpTask);

        //
        // Handle the Future if it exist
        //
        if (response != null) {
            try {
                HttpResponse httpResponse = response.get();
                if (httpResponse != null) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode < 300) {
                        softwareResponse.setIsOk(true);
                    }
                    softwareResponse.setCode(statusCode);
                    HttpEntity entity = httpResponse.getEntity();
                    JsonObject jsonObj = null;
                    if (entity != null) {
                        try {
                            ContentType contentType = ContentType.getOrDefault(entity);
                            String mimeType = contentType.getMimeType();
                            String jsonStr = getStringRepresentation(entity);
                            softwareResponse.setJsonStr(jsonStr);
                            // LOG.log(Level.INFO, "Code Time: API response {0}", jsonStr);
                            if (jsonStr != null && mimeType.indexOf("text/plain") == -1) {
                                JsonElement jsonEl = readAsJsonElement(jsonStr);
                                if (jsonEl != null) {
                                    try {
                                        JsonElement el = jsonEl;
                                        if (el.isJsonPrimitive()) {
                                            if (statusCode < 300) {
                                                softwareResponse.setDataMessage(el.getAsString());
                                            } else {
                                                softwareResponse.setErrorMessage(el.getAsString());
                                            }
                                        } else {
                                            jsonObj = jsonEl.getAsJsonObject();
                                            softwareResponse.setJsonObj(jsonObj);
                                        }
                                    } catch (Exception e) {
                                        LOG.log(Level.WARNING, "Unable to parse response data: {0}", e.getMessage());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            String errorMessage = pluginName + ": Unable to get the response from the http request, error: " + e.getMessage();
                            softwareResponse.setErrorMessage(errorMessage);
                            LOG.log(Level.WARNING, errorMessage);
                        }
                    }

                    if (statusCode >= 400 && statusCode < 500 && jsonObj != null) {
                        if (jsonObj.has("code")) {
                            String code = jsonObj.get("code").getAsString();
                            if (code != null && code.equals("DEACTIVATED")) {
                                softwareResponse.setDeactivated(true);
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                String errorMessage = pluginName + ": Unable to get the response from the http request, error: " + e.getMessage();
                softwareResponse.setErrorMessage(errorMessage);
                LOG.log(Level.WARNING, errorMessage);
            }
        }

        return softwareResponse;
    }

    private static String getStringRepresentation(HttpEntity res) throws IOException {
        if (res == null) {
            return null;
        }

        ContentType contentType = ContentType.getOrDefault(res);
        String mimeType = contentType.getMimeType();
        boolean isPlainText = mimeType.indexOf("text/plain") != -1;

        InputStream inputStream = res.getContent();

        // Timing information--- verified that the data is still streaming
        // when we are called (this interval is about 2s for a large response.)
        // So in theory we should be able to do somewhat better by interleaving
        // parsing and reading, but experiments didn't show any improvement.
        //

        StringBuffer sb = new StringBuffer();
        InputStreamReader reader;
        reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        BufferedReader br = new BufferedReader(reader);
        boolean done = false;
        while (!done) {
            String aLine = br.readLine();
            if (aLine != null) {
                sb.append(aLine);
                if (isPlainText) {
                    sb.append("\n");
                }
            } else {
                done = true;
            }
        }
        br.close();

        return sb.toString();
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

                        String kpmMsgVal = kpmMsg != null ? kpmMsg : pluginName;

                        String kpmIconVal = kpmIcon;
                        if (!showStatusText) {
                            kpmMsgVal = "";
                            kpmIconVal = "clock-blue.png";
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

    public static String humanizeMinutes(long minutes) {
        String str = "";
        if (minutes == 60) {
            str = "1 hr";
        } else if (minutes > 60) {
            float hours = (float)minutes / 60;
            try {
                str = String.format("%.1f", hours) + " hrs";
            } catch (Exception e) {
                str = String.format("%s hrs", Math.round(hours));
            }
        } else if (minutes == 1) {
            str = "1 min";
        } else {
            str = minutes + " min";
        }
        return str;
    }

    public static String humanizeDoubleNumbers(double number) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        return nf.format(number);
    }

    public static String humanizeLongNumbers(long number) {
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(number);
    }

    /**
     * Execute the args
     * @param args
     * @return
     */
    public static String runCommand(String[] args, String dir) {
        // use process builder as it allows to run the command from a specified dir
        ProcessBuilder builder = new ProcessBuilder();

        try {
            builder.command(args);
            if (dir != null) {
                // change to the directory to run the command
                builder.directory(new File(dir));
            }
            Process process = builder.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream is = process.getInputStream();
            copyLarge(is, baos, new byte[4096]);
            return baos.toString().trim();

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static List<String> getResultsForCommandArgs(String[] args, String dir) {
        List<String> results = new ArrayList<>();
        try {
            String result = runCommand(args, dir);
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
        String summaryInfoFile = SoftwareCoSessionManager.getSummaryInfoFile(true);
        String dashboardFile = SoftwareCoSessionManager.getCodeTimeDashboardFile();

        Writer writer = null;
        String api = "/dashboard?linux=" + SoftwareCoUtils.isLinux() + "&showToday=false";
        String dashboardSummary = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null).getJsonStr();
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
        SimpleDateFormat formatDayTime = new SimpleDateFormat("EEE, MMM d h:mma");
        SimpleDateFormat formatDay = new SimpleDateFormat("EEE, MMM d");
        String lastUpdatedStr = formatDayTime.format(new Date());
        dashboardContent += "Code Time          (Last updated on " + lastUpdatedStr + ")";
        dashboardContent += "\n\n";
        String todayStr = formatDay.format(new Date());
        dashboardContent += getSectionHeader("Today (" + todayStr + ")");

        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();

        String activeCodeTimeMin = SoftwareCoUtils.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes);
        String codeTimeMin = SoftwareCoUtils.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        dashboardContent += getDashboardRow("Code time today", codeTimeMin);
        dashboardContent += getDashboardRow("Active code time today", activeCodeTimeMin);

        SessionSummary summary = SessionDataManager.getSessionSummaryData();
        if (summary != null) {
            long averageDailyMinutes = summary.getAverageDailyMinutes();
            String averageDailyMinutesTimeStr = SoftwareCoUtils.humanizeMinutes(averageDailyMinutes);
            dashboardContent += getDashboardRow("90-day avg", averageDailyMinutesTimeStr);
            dashboardContent += "\n";
        } else {
            dashboardContent += getDashboardRow("90-day avg", SoftwareCoUtils.humanizeMinutes(0));
        }

        // append the summary content
        String infoFileContent = FileManager.getFileContent(summaryInfoFile);
        if (infoFileContent != null) {
            dashboardContent += infoFileContent;
        }

        // write the dashboard content to the dashboard file
        FileManager.saveFileContent(dashboardFile, dashboardContent);

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

        String codeTimeFile = SoftwareCoSessionManager.getCodeTimeDashboardFile();
        launchFile(codeTimeFile);

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_summary_btn" : "ct_summary_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = interactionType == UIInteractionType.click ? "purple" : null;
        elementEntity.cta_text = "View summary";
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "guage" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }

    private static String getSingleLineResult(List<String> cmd, int maxLen) {
        String result = null;
        String[] cmdArgs = Arrays.copyOf(cmd.toArray(), cmd.size(), String[].class);
        String content = SoftwareCoUtils.runCommand(cmdArgs, null);

        // for now just get the 1st one found
        if (content != null) {
            String[] contentList = content.split("\n");
            if (contentList != null && contentList.length > 0) {
                int len = (maxLen != -1) ? Math.min(maxLen, contentList.length) : contentList.length;
                for (int i = 0; i < len; i++) {
                    String line = contentList[i];
                    if (line != null && line.trim().length() > 0) {
                        result = line.trim();
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static String getOsUsername() {
        String username = System.getProperty("user.name");
        if (username == null || username.trim().equals("")) {
            try {
                List<String> cmd = new ArrayList<String>();
                if (SoftwareCoUtils.isWindows()) {
                    cmd.add("cmd");
                    cmd.add("/c");
                    cmd.add("whoami");
                } else {
                    cmd.add("/bin/sh");
                    cmd.add("-c");
                    cmd.add("whoami");
                }
                username = getSingleLineResult(cmd, -1);
            } catch (Exception e) {
                //
            }
        }
        return username;
    }

    public static String getAppJwt(boolean serverIsOnline) {
        if (serverIsOnline) {
            long now = Math.round(System.currentTimeMillis() / 1000);
            String api = "/data/apptoken?token=" + now;
            SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null);
            if (resp.isOk()) {
                JsonObject obj = resp.getJsonObj();
                return obj.get("jwt").getAsString();
            }
        }
        return null;
    }

    public static String createAnonymousUser(boolean serverIsOnline) {
        // make sure we've fetched the app jwt
        String appJwt = getAppJwt(serverIsOnline);

        if (serverIsOnline && appJwt != null) {
            String timezone = TimeZone.getDefault().getID();

            JsonObject payload = new JsonObject();
            payload.addProperty("username", getOsUsername());
            payload.addProperty("timezone", timezone);
            payload.addProperty("hostname", getHostname());
            payload.addProperty("creation_annotation", "NO_SESSION_FILE");

            String api = "/data/onboard";
            SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payload.toString(), appJwt);
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                if (data != null && data.has("jwt")) {
                    String dataJwt = data.get("jwt").getAsString();
                    FileManager.setItem("jwt", dataJwt);
                    return dataJwt;
                }
            }
        }
        return null;
    }

    private static JsonObject getUser() {
        String jwt = FileManager.getItem("jwt");
        String api = "/users/me";
        SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
        if (resp.isOk()) {
            // check if we have the data and jwt
            // resp.data.jwt and resp.data.user
            // then update the session.json for the jwt
            JsonObject obj = resp.getJsonObj();
            if (obj != null && obj.has("data")) {
                return obj.get("data").getAsJsonObject();
            }
        }
        return null;
    }

    private static final String regex = "^\\S+@\\S+\\.\\S+$";
    private static final Pattern pattern = Pattern.compile(regex);

    private static boolean validateEmail(String email) {
        return pattern.matcher(email).matches();
    }

    public static boolean getUserLoginState() {
        String pluginjwt = FileManager.getItem("jwt");

        JsonObject userObj = getUser();
        if (userObj != null && userObj.has("email")) {
            // check if the email is valid
            String email = userObj.get("email").getAsString();
            if (validateEmail(email)) {
                FileManager.setItem("jwt", userObj.get("plugin_jwt").getAsString());
                FileManager.setItem("name", email);
                return true;
            }
        }

        String api = "/users/plugin/state";
        SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, pluginjwt);
        if (resp.isOk()) {
            // check if we have the data and jwt
            // resp.data.jwt and resp.data.user
            // then update the session.json for the jwt
            JsonObject data = resp.getJsonObj();
            String state = (data != null && data.has("state")) ? data.get("state").getAsString() : "UNKNOWN";
            // check if we have any data
            if (state.equals("OK")) {
                String dataJwt = data.get("jwt").getAsString();
                FileManager.setItem("jwt", dataJwt);
                String dataEmail = data.get("email").getAsString();
                if (dataEmail != null) {
                    FileManager.setItem("name", dataEmail);
                }
                return true;
            }
        }

        return false;
    }

    public static void showOfflinePrompt(boolean isTenMinuteReconnect) {
        final String reconnectMsg = (isTenMinuteReconnect) ? "in ten minutes.\n" : "soon.\n";
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                String infoMsg = "Our service is temporarily unavailable.\n" +
                        "We will try to reconnect again " + reconnectMsg +
                        "Your status bar will not update at this time.";
                // ask to download the PM
                Messages.showInfoMessage(infoMsg, pluginName);
            }
        });
    }

    public static Date atStartOfWeek(long local_now) {
        // find out how many days to go back
        int daysBack = 0;
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            while (dayOfWeek != Calendar.SUNDAY) {
                daysBack++;
                dayOfWeek -= 1;
            }
        } else {
            daysBack = 7;
        }

        long startOfDayInSec = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
        long startOfWeekInSec = startOfDayInSec - (DAYS_IN_SECONDS * daysBack);

        return new Date(startOfWeekInSec * 1000);
    }

    public static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    public static Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // the timestamps are all in seconds
    public static class TimesData {
        public Integer offset;
        public long now;
        public long local_now;
        public String timezone;
        public long local_start_day;
        public long local_start_yesterday;
        public Date local_start_of_week_date;
        public Date local_start_of_yesterday_date;
        public Date local_start_today_date;
        public long local_start_of_week;
        public long local_end_day;
        public long utc_end_day;

        public TimesData() {
            offset = ZonedDateTime.now().getOffset().getTotalSeconds();
            now = System.currentTimeMillis() / 1000;
            local_now = now + offset;
            timezone = TimeZone.getDefault().getID();
            local_start_day = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
            local_start_yesterday = local_start_day - DAYS_IN_SECONDS;
            local_start_of_week_date = atStartOfWeek(local_now);
            local_start_of_yesterday_date = new Date(local_start_yesterday * 1000);
            local_start_today_date = new Date(local_start_day * 1000);
            local_start_of_week = local_start_of_week_date.toInstant().getEpochSecond();
            local_end_day = atEndOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
            utc_end_day = atEndOfDay(new Date(now * 1000)).toInstant().getEpochSecond();
        }
    }

    public static TimesData getTimesData() {
        TimesData timesData = new TimesData();
        return timesData;
    }

    public static String getTodayInStandardFormat() {
        SimpleDateFormat formatDay = new SimpleDateFormat("YYYY-MM-dd");
        String day = formatDay.format(new Date());
        return day;
    }

    public static boolean isNewDay() {
        String currentDay = FileManager.getItem("currentDay", "");
        String day = SoftwareCoUtils.getTodayInStandardFormat();
        return !day.equals(currentDay);
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
