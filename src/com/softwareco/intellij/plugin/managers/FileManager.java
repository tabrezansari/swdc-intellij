package com.softwareco.intellij.plugin.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.softwareco.intellij.plugin.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class FileManager {

    public static final Logger log = Logger.getLogger("FileManager");

    private static JsonObject sessionJson = null;
    private static Timer _timer = null;
    private static KeystrokeCount lastSavedKeystrokeStats = null;

    private static Semaphore semaphore = new Semaphore(1);

    public static String getSoftwareDir(boolean autoCreate) {
        String softwareDataDir = SoftwareCoUtils.getUserHomeDir();
        if (SoftwareCoUtils.isWindows()) {
            softwareDataDir += "\\.software";
        } else {
            softwareDataDir += "/.software";
        }

        File f = new File(softwareDataDir);
        if (autoCreate && !f.exists()) {
            // make the directory
            f.mkdirs();
        }

        return softwareDataDir;
    }

    public static String getSoftwareDataStoreFile() {
        String file = getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\data.json";
        } else {
            file += "/data.json";
        }
        return file;
    }

    public static String getSoftwareSessionFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\session.json";
        } else {
            file += "/session.json";
        }
        return file;
    }

    public static String getCurrentPayloadFile() {
        String file = getSoftwareDir(false);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\latestKeystrokes.json";
        } else {
            file += "/latestKeystrokes.json";
        }
        return file;
    }

    public static void writeData(String file, Object o) {
        if (o == null) {
            return;
        }
        File f = new File(file);
        final String content = SoftwareCo.gson.toJson(o);

        synchronized (semaphore) {
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(f), Charset.forName("UTF-8")));
                writer.write(content);
            } catch (IOException e) {
                log.warning("Code Time: Error writing content: " + e.getMessage());
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {/*ignore*/}
            }
        }
    }

    public static void appendData(String file, Object o) {
        if (o == null) {
            return;
        }
        File f = new File(file);
        String content = SoftwareCo.gson.toJson(o);
        if (SoftwareCoUtils.isWindows()) {
            content += "\r\n";
        } else {
            content += "\n";
        }
        final String contentToWrite = content;
        synchronized (semaphore) {
            try {
                log.info("Code Time: Storing content: " + contentToWrite);
                Writer output;
                output = new BufferedWriter(new FileWriter(f, true));  //clears file every time
                output.append(contentToWrite);
                output.close();
            } catch (Exception e) {
                log.warning("Code Time: Error appending content: " + e.getMessage());
            }
        }
    }

    public static JsonArray getFileContentAsJsonArray(String file) {
        synchronized (semaphore) {
            JsonParser parser = new JsonParser();
            try {
                Object obj = parser.parse(new FileReader(file));
                JsonArray jsonArray = parser.parse(cleanJsonString(obj.toString())).getAsJsonArray();
                return jsonArray;
            } catch (Exception e) {
                log.warning("Code Time: Error trying to read and parse " + file + ": " + e.getMessage());
            }
        }
        return new JsonArray();
    }

    public static JsonObject getFileContentAsJson(String file) {
        synchronized (semaphore) {
            JsonParser parser = new JsonParser();
            try {
                Object obj = parser.parse(new FileReader(file));
                JsonObject jsonArray = parser.parse(cleanJsonString(obj.toString())).getAsJsonObject();
                return jsonArray;
            } catch (Exception e) {
                log.warning("Code Time: Error trying to read and parse " + file + ": " + e.getMessage());
            }
        }
        return new JsonObject();
    }

    public static void deleteFile(String file) {
        File f = new File(file);
        // if the file exists, delete it
        if (f.exists()) {
            f.delete();
        }
    }


    public static void sendJsonArrayData(String file, String api) {
        File f = new File(file);
        if (f.exists()) {
            synchronized (semaphore) {
                try {
                    JsonArray jsonArr = FileManager.getFileContentAsJsonArray(file);
                    String payloadData = SoftwareCo.gson.toJson(jsonArr);
                    SoftwareResponse resp =
                            SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payloadData);
                    if (!resp.isOk()) {
                        // add these back to the offline file
                        log.info("Code Time: Unable to send array data: " + resp.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.info("Code Time: Unable to send array data: " + e.getMessage());
                }
            }
        }
    }

    public static void sendBatchData(String file, String api) {
        File f = new File(file);
        if (f.exists()) {
            synchronized (semaphore) {
                // found a data file, check if there's content
                StringBuffer sb = new StringBuffer();
                try {
                    FileInputStream fis = new FileInputStream(f);

                    // Construct BufferedReader from InputStreamReader
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                    String line = null;
                    // add commas to the end of each line
                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            sb.append(line).append(",");
                        }
                    }

                    br.close();

                    if (sb.length() > 0) {
                        // check to see if it's already an array
                        String payloads = sb.toString();
                        payloads = payloads.substring(0, payloads.lastIndexOf(","));
                        payloads = "[" + payloads + "]";

                        JsonArray jsonArray = (JsonArray) SoftwareCo.jsonParser.parse(payloads);

                        // delete the file
                        deleteFile(file);

                        JsonArray batch = new JsonArray();
                        // go through the array about 50 at a time
                        for (int i = 0; i < jsonArray.size(); i++) {
                            batch.add(jsonArray.get(i));
                            if (i > 0 && i % 50 == 0) {
                                String payloadData = SoftwareCo.gson.toJson(batch);
                                SoftwareResponse resp =
                                        SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payloadData);
                                if (!resp.isOk()) {
                                    // add these back to the offline file
                                    log.info("Code Time: Unable to send batch data: " + resp.getErrorMessage());
                                }
                                batch = new JsonArray();
                            }
                        }
                        if (batch.size() > 0) {
                            String payloadData = SoftwareCo.gson.toJson(batch);
                            SoftwareResponse resp =
                                    SoftwareCoUtils.makeApiCall("/data/batch", HttpPost.METHOD_NAME, payloadData);
                            if (!resp.isOk()) {
                                // add these back to the offline file
                                log.info("Code Time: Unable to send batch data: " + resp.getErrorMessage());
                            }
                        }

                    } else {
                        log.info("Code Time: No offline data to send");
                    }
                } catch (Exception e) {
                    log.warning("Code Time: Error trying to read and send offline data: " + e.getMessage());
                }
            }
        }
    }

    public static String getFileContent(String file) {
        String content = null;

        File f = new File(file);
        if (f.exists()) {
            synchronized (semaphore) {
                try {
                    byte[] encoded = Files.readAllBytes(Paths.get(file));
                    content = new String(encoded, Charset.forName("UTF-8"));
                } catch (Exception e) {
                    log.warning("Code Time: Error trying to read and parse: " + e.getMessage());
                }
            }
        }
        return content;
    }

    public static void saveFileContent(String file, String content) {
        File f = new File(file);
        synchronized (semaphore) {
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(f), Charset.forName("UTF-8")));
                writer.write(content);
            } catch (IOException ex) {
                // Report
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {/*ignore*/}
            }
        }
    }

    public static void storePayload(String payload) {
        if (payload == null || payload.length() == 0) {
            return;
        }
        if (SoftwareCoUtils.isWindows()) {
            payload += "\r\n";
        } else {
            payload += "\n";
        }
        String dataStoreFile = FileManager.getSoftwareDataStoreFile();
        synchronized (semaphore) {
            File f = new File(dataStoreFile);
            try {
                log.info("Code Time: Storing kpm metrics: " + payload);
                Writer output;
                output = new BufferedWriter(new FileWriter(f, true));  //clears file every time
                output.append(payload);
                output.close();
            } catch (Exception e) {
                log.warning("Code Time: Error appending to the Software data store file, error: " + e.getMessage());
            }
        }
    }

    public synchronized static void storeLatestPayloadLazily(final String data) {
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }

        _timer = new Timer();
        if (_timer != null) {
            _timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    FileManager.saveFileContent(FileManager.getCurrentPayloadFile(), data);
                }
            }, 2000);
        }
    }

    public static void openReadmeFile() {
        Project p = SoftwareCoUtils.getOpenProject();
        if (p == null) {
            return;
        }

        String fileContent = getReadmeContent();

        String readmeFile = SoftwareCoSessionManager.getReadmeFile();
        File f = new File(readmeFile);
        if (!f.exists()) {
            Writer writer = null;
            // write the summary content
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(new File(readmeFile)), StandardCharsets.UTF_8));
                writer.write(fileContent);
            } catch (IOException ex) {
                // Report
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {/*ignore*/}
            }
        }
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
        OpenFileDescriptor descriptor = new OpenFileDescriptor(p, vFile);
        FileEditorManager.getInstance(p).openTextEditor(descriptor, true);

        // possible way of opening an asset file but it doesn't work in prod, only in debug
