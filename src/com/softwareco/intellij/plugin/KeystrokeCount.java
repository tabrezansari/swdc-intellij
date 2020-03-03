/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved...
 */
package com.softwareco.intellij.plugin;

import com.softwareco.intellij.plugin.aggdata.FileAggregateDataManager;
import com.softwareco.intellij.plugin.models.FileChangeInfo;
import com.softwareco.intellij.plugin.models.KeystrokeAggregate;
import com.softwareco.intellij.plugin.sessiondata.SessionDataManager;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class KeystrokeCount {

    private static final Logger LOG = Logger.getLogger("KeystrokeCount");
    // TODO: backend driven, we should look at getting a list of types at some point
    private String type = "Events";

    // non-hardcoded attributes
    private Map<String, FileInfo> source = new HashMap<>();
    private String version;
    private int pluginId;
    private int keystrokes = 0;
    // start and end are in seconds
    private long start;
    private long local_start;
    private String os;
    private String timezone;
    private KeystrokeProject project;

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

        if (this.start == 0) {
            this.start = timesData.now;
            this.local_start = timesData.local_now;
            this.timezone = timesData.timezone;

            // start the keystroke processor 1 minute timer
            final Runnable service = () -> this.processKeystrokes();
            AsyncManager.getInstance().executeOnceInSeconds(service, 60);
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

    public void endUnendedFiles() {
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

        // set the latest payload timestamp utc so help with session time calculations
        SoftwareCoSessionManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
    }

    // update each source with it's true amount of keystrokes
    public boolean hasData() {
        boolean foundKpmData = false;
        if (this.getKeystrokes() > 0 || this.hasOpenAndCloseMetrics()) {
            foundKpmData = true;
        }

        int keystrokesTally = 0;

        // tally the metrics to set the keystrokes for each source key
        Map<String, FileInfo> fileInfoDataSet = this.source;
        for ( FileInfo data : fileInfoDataSet.values() ) {
            data.keystrokes = data.add + data.paste + data.delete + data.linesAdded + data.linesRemoved;
            keystrokesTally += data.keystrokes;
        }

        if (keystrokesTally > this.getKeystrokes()) {
            this.setKeystrokes(keystrokesTally);
        }

        return foundKpmData;
    }


    public void processKeystrokes() {
        if (this.hasData()) {

            SoftwareCoSessionManager sessionMgr = SoftwareCoSessionManager.getInstance();

            // end the file end times.
            this.endUnendedFiles();

            // update the file aggregate info.
            this.updateAggregates();

            final String payload = SoftwareCo.gson.toJson(this);

            // store to send later
            sessionMgr.storePayload(payload);
        }

        this.resetData();
    }

    private void updateAggregates() {
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        KeystrokeAggregate aggregate = new KeystrokeAggregate();
        if (this.project != null) {
            aggregate.directory = this.project.getDirectory();
        } else {
            aggregate.directory = "Unnamed";
        }
        for (String key : this.source.keySet()) {
            FileInfo fileInfo = this.source.get(key);
            fileInfo.duration_seconds = fileInfo.end - fileInfo.start;
            fileInfo.fsPath = key;

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
        }

        // update the aggregate info
        SessionDataManager.incrementSessionSummary(aggregate);

        // update the file info map
        FileAggregateDataManager.updateFileChangeInfo(fileChangeInfoMap);

        // refresh the code time tree view
        WallClockManager.getInstance().dispatchStatusViewUpdate();
    }

    public int getKeystrokes() {
        return keystrokes;
    }

    public void setKeystrokes(int keystrokes) {
        this.keystrokes = keystrokes;
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
