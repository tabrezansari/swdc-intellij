/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved...
 */
package com.softwareco.intellij.plugin;

import com.softwareco.intellij.plugin.managers.FileManager;
import com.softwareco.intellij.plugin.managers.FileAggregateDataManager;
import com.softwareco.intellij.plugin.models.ElapsedTime;
import com.softwareco.intellij.plugin.models.FileChangeInfo;
import com.softwareco.intellij.plugin.models.KeystrokeAggregate;
import com.softwareco.intellij.plugin.models.TimeData;
import com.softwareco.intellij.plugin.managers.SessionDataManager;
import com.softwareco.intellij.plugin.managers.TimeDataManager;
import com.softwareco.intellij.plugin.managers.WallClockManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class KeystrokeCount {

    // TODO: backend driven, we should look at getting a list of types at some point
    private String type = "Events";

    // non-hardcoded attributes
    private Map<String, FileInfo> source = new HashMap<>();
    private String version;
    private int pluginId;
    public int keystrokes = 0;
    // start and end are in seconds
    private long start;
    private long local_start;
    private String os;
    private String timezone;
    private KeystrokeProject project;
    private long cumulative_editor_seconds = 0;
    private long elapsed_seconds = 0;

    public KeystrokeCount() {
        String appVersion = SoftwareCo.getVersion();
        if (appVersion != null) {
            this.version = appVersion;
        } else {
            this.version = SoftwareCoUtils.VERSION;
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
        this.keystrokes = 0;
        this.source = new HashMap<>();
        if (this.project != null) {
            this.project.resetData();
        }

        this.start = 0L;
        this.local_start = 0L;
        this.timezone = "";

        SoftwareCoUtils.setLatestPayload(null);
    }

    private boolean hasOpenAndCloseMetrics() {
        Map<String, FileInfo> fileInfoDataSet = this.source;
        for ( FileInfo fileInfoData : fileInfoDataSet.values() ) {
            if (fileInfoData.open > 0 && fileInfoData.close > 0) {
                return true;
            }
        }
        return false;
    }

    public static class FileInfo {
        public Integer add = 0;
        public Integer paste = 0;
        public Integer open = 0;
        public Integer close = 0;
        public Integer delete = 0;
        public Integer length = 0;
        public Integer netkeys = 0;
        public Integer lines = 0;
        public Integer linesAdded = 0;
        public Integer linesRemoved = 0;
        public Integer keystrokes = 0;
        public String syntax = "";
        public long start = 0;
        public long end = 0;
        public long local_start = 0;
        public long local_end = 0;
        public long duration_seconds = 0;
        public String fsPath = "";
        public String name = "";
    }

    public FileInfo getSourceByFileName(String fileName) {
        if (source.get(fileName) != null) {
            return source.get(fileName);
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

        // start the process keystrokes timer if this is the start of a new payload
        if (this.start == 0) {
            this.start = timesData.now;
            this.local_start = timesData.local_now;
            this.timezone = timesData.timezone;
        }

        // create one and return the one just created
        FileInfo fileInfoData = new FileInfo();
        fileInfoData.start = timesData.now;
        fileInfoData.local_start = timesData.local_now;
        source.put(fileName, fileInfoData);

        return fileInfoData;
    }

    public String getSource() {
        return SoftwareCo.gson.toJson(source);
    }

    public void endPreviousModifiedFiles(String currentFileName) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Map<String, FileInfo> fileInfoDataSet = this.source;

        for (FileInfo fileInfoData : fileInfoDataSet.values()) {
            if (fileInfoData.end == 0) {
                fileInfoData.end = timesData.now;
                fileInfoData.local_end = timesData.local_now;
            }
        }
        if(fileInfoDataSet.get(currentFileName) != null) {
            FileInfo fileInfoData = fileInfoDataSet.get(currentFileName);
            fileInfoData.end = 0;
            fileInfoData.local_end = 0;
        }
    }

    // update each source with it's true amount of keystrokes
    public boolean hasData() {
        boolean foundKpmData = false;
        if (this.keystrokes > 0 || this.hasOpenAndCloseMetrics()) {
            foundKpmData = true;
        }

        int keystrokesTally = 0;

        // tally the metrics to set the keystrokes for each source key
        Map<String, FileInfo> fileInfoDataSet = this.source;
        for ( FileInfo data : fileInfoDataSet.values() ) {
            data.keystrokes = data.add + data.paste + data.delete + data.linesAdded + data.linesRemoved;
            keystrokesTally += data.keystrokes;
        }

        if (keystrokesTally > this.keystrokes) {
            this.keystrokes = keystrokesTally;
        }

        return foundKpmData;
    }


    public void processKeystrokes() {
        if (this.hasData()) {

            ElapsedTime eTime = SessionDataManager.getTimeBetweenLastPayload();

            // end the file end times.
            this.endUnendedFiles(eTime.sessionSeconds, eTime.elapsedSeconds);

            // update the file aggregate info.
            this.updateAggregates(eTime.sessionSeconds);

            final String payload = SoftwareCo.gson.toJson(this);

            // store to send later
            SoftwareCoSessionManager.getInstance().storePayload(payload);

            // refresh the code time tree view
            WallClockManager.getInstance().dispatchStatusViewUpdate();

            // set the latest payload
            SoftwareCoUtils.setLatestPayload(this);
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        // set the latest payload timestamp utc so help with session time calculations
        FileManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);

        this.resetData();
    }

    public void updateLatestPayloadLazily() {
        String payload = SoftwareCo.gson.toJson(this);
        FileManager.storeLatestPayloadLazily(payload);
    }

    // end unended file payloads and add the cumulative editor seconds
    public void endUnendedFiles(long sessionSeconds, long elapsedSeconds) {
        // update the project time data session seconds
        TimeDataManager.incrementSessionAndFileSeconds(this.project, sessionSeconds);

        TimeData td = TimeDataManager.getTodayTimeDataSummary(this.project);

        long editorSeconds = 60;
        if (td != null) {
            editorSeconds = Math.max(td.getEditor_seconds(), td.getSession_seconds());
        }

        this.cumulative_editor_seconds = editorSeconds;
        this.elapsed_seconds = elapsedSeconds;

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Map<String, FileInfo> fileInfoDataSet = this.source;
        for ( FileInfo fileInfoData : fileInfoDataSet.values() ) {
            // end the ones that don't have an end time
            if (fileInfoData.end == 0) {
                // set the end time for this file
                fileInfoData.end = timesData.now;
                fileInfoData.local_end = timesData.local_now;
            }
        }
    }

    private void updateAggregates(long sessionSeconds) {
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        KeystrokeAggregate aggregate = new KeystrokeAggregate();
        if (this.project != null) {
            aggregate.directory = this.project.getDirectory();
        } else {
            aggregate.directory = "Untitled";
        }
        for (String key : this.source.keySet()) {
            FileInfo fileInfo = this.source.get(key);
            fileInfo.duration_seconds = fileInfo.end - fileInfo.start;
            fileInfo.fsPath = key;
            try {
                Path path = Paths.get(key);
                if (path != null) {
                    Path fileName = path.getFileName();
                    if (fileName != null) {
                        fileInfo.name = fileName.toString();
                    }
                }

                aggregate.aggregate(fileInfo);

                FileChangeInfo existingFileInfo = fileChangeInfoMap.get(key);
                if (existingFileInfo == null) {
                    existingFileInfo = new FileChangeInfo();
                    fileChangeInfoMap.put(key, existingFileInfo);
                }
                existingFileInfo.aggregate(fileInfo);
                existingFileInfo.kpm = existingFileInfo.keystrokes / existingFileInfo.update_count;
            } catch (Exception e) {
                // error getting the path
            }
        }

        // update the aggregate info
        SessionDataManager.incrementSessionSummary(aggregate, sessionSeconds);

        // update the file info map
        FileAggregateDataManager.updateFileChangeInfo(fileChangeInfoMap);
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
