package com.softwareco.intellij.plugin.managers;

import com.intellij.openapi.application.ApplicationManager;
import com.softwareco.intellij.plugin.*;
import com.softwareco.intellij.plugin.models.CodeTimeSummary;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WallClockManager {

    public static final Logger LOG = Logger.getLogger("WallClockManager");

    private static final int SECONDS_INCREMENT = 30;
    private static final int DAY_CHECK_TIMER_INTERVAL = 60;

    private static WallClockManager instance = null;
    private AsyncManager asyncManager;
    private static boolean dispatching = false;

    public static WallClockManager getInstance() {
        if (instance == null) {
            synchronized (LOG) {
                if (instance == null) {
                    instance = new WallClockManager();
                }
            }
        }
        return instance;
    }

    private WallClockManager() {
        asyncManager = AsyncManager.getInstance();
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

        dispatchStatusViewUpdate();
    }

    public void newDayChecker() {
        if (SoftwareCoUtils.isNewDay()) {

            // clear the last payload we have in memory
            FileManager.clearLastSavedKeystrokeStats();

            // clear the wc time and the session summary and the file change info summary
            clearWcTime();
            SessionDataManager.clearSessionSummaryData();
            TimeDataManager.clearTimeDataSummary();
            FileAggregateDataManager.clearFileChangeInfoSummaryData();

            // update the current day
            String day = SoftwareCoUtils.getTodayInStandardFormat();
            FileManager.setItem("currentDay", day);

            // update the last payload timestamp
            FileManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

        }
    }

    private void updateWallClockTime() {
        // pass control from a background thread to the event dispatch thread,
        ApplicationManager.getApplication().invokeLater(() -> {
            boolean isActive = ApplicationManager.getApplication().isActive();
            if (isActive && SoftwareCoEventManager.isCurrentlyActive) {
                long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
                FileManager.setNumericItem("wctime", wctime);

                // update the json time data file
                TimeDataManager.incrementEditorSeconds(SECONDS_INCREMENT);
            }
            dispatchStatusViewUpdate();
        });
    }

    public synchronized void dispatchStatusViewUpdate() {
        try {
            CodeTimeSummary ctSummary = TimeDataManager.getCodeTimeSummary();

            String icon = SoftwareCoUtils.showingStatusText() ? "paw-grey.png" : "clock.png";
            String currentDayTimeStr = SoftwareCoUtils.humanizeMinutes(ctSummary.activeCodeTimeMinutes);

            // STATUS BAR REFRESH
            SoftwareCoUtils.updateStatusBar(
                    icon, currentDayTimeStr, "Code time today. Click to see more from Code Time.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Status bar update error: " + e.getMessage());
        }
        // TREE REFRESH
        CodeTimeToolWindow.refresh();
    }

    private void clearWcTime() {
        setWcTime(0);
    }

    public long getWcTimeInSeconds() {
        return FileManager.getNumericItem("wctime", 0L);
    }

    public void setWcTime(long seconds) {
        FileManager.setNumericItem("wctime", seconds);
        updateWallClockTime();
    }

}
