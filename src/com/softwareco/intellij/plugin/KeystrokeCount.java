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
import org.apache.commons.lang.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class KeystrokeCount {

    // TODO: backend driven, we should look at getting a list of types at some point
    private String type = "Events";

    // non-hardcoded attributes
    private Map<String, FileInfo> source = new HashMap<>();
    private String version;
    private int pluginId;
    public int keystrokes = 0;
    // start and end are in seconds
    public long start;
    private long local_start;
    private String os;
    private String timezone;
    private KeystrokeProject project;

    public long cumulative_editor_seconds = 0;
    public long cumulative_session_seconds = 0;
    public long elapsed_seconds = 0;
    public int new_day = 0; // 1 or zero to denote new day or not
    public String project_null_error = "";
    public String editor_seconds_error = "";
    public String session_seconds_error = "";

    private boolean triggered = false;


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

        kc.cumulative_editor_seconds = this.cumulative_editor_seconds;
        kc.cumulative_session_seconds = this.cumulative_session_seconds;
        kc.elapsed_seconds = this.elapsed_seconds;
        kc.new_day = this.new_day; // 1 or zero to denote new day or not
        kc.project_null_error = this.project_null_error;
        kc.editor_seconds_error = this.editor_seconds_error;
        kc.session_seconds_error = this.session_seconds_error;

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
        this.triggered = false;
        this.cumulative_editor_seconds = 0;
        this.cumulative_session_seconds = 0;
        this.elapsed_seconds = 0;
        this.new_day = 0; // 1 or zero to denote new day or not
        this.project_null_error = "";
        this.editor_seconds_error = "";
        this.session_seconds_error = "";
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
        // Initiate Process Keystrokes Timer
        if (!this.triggered) {
            this.triggered = true;

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    processKeystrokes();
                }
            }, 1000 * 60);
        }

        // Fetch the FileInfo
        if (source != null && source.get(fileName) != null) {
            return source.get(fileName);
        }

        if (source == null) {
            source = new HashMap<>();
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

        // Keystrokes metadata needs to be initialized
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
        fileInfoData.fsPath = fileName;

        return fileInfoData;
    }

    public String getSource() {
        return SoftwareCo.gson.toJson(source);
    }

    public void endPreviousModifiedFiles(String fileName) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        if (this.source != null) {
            for (String key : this.source.keySet()) {
                FileInfo fileInfo = this.source.get(key);
                if (key.equals(fileName)) {
                    fileInfo.end = 0;
                    fileInfo.local_end = 0;
                } else {
                    fileInfo.end = timesData.now;
                    fileInfo.local_end = timesData.local_now;
                }
            }
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
        try {
            if (this.hasData()) {

                // make sure a project is available
                if (this.project == null || StringUtils.isBlank(this.project.getDirectory())) {
                    this.project = new KeystrokeProject("Unnamed", "Untitled");
                }

                ElapsedTime eTime = SessionDataManager.getTimeBetweenLastPayload();

                // end the file end times.
                this.preProcessKeystrokeData(eTime.sessionSeconds, eTime.elapsedSeconds);

                // update the file aggregate info.
                this.updateAggregates(eTime.sessionSeconds);

                final String payload = SoftwareCo.gson.toJson(this);

                // store to send later
                FileManager.storePayload(payload);

                // refresh the code time tree view
                WallClockManager.getInstance().dispatchStatusViewUpdate();

                // set the latest payload
                SoftwareCoUtils.setLatestPayload(this);
            }

            SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
            // set the latest payload timestamp utc so help with session time calculations
            FileManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
        } catch (Exception e) {
            //
        }

        this.resetData();
    }

    public void updateLatestPayloadLazily() {
        String payload = SoftwareCo.gson.toJson(this);
        FileManager.storeLatestPayloadLazily(payload);
    }

    private void validateAndUpdateCumulativeData(long sessionSeconds) {

        TimeData td = TimeDataManager.incrementSessionAndFileSeconds(this.project, sessionSeconds);

        // add the cumulative data
        long lastPayloadEnd = FileManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        this.new_day = lastPayloadEnd == 0 ? 1 : 0;

        // get the current payloads so we can compare our last cumulative seconds
        KeystrokeCount lastKpm = FileManager.getLastSavedKeystrokeStats();
        if (lastKpm != null) {
            if (lastKpm.cumulative_editor_seconds == 0 ||
                lastKpm.cumulative_session_seconds == 0) {
                lastKpm = null;
            }
            if (lastKpm != null &&
                    SoftwareCoUtils.getFormattedDay(lastKpm.start).equals(SoftwareCoUtils.getFormattedDay(this.start))) {
                lastKpm = null;
            }
        }

        this.cumulative_session_seconds = 60;
        this.cumulative_editor_seconds = 60;

        if (td != null) {
            this.cumulative_editor_seconds = td.getEditor_seconds();
            this.cumulative_session_seconds = td.getSession_seconds();
            if (lastKpm != null) {
                // editor seconds check
                if (lastKpm.cumulative_editor_seconds > cumulative_editor_seconds) {
                    long diff = lastKpm.cumulative_editor_seconds - cumulative_editor_seconds;
                    cumulative_editor_seconds = lastKpm.cumulative_editor_seconds + 60;
                    this.editor_seconds_error = "TimeData has lower editor seconds than last saved keystroke data by " + diff + " seconds";
                }
                // session seconds check
                if (lastKpm.cumulative_session_seconds > cumulative_session_seconds) {
                    long diff = lastKpm.cumulative_session_seconds - cumulative_session_seconds;
                    cumulative_session_seconds = lastKpm.cumulative_session_seconds + 60;
                    this.session_seconds_error = "TimeData has lower session seconds than last saved keystroke data by " + diff + " seconds";
                }
            }
        } else if (lastKpm != null) {
            // no time data found, project null error
            this.project_null_error = "TimeData not found using " + this.project.getDirectory() + " for editor and session seconds";
            cumulative_editor_seconds = lastKpm.cumulative_editor_seconds + 60;
            cumulative_session_seconds = lastKpm.cumulative_session_seconds + 60;
        }

        if (cumulative_editor_seconds < cumulative_session_seconds) {
            long diff = cumulative_session_seconds - cumulative_editor_seconds;
            if (diff > 45) {
                this.editor_seconds_error = "Cumulative editor seconds is behind session seconds by " + diff + " seconds";
            }
            cumulative_editor_seconds = cumulative_session_seconds;
        }

        // update the cumulative editor second
        this.cumulative_editor_seconds = cumulative_editor_seconds;
        this.cumulative_session_seconds = cumulative_session_seconds;
    }

    // end unended file payloads and add the cumulative editor seconds
    public void preProcessKeystrokeData(long sessionSeconds, long elapsedSeconds) {

        this.validateAndUpdateCumulativeData(sessionSeconds);

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
