package com.softwareco.intellij.plugin.wallclock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.softwareco.intellij.plugin.*;
import com.softwareco.intellij.plugin.managers.FileAggregateDataManager;
import com.softwareco.intellij.plugin.event.EventManager;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.SessionSummary;
import com.softwareco.intellij.plugin.sessiondata.SessionDataManager;
import com.softwareco.intellij.plugin.timedata.TimeDataManager;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import org.apache.http.client.methods.HttpGet;

import java.lang.reflect.Type;
import java.util.logging.Logger;

public class WallClockManager {

    public static final Logger log = Logger.getLogger("WallClockManager");

    private static final int SECONDS_INCREMENT = 30;
    private static final int DAY_CHECK_TIMER_INTERVAL = 60;

    private static WallClockManager instance = null;
    private AsyncManager asyncManager = AsyncManager.getInstance();

    private static boolean dispatching = false;

    public static WallClockManager getInstance() {
        if (instance == null) {
            synchronized (log) {
                if (instance == null) {
                    instance = new WallClockManager();
                }
            }
        }
        return instance;
    }

    private WallClockManager() {
        // initialize the timer
        this.init();
    }

    private void init() {
        final Runnable wallClockTimer = () -> updateWallClockTime();
        asyncManager.scheduleService(
                wallClockTimer, "wallClockTimer", 0, SECONDS_INCREMENT);

        final Runnable newDayCheckerTimer = () -> newDayChecker();
        asyncManager.scheduleService(
                newDayCheckerTimer, "newDayCheckerTimer", 30, DAY_CHECK_TIMER_INTERVAL);
    }

    private void newDayChecker() {
        String currentDay = SoftwareCoSessionManager.getItem("currentDay", "");
        String day = SoftwareCoUtils.getTodayInStandardFormat();
        if (!day.equals(currentDay)) {
            // send the payloads
            SoftwareCoSessionManager.getInstance().sendOfflineData(true);

            // send the time data
            TimeDataManager.sendOfflineTimeData();

            // send the events data
            EventManager.sendOfflineEvents();

            // clear the wc time and the session summary and the file change info summary
            clearWcTime();
            SessionDataManager.clearSessionSummaryData();
            TimeDataManager.clearTimeDataSummary();
            FileAggregateDataManager.clearFileChangeInfoSummaryData();

            // update the current day
            SoftwareCoSessionManager.setItem("currentDay", day);

            // update the last payload timestamp
            SoftwareCoSessionManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

            // refresh the tree
            CodeTimeToolWindow.refresh();

            final Runnable service = () -> updateSessionSummaryFromServer(false);
            AsyncManager.getInstance().executeOnceInSeconds(service, 70);
        }
    }

    public void updateSessionSummaryFromServer(boolean isNewDay) {
        SessionSummary summary = SessionDataManager.getSessionSummaryData();

        String jwt = SoftwareCoSessionManager.getItem("jwt");
        String api = "/sessions/summary?refresh=true";
        SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
        if (resp.isOk()) {
            JsonObject jsonObj = resp.getJsonObj();

            JsonElement lastUpdatedToday = jsonObj.get("lastUpdatedToday");
            if (lastUpdatedToday != null) {
                // make sure it's a boolean and not a number
                if (!lastUpdatedToday.getAsJsonPrimitive().isBoolean()) {
                    // set it to boolean
                    boolean newVal = lastUpdatedToday.getAsInt() == 0 ? false : true;
                    jsonObj.addProperty("lastUpdatedToday", newVal);
                }
            }
            JsonElement inFlow = jsonObj.get("inFlow");
            if (inFlow != null) {
                // make sure it's a boolean and not a number
                if (!inFlow.getAsJsonPrimitive().isBoolean()) {
                    // set it to boolean
                    boolean newVal = inFlow.getAsInt() == 0 ? false : true;
                    jsonObj.addProperty("inFlow", newVal);
                }
            }

            Type type = new TypeToken<SessionSummary>() {}.getType();
            SessionSummary fetchedSummary = SoftwareCo.gson.fromJson(jsonObj, type);

            // clone all
            summary.clone(fetchedSummary, isNewDay);

            updateBasedOnSessionSeconds(fetchedSummary.getCurrentDayMinutes() * 60);

            // save the file
            FileManager.writeData(SessionDataManager.getSessionDataSummaryFile(), summary);
        }
    }

    private void updateWallClockTime() {
        boolean isActive = ApplicationManager.getApplication().isActive();
        if (isActive) {
            long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
            SoftwareCoSessionManager.setNumericItem("wctime", wctime);

            // update the json time data file
            TimeDataManager.updateEditorSeconds(SECONDS_INCREMENT);
        }
        dispatchStatusViewUpdate();
    }

    public void dispatchStatusViewUpdate() {
        if (!dispatching) {
            dispatching = true;

            SessionSummary summary = SessionDataManager.getSessionSummaryData();

            String icon = SoftwareCoUtils.showingStatusText() ? "paw-grey.png" : "clock.png";
            String currentDayTimeStr = SoftwareCoUtils.humanizeMinutes(summary.getCurrentDayMinutes());
            SoftwareCoUtils.updateStatusBar(
                    icon, currentDayTimeStr, "Code time today. Click to see more from Code Time.");
            // refresh the code time tree view
            CodeTimeToolWindow.refresh();
        }
        dispatching = false;
    }

    private void clearWcTime() {
        setWcTime(0);
    }

    public long getWcTimeInSeconds() {
        return SoftwareCoSessionManager.getNumericItem("wctime", 0L);
    }

    public void setWcTime(long seconds) {
        SoftwareCoSessionManager.setNumericItem("wctime", seconds);
        updateWallClockTime();
    }

    public void updateBasedOnSessionSeconds(long sessionSeconds) {
        long wcTimeVal = getWcTimeInSeconds();
        if (wcTimeVal < sessionSeconds) {
            // this will update the status bar and tree view metrics
            setWcTime((sessionSeconds));
        }
    }
}