//        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
//        // TODO: figure out how to show it with only the preview window
//        FileEditorManager fileEditorManager = FileEditorManager.getInstance(p);
//        OpenFileDescriptor descriptor = new OpenFileDescriptor(p, vFile);
//        fileEditorManager.openEditor(descriptor, true);
    }

    public static void sendOfflineData() {
        try {
            String payloads = getKeystrokePayloads();
            if (payloads == null || StringUtils.isBlank(payloads)) {
                return;
            }

            JsonArray jsonArray = (JsonArray) SoftwareCo.jsonParser.parse(payloads);

            JsonArray batch = new JsonArray();
            // go through the array about 50 at a time
            for (int i = 0; i < jsonArray.size(); i++) {
                batch.add(jsonArray.get(i));
                if (i > 0 && i % 50 == 0) {
                    String payloadData = SoftwareCo.gson.toJson(batch);
                    SoftwareResponse resp =
                            SoftwareCoUtils.makeApiCall("/data/batch", HttpPost.METHOD_NAME, payloadData);
                    if (!resp.isOk()) {
                        // add these back to the offline file
                        log.info("Code Time: Unable to send batch data: " + resp.getErrorMessage());
                        return;
                    }
                    batch = new JsonArray();
                }
            }
            if (batch.size() > 0) {
                String payloadData = SoftwareCo.gson.toJson(batch);
                SoftwareResponse resp =
                        SoftwareCoUtils.makeApiCall("/data/batch", HttpPost.METHOD_NAME, payloadData);
                if (!resp.isOk()) {
                    // add these back to the offline file
                    log.info("Code Time: Unable to send batch data: " + resp.getErrorMessage());
                    return;
                }
            }

            // delete the file now that we've made it this far without http errors
            deleteFile(getSoftwareDataStoreFile());
        } catch (Exception e) {
            log.warning("Code Time: Error trying to read and send offline data, error: " + e.getMessage());
        }

    }

    private static String getReadmeContent() {
        return "# Code Time\n" +
                "\n" +
                "[Code Time](https://www.software.com/code-time) is an open source plugin for automatic programming metrics and time-tracking.\n" +
                "\n" +
                "![Marketplace](https://img.shields.io/jetbrains/plugin/v/10687-code-time.svg)\n" +
                "![Installs](https://img.shields.io/jetbrains/plugin/d/10687-code-time)\n" +
                "\n" +
                "## Getting started\n" +
                "\n" +
                "**1. Create your web account**\n" +
                "\n" +
                "![Code Time for IntelliJ login](https://swdc-intellij.s3-us-west-1.amazonaws.com/login-prompt.png)\n" +
                "\n" +
                "The Code Time web app has data visualizations and settings you can customize, such as your work hours and rates per project for advanced time tracking. You can also connect Google Calendar to visualize your Code Time vs. meetings in a single calendar.\n" +
                "\n" +
                "You can connect multiple code editors on multiple devices using the same email account.\n" +
                "\n" +
                "**2. Track your progress during the day**\n" +
                "\n" +
                "![Code Time for IntelliJ status bar](https://swdc-intellij.s3-us-west-1.amazonaws.com/status-bar.png)\n" +
                "\n" +
                "Your status bar shows you in real-time how many hours and minutes you code each day.\n" +
                "\n" +
                "**3. Check out your coding activity**\n" +
                "\n" +
                "To see an overview of your coding activity and project metrics, open the **Code Time panel** by clicking on the Code Time icon in your side bar.\n" +
                "\n" +
                "![Code Time for IntelliJ side bar](https://swdc-intellij.s3-us-west-1.amazonaws.com/side-bar.png)\n" +
                "\n" +
                "In your **Activity Metrics**, your _editor time_ is the total time you have spent in your editor today. Your _code time_ is the total time you have been typing in your editor today. Each metric shows how you compare today to your average and the global average. Each average is calculated by day of week over the last 90 days (e.g. a Friday average is an average of all previous Fridays).\n" +
                "\n" +
                "You can also see your top files today by KPM (keystrokes per minute), keystrokes, and code time.\n" +
                "\n" +
                "Your **Project Metrics** provides a breakdown for the current open project. The _Open changes_ section shows how many lines you have added and deleted before you commit them and the _Committed today_ section summarizes your commits today.\n" +
                "\n" +
                "**4. Generate your Code Time dashboard**\n" +
                "\n" +
                "At the end of your first day, open Code Time in your side bar and click _Generate dashboard_ to open your dashboard in a new editor tab. Your dashboard summarizes your coding data—such as your code time by project, lines of code, and keystrokes per minute—today, yesterday, last week, and over the last 90 days.\n" +
                "\n" +
                "![Code Time for IntelliJ dashboard](https://swdc-intellij.s3-us-west-1.amazonaws.com/editor-dashboard.png)\n" +
                "\n" +
                "## Web app data visualizations\n" +
                "\n" +
                "Click \"See advanced metrics\" in the Code Time side bar or visit [app.software.com](https://app.software.com/) to see more data visualizations. Here are a few examples of what you will see in your feed after your first week.\n" +
                "\n" +
                "**Code Time heatmap**\n" +
                "\n" +
                "Code Time measures your coding activity per hour and summarizes your data in a weekly and 90-day average heatmap. Protect your best times on your heatmap from meetings and interrupts to help boost your productivity.\n" +
                "\n" +
                "![Code Time for IntelliJ heatmap](https://swdc-vscode.s3-us-west-1.amazonaws.com/weekly-heatmap.png)\n" +
                "\n" +
                "**Project-based reports**\n" +
                "\n" +
                "See how much time you spend per project per week. Code Time also lets you set a rate per project and export your data to a CSV.\n" +
                "\n" +
                "![Code Time for IntelliJ top projects](https://swdc-vscode.s3-us-west-1.amazonaws.com/top-projects.png)\n" +
                "\n" +
                "**Work-life balance**\n" +
                "\n" +
                "How much do you code after hours and weekends? Code Time helps you see your breakdown at work vs. outside work so you can find ways to improve your work-life balance.\n" +
                "\n" +
                "![Code Time for IntelliJ work life balance](https://swdc-vscode.s3-us-west-1.amazonaws.com/work-life-balance.png)\n" +
                "\n" +
                "**Commit velocity**\n" +
                "\n" +
                "Code Time integrates with Git, so you can see your speed, frequency, and top files across your commits.\n" +
                "\n" +
                "![Code Time for IntelliJ commit velocity](https://swdc-vscode.s3-us-west-1.amazonaws.com/commit-velocity.png)\n" +
                "\n" +
                "## It’s safe, secure, and free\n" +
                "\n" +
                "**We never access your code:** We do not process, send, or store your proprietary code. We only provide metrics about programming, and we make it easy to see the data we collect.\n" +
                "\n" +
                "**Your data is private:** We will never share your individually identifiable data with your boss. In the future, we will roll up data into groups and teams but we will keep your data anonymized.\n" +
                "\n" +
                "**Free for you, forever:** We provide 90 days of data history for free, forever. In the future, we will provide premium plans for advanced features and historical data access.\n" +
                "\n" +
                "Code Time also collects basic usage metrics to help us make informed decisions about our roadmap.\n" +
                "\n" +
                "## Get in touch\n" +
                "\n" +
                "Enjoying Code Time? Let us know how it’s going by tweeting or following us at [@software_hq](https://twitter.com/software_hq).\n" +
                "\n" +
                "We recently released a new beta plugin, Music Time for VS Code, which helps you find your most productive songs for coding. You can learn more [here](https://www.software.com).\n" +
                "\n" +
                "Have any questions? Please email us at [support@software.com](mailto:support@software.com) and we’ll get back to you as soon as we can.\n";
    }

    public static String cleanJsonString(String data) {
        data = data.replace("/\r\n/g", "").replace("/\n/g", "").trim();
        return data;
    }

    public static String getItem(String key) {
        sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsString();
        }
        return null;
    }

    public static String getItem(String key, String defaultVal) {
        sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsString();
        }
        if (defaultVal != null) {
            return defaultVal;
        }
        return null;
    }

    public static void setNumericItem(String key, long val) {
        sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);

        String content = sessionJson.toString();

        String sessionFile = getSoftwareSessionFile(true);
        saveFileContent(sessionFile, content);
    }

    public static void setItem(String key, String val) {
        sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);

        String content = sessionJson.toString();
        String sessionFile = getSoftwareSessionFile(true);

        saveFileContent(sessionFile, content);

    }

    public static long getNumericItem(String key, Long defaultVal) {
        sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsLong();
        }
        return defaultVal.longValue();
    }

    public static synchronized JsonObject getSoftwareSessionAsJson() {
        if (sessionJson == null) {
            String sessionFile = getSoftwareSessionFile(true);
            File f = new File(sessionFile);
            if (f.exists()) {
                try {
                    Path p = Paths.get(sessionFile);

                    byte[] encoded = Files.readAllBytes(p);
                    String content = new String(encoded, Charset.defaultCharset());
                    if (content != null) {
                        // json parse it
                        sessionJson = SoftwareCo.jsonParser.parse(content).getAsJsonObject();
                    }

                } catch (Exception e) {
                    log.warning("Code Time: Error trying to read and json parse the session file.");
                }
            }
            if (sessionJson == null) {
                sessionJson = new JsonObject();
            }
        }
        return sessionJson;
    }

    public static KeystrokeCount getLastSavedKeystrokeStats() {
        List<KeystrokeCount> list = convertPayloadsToList(getKeystrokePayloads());
        if (list != null && list.size() > 0) {
            list.sort((o1, o2) -> o2.start < o1.start ? -1 : o2.start > o1.start ? 1 : 0);
            lastSavedKeystrokeStats = list.get(0);
        }
        return lastSavedKeystrokeStats;
    }

    private static List<KeystrokeCount> convertPayloadsToList(String payloads) {
        if (StringUtils.isNotBlank(payloads)) {
            JsonArray jsonArray = (JsonArray) SoftwareCo.jsonParser.parse(payloads);
            if (jsonArray != null && jsonArray.size() > 0) {
                Type type = new TypeToken<List<KeystrokeCount>>() {
                }.getType();
                List<KeystrokeCount> list = SoftwareCo.gson.fromJson(jsonArray, type);

                return list;
            }
        }
        return new ArrayList<>();
    }

    private static String getKeystrokePayloads() {
        final String dataStoreFile = getSoftwareDataStoreFile();
        File f = new File(dataStoreFile);

        if (f.exists()) {
            synchronized (semaphore) {
                // found a data file, check if there's content
                StringBuffer sb = new StringBuffer();
                try {
                    FileInputStream fis = new FileInputStream(f);

                    //Construct BufferedReader from InputStreamReader
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                    String line = null;
                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            sb.append(line).append(",");
                        }
                    }

                    br.close();

                    if (sb.length() > 0) {
                        // we have data to send
                        String payloads = sb.toString();
                        payloads = payloads.substring(0, payloads.lastIndexOf(","));
                        payloads = "[" + payloads + "]";

                        return payloads;

                    } else {
                        log.info("Code Time: No offline data to send");
                    }
                } catch (Exception e) {
                    log.warning("Code Time: Error trying to read and send offline data, error: " + e.getMessage());
                }
            }
        }
        return null;
    }

}
