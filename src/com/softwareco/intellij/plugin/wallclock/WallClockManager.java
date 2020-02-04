package com.softwareco.intellij.plugin.wallclock;


import com.softwareco.intellij.plugin.AsyncManager;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.models.TimeData;
import com.softwareco.intellij.plugin.timedata.TimeDataManager;

import java.util.logging.Logger;

public class WallClockManager {

    public static final Logger log = Logger.getLogger("SoftwareCoMusicManager");

    private static final int SECONDS_INCREMENT = 30;

    private static WallClockManager instance = null;
    private AsyncManager asyncManager = AsyncManager.getInstance();

    public static WallClockManager getInstance() {
        if (instance == null) {
            instance = new WallClockManager();
        }
        return instance;
    }

    private WallClockManager() {
        // initialize the timer
        this.init();
    }

    private void init() {
        final Runnable wallClockTimer = () -> this.updateWallClockTime();
        asyncManager.scheduleService(
                wallClockTimer, "wallClockTimer", 0, SECONDS_INCREMENT);
    }

    private void updateWallClockTime() {
        long wctime = this.getWcTimeInSeconds() + SECONDS_INCREMENT;
        SoftwareCoSessionManager.setNumericItem("wctime", wctime);
        this.dispatchStatusViewUpdate();

        // TODO: refresh the code time tree view

        // update the json time data file
        this.updateTimeData();
    }

    public void dispatchStatusViewUpdate() {
        long wcTimeVal = this.getWcTimeInSeconds();

        String icon = SoftwareCoUtils.showingStatusText() ? "software-paw.png" : "clock-blue.png";

        long minutes = wcTimeVal / 60;
        String currentDayTimeStr = SoftwareCoUtils.humanizeMinutes(minutes);
        SoftwareCoUtils.updateStatusBar(icon, currentDayTimeStr, "Code time today vs. your daily average. Click to see more from Code Time");
    }

    public long getWcTimeInSeconds() {
        return SoftwareCoSessionManager.getNumericItem("wctime", 0L);
    }

    public void setWcTime(long seconds) {
        SoftwareCoSessionManager.setNumericItem("wctime", seconds);
        this.updateWallClockTime();
    }

    private void updateTimeData() {
        long editorSeconds = this.getWcTimeInSeconds();
        TimeData td = TimeDataManager.getTodayTimeDataSummary();

        TimeDataManager.updateTimeDataSummary(editorSeconds, td.getSession_seconds(), td.getFile_seconds());
    }

    private void updateBasedOnSessionSeconds(long sessionSeconds) {
        long wcTimeVal = this.getWcTimeInSeconds();
        if (wcTimeVal < sessionSeconds) {
            this.setWcTime((sessionSeconds));
        }
    }
}
