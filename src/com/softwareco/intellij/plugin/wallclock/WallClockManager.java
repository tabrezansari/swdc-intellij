package com.softwareco.intellij.plugin.wallclock;

import com.softwareco.intellij.plugin.AsyncManager;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.aggdata.FileAggregateDataManager;
import com.softwareco.intellij.plugin.models.TimeData;
import com.softwareco.intellij.plugin.sessiondata.SessionDataManager;
import com.softwareco.intellij.plugin.timedata.TimeDataManager;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;

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
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        String day = SoftwareCoUtils.getTodayInStandardFormat();
        if (!day.equals(currentDay)) {
            // send the payloads
            SoftwareCoSessionManager.getInstance().sendOfflineData();

            // send the time data
            TimeDataManager.sendOfflineTimeData();

            // clear the wc time and the session summary
            clearWcTime();
            SessionDataManager.clearSessionSummaryData();
            FileAggregateDataManager.clearFileChangeInfoSummaryData();

            // update the current day
            SoftwareCoSessionManager.setItem("currentDay", day);

            // update the last payload timestamp
            SoftwareCoSessionManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

            // refresh the tree
            CodeTimeToolWindow.refresh();
        }
    }

    private void updateWallClockTime() {
        long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
        SoftwareCoSessionManager.setNumericItem("wctime", wctime);
        dispatchStatusViewUpdate();

        // update the json time data file
        updateTimeData();
    }

    public void dispatchStatusViewUpdate() {
        if (!dispatching) {
            dispatching = true;
            long wcTimeVal = getWcTimeInSeconds();

            String icon = SoftwareCoUtils.showingStatusText() ? "software-paw.png" : "clock-blue.png";

            long minutes = wcTimeVal / 60;
            String currentDayTimeStr = SoftwareCoUtils.humanizeMinutes(minutes);
            SoftwareCoUtils.updateStatusBar(
                    icon, currentDayTimeStr, "Code time today vs. your daily average. Click to see more from Code Time");
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

    private void updateTimeData() {
        long editorSeconds = getWcTimeInSeconds();
        TimeData td = TimeDataManager.getTodayTimeDataSummary();

        TimeDataManager.updateTimeDataSummary(editorSeconds, td.getSession_seconds(), td.getFile_seconds());
    }

    public void updateBasedOnSessionSeconds(long sessionSeconds) {
        long wcTimeVal = getWcTimeInSeconds();
        if (wcTimeVal < sessionSeconds) {
            // this will update the status bar and tree view metrics
            setWcTime((sessionSeconds));
        } else {
            // just update the tree view metrics
            CodeTimeToolWindow.refresh();
        }
    }
}
