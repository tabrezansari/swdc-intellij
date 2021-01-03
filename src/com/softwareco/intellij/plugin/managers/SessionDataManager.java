package com.softwareco.intellij.plugin.managers;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.models.ElapsedTime;
import com.softwareco.intellij.plugin.models.KeystrokeAggregate;
import com.softwareco.intellij.plugin.models.SessionSummary;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

import java.lang.reflect.Type;

public class SessionDataManager {


    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static void refreshSessionDataAndTree() {
        SessionDataManager.clearSessionSummaryData();
        TimeDataManager.clearTimeDataSummary();

        // prompt they've completed the setup
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                // this will fetch the session summary data and refresh the tree
                SessionDataManager.treeDataUpdateCheck(true);

                // ask to download the PM
                Messages.showInfoMessage("Successfully logged onto Code Time", "Code Time Setup Complete");

            }
        });
    }

    public static void treeDataUpdateCheck(boolean isNewUser) {
        String day = UtilManager.getTodayInStandardFormat();
        String currentDay = FileUtilManager.getItem("updatedTreeDate", "");
        SessionSummary existingSummary = SessionDataManager.getSessionSummaryData();
        if (!currentDay.equals(day) || existingSummary.getGlobalAverageDailyMinutes() == 0 || isNewUser) {
            updateSessionSummaryFromServer();
            FileUtilManager.setItem("updatedTreeDate", day);
        }
    }

    private static void updateSessionSummaryFromServer() {
        SessionSummary summary = SessionDataManager.getSessionSummaryData();

        String jwt = FileUtilManager.getItem("jwt");
        String api = "/sessions/summary?refresh=true";
        ClientResponse resp = OpsHttpClient.softwareGet(api, jwt);
        if (resp.isOk()) {
            JsonObject jsonObj = resp.getJsonObj();

            Type type = new TypeToken<SessionSummary>() {}.getType();
            SessionSummary fetchedSummary = SoftwareCo.gson.fromJson(jsonObj, type);

            // clone all
            summary.clone(fetchedSummary);

            TimeDataManager.updateSessionFromSummaryApi(fetchedSummary.getCurrentDayMinutes());

            // save the file
            FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
        }

        WallClockManager.getInstance().dispatchStatusViewUpdate();
    }

    public static SessionSummary getSessionSummaryData() {
        JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
        if (jsonObj == null) {
            clearSessionSummaryData();
            jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
        }
        JsonElement lastUpdatedToday = jsonObj.get("lastUpdatedToday");
        if (lastUpdatedToday != null) {
            // make sure it's a boolean and not a number
            if (!lastUpdatedToday.getAsJsonPrimitive().isBoolean()) {
                // set it to boolean
                boolean newVal = lastUpdatedToday.getAsInt() != 0;
                jsonObj.addProperty("lastUpdatedToday", newVal);
            }
        }
        JsonElement inFlow = jsonObj.get("inFlow");
        if (inFlow != null) {
            // make sure it's a boolean and not a number
            if (!inFlow.getAsJsonPrimitive().isBoolean()) {
                // set it to boolean
                boolean newVal = inFlow.getAsInt() != 0;
                jsonObj.addProperty("inFlow", newVal);
            }
        }
        Type type = new TypeToken<SessionSummary>() {}.getType();
        SessionSummary summary = SoftwareCo.gson.fromJson(jsonObj, type);
        return summary;
    }

    public static void incrementSessionSummary(KeystrokeAggregate aggregate, long sessionSeconds) {
        SessionSummary summary = getSessionSummaryData();

        long sessionMinutes = sessionSeconds / 60;
        summary.setCurrentDayMinutes(summary.getCurrentDayMinutes() + sessionMinutes);

        summary.setCurrentDayKeystrokes(summary.getCurrentDayKeystrokes() + aggregate.keystrokes);
        summary.setCurrentDayLinesAdded(summary.getCurrentDayLinesAdded() + aggregate.linesAdded);
        summary.setCurrentDayLinesRemoved(summary.getCurrentDayLinesRemoved() + aggregate.linesRemoved);

        // save the file
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static ElapsedTime getTimeBetweenLastPayload() {
        ElapsedTime eTime = new ElapsedTime();

        // default of 1 minute
        long sessionSeconds = 60;
        long elapsedSeconds = 60;

        long lastPayloadEnd = FileUtilManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        if (lastPayloadEnd > 0) {
            UtilManager.TimesData timesData = UtilManager.getTimesData();
            elapsedSeconds = Math.max(60, timesData.now - lastPayloadEnd);
            long sessionThresholdSeconds = 60 * 15;
            if (elapsedSeconds > 0 && elapsedSeconds <= sessionThresholdSeconds) {
                sessionSeconds = elapsedSeconds;
            }
            sessionSeconds = Math.max(60, sessionSeconds);
        }

        eTime.sessionSeconds = sessionSeconds;
        eTime.elapsedSeconds = elapsedSeconds;

        return eTime;
    }
}
