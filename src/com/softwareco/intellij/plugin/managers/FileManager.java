package com.softwareco.intellij.plugin.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.softwareco.intellij.plugin.*;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class FileManager {

    public static final Logger log = Logger.getLogger("FileManager");

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

    private static String getDeviceFile() {
        String file = getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\device.json";
        } else {
            file += "/device.json";
        }
        return file;
    }

    public static synchronized void writeData(String file, Object o) {
        if (o == null) {
            return;
        }
        File f = new File(file);
        final String content = SoftwareCo.gson.toJson(o);

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(content);
        } catch (IOException e) {
            log.warning("Code Time: Error writing content: " + e.getMessage());
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    public static JsonArray getFileContentAsJsonArray(String file) {
        return getJsonArrayFromFile(file);
    }

    public static JsonObject getFileContentAsJson(String file) {
        return getJsonObjectFromFile(file);
    }

    public static void deleteFile(String file) {
        File f = new File(file);
        // if the file exists, delete it
        if (f.exists()) {
            f.delete();
        }
    }

    public static void saveFileContent(String file, String content) {
        File f = new File(file);
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(content);
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    private synchronized static void writeSessionJsonContent(JsonObject obj) {
        File f = new File(getSoftwareSessionFile(true));
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(obj.toString());
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    public static void openReadmeFile(UIInteractionType interactionType) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Project p = SoftwareCoUtils.getOpenProject();
            if (p == null) {
                return;
            }

            UIElementEntity elementEntity = new UIElementEntity();
            elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_learn_more_btn" : "ct_learn_more_cmd";
            elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
            elementEntity.color = interactionType == UIInteractionType.click ? "yellow" : null;
            elementEntity.cta_text = "Learn more";
            elementEntity.icon_name = interactionType == UIInteractionType.click ? "document" : null;
            EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);

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

            SoftwareCoUtils.launchFile(f.getPath());

        });
    }

    private static String getReadmeContent() {
        return "CODE TIME\n" +
                "---------\n" +
                "\n" +
                "Code Time is an open source plugin for automatic programming metrics and time-tracking. \n" +
                "\n" +
                "\n" +
                "GETTING STARTED\n" +
                "---------------\n" +
                "\n" +
                "1. Create your web account\n" +
                "\n" +
                "    The Code Time web app has data visualizations and settings you can customize, such as your work hours and office type for advanced time tracking. You can also connect Google Calendar to visualize your Code Time vs. meetings in a single calendar.\n" +
                "\n" +
                "    You can connect multiple code editors on multiple devices using the same email account.\n" +
                "\n" +
                "2. Track your progress during the day\n" +
                "\n" +
                "    Your status bar shows you in real-time how many hours and minutes you code each day. A rocket will appear if your code time exceeds your daily average on this day of the week.\n" +
                "\n" +
                "3. Check out your coding activity\n" +
                "\n" +
                "    To see an overview of your coding activity and project metrics, open the Code Time panel by clicking on the Code Time icon in your side bar.\n" +
                "\n" +
                "    In your Activity Metrics, your _code time_ is the total time you have spent in your editor today. Your _active code_ time is the total time you have been typing in your editor today. Each metric shows how you compare today to your average and the global average. Each average is calculated by day of week over the last 90 days (e.g. a Friday average is an average of all previous Fridays). You can also see your top files today by KPM (keystrokes per minute), keystrokes, and code time.\n" +
                "\n" +
                "    If you have a Git repository open, Contributors provides a breakdown of contributors to the current open project and their latest commits.\n" +
                "\n" +
                "4. Generate your Code Time dashboard\n" +
                "\n" +
                "    At the end of your first day, open Code Time in your side bar and click _View summary_ to open your dashboard in a new editor tab. Your dashboard summarizes your coding data—such as your code time by project, lines of code, and keystrokes per minute—today, yesterday, last week, and over the last 90 days.\n" +
                "\n" +
                "\n" +
                "WEB APP DATA VISUALIZATIONS\n" +
                "---------------------------\n" +
                "\n" +
                "Click \"See advanced metrics\" in the Code Time side bar or visit app.software.com to see more data visualizations. Here are a few examples of what you will see in your dashboard after your first week.\n" +
                "\n" +
                "* Active code time\n" +
                "\n" +
                "    Visualize your daily active code time. See productivity trends compared to weekly and monthly averages. See how you stack up against the Software community of over 100,000 developers.\n" +
                "\n" +
                "* Top projects\n" +
                "\n" +
                "    See how much time you spend per project per week. Get a breakdown of your top projects right in your dashboard.\n" +
                "\n" +
                "* Work-life balance\n" +
                "\n" +
                "    Connect your Google Calendar to visualize meeting time versus code time. See how much coding happens during work hours versus nights and weekends so you can find ways to improve your work-life balance.\n" +
                "\n" +
                "SAFE, SECURE, AND FREE\n" +
                "----------------------\n" +
                "\n" +
                "We never access your code: We do not process, send, or store your proprietary code. We only provide metrics about programming, and we make it easy to see the data we collect.\n" +
                "\n" +
                "Your data is private: We will never share your individually identifiable data with your boss. In the future, we will roll up data into groups and teams but we will keep your data anonymized.\n" +
                "\n" +
                "Free for you, forever: We provide 90 days of data history for free, forever. In the future, we will provide premium plans for advanced features and historical data access.\n" +
                "\n" +
                "Code Time also collects basic usage metrics to help us make informed decisions about our roadmap.\n" +
                "\n" +
                "\n" +
                "GET IN TOUCH\n" +
                "------------\n" +
                "\n" +
                "Enjoying Code Time? Let us know how it’s going by tweeting or following us at @software_hq.\n" +
                "\n" +
                "We recently released a new beta plugin, Music Time for Visual Studio Code, which helps you find your most productive songs for coding. You can learn more at software.com.\n" +
                "\n" +
                "Have any questions? Please email us at support@software.com and we’ll get back to you as soon as we can.\n";
    }

    public static String getItem(String key) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsString();
        }
        return null;
    }

    public static String getItem(String key, String defaultVal) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsString();
        }
        return defaultVal;
    }

    public static void setNumericItem(String key, long val) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);
        writeSessionJsonContent(sessionJson);
    }

    public static void setItem(String key, String val) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);
        writeSessionJsonContent(sessionJson);
    }

    public static void setBooleanItem(String key, boolean val) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);
        writeSessionJsonContent(sessionJson);
    }

    public static boolean getBooleanItem(String key) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsBoolean();
        }
        return false;
    }

    public static long getNumericItem(String key, Long defaultVal) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsLong();
        }
        return defaultVal.longValue();
    }

    public static JsonObject getSoftwareSessionAsJson() {
        return getJsonObjectFromFile(getSoftwareSessionFile(true));
    }

    public static JsonObject getJsonObjectFromFile(String fileName) {
        JsonObject jsonObject = new JsonObject();
        String content = getFileContent(fileName);

        if (content != null) {
            // json parse it
            jsonObject = SoftwareCoUtils.readAsJsonObject(content);
        }

        if (jsonObject == null) {
            jsonObject = new JsonObject();
        }
        return jsonObject;
    }

    public static JsonArray getJsonArrayFromFile(String fileName) {
        JsonArray jsonArray = new JsonArray();
        String content = getFileContent(fileName);

        if (content != null) {
            // json parse it
            jsonArray = SoftwareCoUtils.readAsJsonArray(content);
        }

        if (jsonArray == null) {
            jsonArray = new JsonArray();
        }
        return jsonArray;
    }

    public static String getFileContent(String file) {
        String content = null;

        File f = new File(file);
        if (f.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(file));
                content = new String(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warning("Code Time: Error trying to read and parse: " + e.getMessage());
            }
        }
        return content;
    }

    public static String getPluginUuid() {
        String plugin_uuid = null;
        JsonObject deviceJson = getJsonObjectFromFile(getDeviceFile());
        if (deviceJson.has("plugin_uuid") && !deviceJson.get("plugin_uuid").isJsonNull()) {
            plugin_uuid = deviceJson.get("plugin_uuid").getAsString();
        } else {
            // set it for the 1st and only time
            plugin_uuid = UUID.randomUUID().toString();
            deviceJson.addProperty("plugin_uuid", plugin_uuid);
            String content = deviceJson.toString();
            saveFileContent(getDeviceFile(), content);
        }
        return plugin_uuid;
    }

    public static String getAuthCallbackState() {
        JsonObject deviceJson = getJsonObjectFromFile(getDeviceFile());
        if (deviceJson != null && deviceJson.has("auth_callback_state") && !deviceJson.get("auth_callback_state").isJsonNull()) {
            return deviceJson.get("auth_callback_state").getAsString();
        }
        return null;
    }

    public static void setAuthCallbackState(String value) {
        String deviceFile = getDeviceFile();
        JsonObject deviceJson = getJsonObjectFromFile(deviceFile);
        deviceJson.addProperty("auth_callback_state", value);

        String content = deviceJson.toString();

        saveFileContent(deviceFile, content);
    }

}
