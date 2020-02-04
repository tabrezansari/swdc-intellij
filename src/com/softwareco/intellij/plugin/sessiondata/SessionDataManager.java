package com.softwareco.intellij.plugin.sessiondata;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.SoftwareResponse;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.SessionSummary;
import com.softwareco.intellij.plugin.models.TimeData;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;
import org.apache.http.client.methods.HttpGet;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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

    private static SessionSummary getSessionSummaryData() {
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
        WallClockManager.updateBasedOnSessionSeconds(session_seconds);

        return summary;
    }
}
