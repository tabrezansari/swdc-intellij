package com.softwareco.intellij.plugin.sessiondata;


import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.SoftwareResponse;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.KeystrokeAggregate;
import com.softwareco.intellij.plugin.models.SessionSummary;
import com.softwareco.intellij.plugin.models.TimeData;
import com.softwareco.intellij.plugin.timedata.TimeDataManager;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;
import org.apache.http.client.methods.HttpGet;

import java.lang.reflect.Type;

public class SessionDataManager {

    private static String getSessionDataSummaryFile() {
        String file = SoftwareCoSessionManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\sessionSummary.json";
        } else {
            file += "/sessionSummary.json";
        }
        return file;
    }

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileManager.writeData(getSessionDataSummaryFile(), summary);
    }

    public static SessionSummary getSessionSummaryData() {
        JsonObject jsonObj = FileManager.getFileContentAsJson(getSessionDataSummaryFile());
        if (jsonObj == null) {
            clearSessionSummaryData();
            jsonObj = FileManager.getFileContentAsJson(getSessionDataSummaryFile());
        }
        Type type = new TypeToken<SessionSummary>() {}.getType();
        SessionSummary summary = SoftwareCo.gson.fromJson(jsonObj, type);
        return summary;
    }

    public static SessionSummary fetchSessionSummary(boolean forceSummaryFetch) {
        SessionSummary summary = getSessionSummaryData();

        if (SoftwareCoSessionManager.isServerOnline() && forceSummaryFetch) {
            String sessionsApi = "/sessions/summary";

            // make an async call to get the kpm info
            SoftwareResponse resp = SoftwareCoUtils.makeApiCall(
                    sessionsApi, HttpGet.METHOD_NAME, null);
            if (resp.isOk()) {
                JsonObject jsonObj = resp.getJsonObj();
                Type type = new TypeToken<SessionSummary>() {}.getType();
                summary = SoftwareCo.gson.fromJson(jsonObj, type);

                // save the file
                FileManager.writeData(getSessionDataSummaryFile(), summary);

                // check if we need to update the latestPayloadTimestampEndUtc
                long currentTs = SoftwareCoSessionManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
                if (summary.getLatestPayloadTimestamp() > currentTs) {
                    SoftwareCoSessionManager.setNumericItem("latestPayloadTimestampEndUtc", summary.getLatestPayloadTimestamp());
                }
            }
        }

        // update the wallclock time if it's
        // lagging behind the newly gathered current day seconds
        long session_seconds = summary.getCurrentDayMinutes() * 60;
        WallClockManager.getInstance().updateBasedOnSessionSeconds(session_seconds);

        return summary;
    }

    public static void incrementSessionSummary(KeystrokeAggregate aggregate) {
        WallClockManager wcMgr = WallClockManager.getInstance();
        SessionSummary summary = getSessionSummaryData();

        long incrementMinutes = getMinutesSinceLastPayload();
        summary.setCurrentDayMinutes(summary.getCurrentDayMinutes() + incrementMinutes);

        long sessionSeconds = summary.getCurrentDayMinutes() * 60;
        wcMgr.updateBasedOnSessionSeconds(sessionSeconds);
        long editorSeconds = wcMgr.getWcTimeInSeconds();

        summary.setCurrentDayKeystrokes(summary.getCurrentDayKeystrokes() + aggregate.keystrokes);
        summary.setCurrentDayLinesAdded(summary.getCurrentDayLinesAdded() + aggregate.linesAdded);
        summary.setCurrentDayLinesRemoved(summary.getCurrentDayLinesRemoved() + aggregate.linesRemoved);

        // get the current time data and update
        TimeData td = TimeDataManager.getTodayTimeDataSummary();
        long fileSeconds = td.getFile_seconds() + 60;

        TimeDataManager.updateTimeDataSummary(editorSeconds, sessionSeconds, fileSeconds);
    }

    private static long getMinutesSinceLastPayload() {
        long minutesSinceLastPayload = 1;
        long lastPayloadEnd = SoftwareCoSessionManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        if (lastPayloadEnd > 0) {
            SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
            long diffInSec = timesData.now - lastPayloadEnd;
            long sessionThresholdSeconds = 60 * 15;
            if (diffInSec > 0 && diffInSec <= sessionThresholdSeconds) {
                minutesSinceLastPayload = diffInSec / 60;
            }
        }

        return minutesSinceLastPayload;
    }
}
