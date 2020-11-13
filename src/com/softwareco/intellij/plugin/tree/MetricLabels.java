package com.softwareco.intellij.plugin.tree;

import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.models.CodeTimeSummary;
import com.softwareco.intellij.plugin.models.SessionSummary;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MetricLabels {
    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");

    public String keystrokes = "";
    public String keystrokesAvg = "";
    public String keystrokesGlobalAvg = "";
    public String keystrokesAvgIcon = "";
    public String linesAdded = "";
    public String linesAddedAvg = "";
    public String linesAddedGlobalAvg = "";
    public String linesAddedAvgIcon = "";
    public String linesRemoved = "";
    public String linesRemovedAvg = "";
    public String linesRemovedGlobalAvg = "";
    public String linesRemovedAvgIcon = "";
    public String activeCodeTime = "";
    public String activeCodeTimeAvg = "";
    public String activeCodeTimeGlobalAvg = "";
    public String activeCodeTimeAvgIcon = "";
    public String codeTime = "";

    public void updateLabels(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        String dayStr = formatDay.format(new Date());

        if (sessionSummary != null) {
            keystrokes = "Today: " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayKeystrokes());
            keystrokesAvg = "Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageDailyKeystrokes());
            keystrokesGlobalAvg = "Global average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageDailyKeystrokes());
            keystrokesAvgIcon = sessionSummary.getAverageDailyKeystrokes() < sessionSummary.getCurrentDayKeystrokes() ? "bolt.png" : "bolt-grey.png";

            linesAdded = "Today: " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayLinesAdded());
            linesAddedAvg = "Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageLinesAdded());
            linesAddedGlobalAvg = "Global average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
            linesAddedAvgIcon = sessionSummary.getAverageLinesAdded() < sessionSummary.getCurrentDayLinesAdded() ? "bolt.png" : "bolt-grey.png";

            linesRemoved = "Today: " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getCurrentDayLinesRemoved());
            linesRemovedAvg = "Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getAverageLinesRemoved());
            linesRemovedGlobalAvg = "Global average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesRemoved());
            linesRemovedAvgIcon = sessionSummary.getAverageLinesRemoved() < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.png";
        }

        if (codeTimeSummary != null && sessionSummary != null) {
            activeCodeTime = "Today: " + SoftwareCoUtils.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes);
            activeCodeTimeAvg = "Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeMinutes((int) sessionSummary.getAverageDailyMinutes());
            activeCodeTimeGlobalAvg = "Global average (" + dayStr + "): " + SoftwareCoUtils.humanizeMinutes((int) sessionSummary.getGlobalAverageDailyMinutes());
            activeCodeTimeAvgIcon = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.png" : "bolt-grey.png";

            codeTime = "Today: " + SoftwareCoUtils.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        }
    }
}