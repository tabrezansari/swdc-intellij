/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved...
 */
package com.softwareco.intellij.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

import java.util.Map;
import java.util.Set;

public class KeystrokeCount {

    // TODO: backend driven, we should look at getting a list of types at some point
    private String type = "Events";

    // non-hardcoded attributes
    private JsonObject source = new JsonObject();
    private String version;
    private int pluginId;
    private String keystrokes = "0"; // keystroke count
    // start and end are in seconds
    private long start;
    private long local_start;
    private String os;
    private String timezone;
    private KeystrokeProject project;

    public KeystrokeCount() {
        String appVersion = PluginManager.getPlugin(PluginId.getId("com.softwareco.intellij.plugin")).getVersion();
        if (appVersion != null) {
            this.version = appVersion;
        } else {
            this.version = SoftwareCoUtils.getVersion();
        }
        this.pluginId = SoftwareCoUtils.pluginId;
        this.os = SoftwareCoUtils.getOs();
    }

    public KeystrokeCount clone() {
        KeystrokeCount kc = new KeystrokeCount();
        kc.keystrokes = this.keystrokes;
        kc.start = this.start;
        kc.local_start = this.local_start;
        kc.version = this.version;
        kc.pluginId = this.pluginId;
        kc.project = this.project;
        kc.type = this.type;
        kc.source = this.source;
        kc.timezone = this.timezone;

        return kc;
    }

    public void resetData() {
        this.keystrokes = "0";
        this.source = new JsonObject();
        if (this.project != null) {
            this.project.resetData();
        }

        this.start = 0L;
        this.local_start = 0L;
        this.timezone = "";
    }

    public JsonObject getSourceByFileName(String fileName) {
        if (source.has(fileName)) {
            return source.get(fileName).getAsJsonObject();
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

        if (this.start == 0) {
            this.start = timesData.now;
            this.local_start = timesData.local_now;
            this.timezone = timesData.timezone;

            // start the keystroke processor 1 minute timer
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 60);
                    this.processKeystrokes();
                } catch (Exception e) {
                    System.err.println(e);
                }
            }).start();
        }

        // create one and return the one just created
        JsonObject fileInfoData = new JsonObject();
        fileInfoData.addProperty("add", 0);
        fileInfoData.addProperty("paste", 0);
        fileInfoData.addProperty("open", 0);
        fileInfoData.addProperty("close", 0);
        fileInfoData.addProperty("delete", 0);
        fileInfoData.addProperty("length", 0);
        fileInfoData.addProperty("netkeys", 0);
        fileInfoData.addProperty("lines", 0);
        fileInfoData.addProperty("linesAdded", 0);
        fileInfoData.addProperty("linesRemoved", 0);
        fileInfoData.addProperty("syntax", "");
        fileInfoData.addProperty("start", timesData.now);
        fileInfoData.addProperty("end", 0);
        fileInfoData.addProperty("local_start", timesData.local_now);
        fileInfoData.addProperty("local_end", 0);
        source.add(fileName, fileInfoData);

        return fileInfoData;
    }

    public String getSource() {
        return SoftwareCo.gson.toJson(source);
    }

    public void endPreviousModifiedFiles(String currentFileName) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Set<Map.Entry<String, JsonElement>> fileInfoDataSet = this.source.entrySet();
        for ( Map.Entry<String, JsonElement> fileInfoData : fileInfoDataSet ) {
            JsonObject fileinfoDataJsonObj = (JsonObject) fileInfoData.getValue();

            // if the file info data element doesn't equal the current file name, set the end timestamp
            if (!fileInfoData.getKey().equals(currentFileName)) {
                long endVal = fileinfoDataJsonObj.get("end").getAsLong();
                if (endVal == 0) {
                    // set the end time for this file
                    fileinfoDataJsonObj.addProperty("end", timesData.now);
                    fileinfoDataJsonObj.addProperty("local_end", timesData.local_now);
                }
            } else {
                // it does match it, zero out the end timestamp
                fileinfoDataJsonObj.addProperty("end", 0);
                fileinfoDataJsonObj.addProperty("local_end", 0);

            }
        }
    }

    public void endUnendedFiles() {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Set<Map.Entry<String, JsonElement>> fileInfoDataSet = this.source.entrySet();
        for ( Map.Entry<String, JsonElement> fileInfoData : fileInfoDataSet ) {
            JsonObject fileinfoDataJsonObj = (JsonObject) fileInfoData.getValue();
            long endVal = fileinfoDataJsonObj.get("end").getAsLong();
            // end the ones that don't have an end time
            if (endVal == 0) {
                // set the end time for this file
                fileinfoDataJsonObj.addProperty("end", timesData.now);
                fileinfoDataJsonObj.addProperty("local_end", timesData.local_now);
            }
        }
    }

    public boolean hasData() {
        if (Integer.parseInt(this.getKeystrokes()) > 0) {
            return true;
        }

        return false;
    }

    public void processKeystrokes() {

        if (this.hasData()) {

            SoftwareCoSessionManager sessionMgr = SoftwareCoSessionManager.getInstance();

            // end the file end times
            this.endUnendedFiles();

            SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

            final String payload = SoftwareCo.gson.toJson(this);

            SoftwareCoOfflineManager.getInstance().incrementSessionSummaryData(1, Integer.parseInt(keystrokes));

            // store to send later
            sessionMgr.storePayload(payload);

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    sessionMgr.fetchDailyKpmSessionInfo();
                } catch (Exception e) {
                    System.err.println(e);
                }
            }).start();

        }

        this.resetData();

    }

    public String getKeystrokes() {
        return keystrokes;
    }

    public void setKeystrokes(String keystrokes) {
        this.keystrokes = keystrokes;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setLocal_start(long local_start) {
        this.local_start = local_start;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public KeystrokeProject getProject() {
        return project;
    }

    public void setProject(KeystrokeProject project) {
        this.project = project;
    }

    @Override
    public String toString() {
        return "KeystrokeCount{" +
                "type='" + type + '\'' +
                ", pluginId=" + pluginId +
                ", source=" + source +
                ", keystrokes='" + keystrokes + '\'' +
                ", start=" + start +
                ", local_start=" + local_start +
                ", timezone='" + timezone + '\'' +
                ", project=" + project +
                '}';
    }
}
