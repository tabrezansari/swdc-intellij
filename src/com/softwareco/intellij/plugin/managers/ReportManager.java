package com.softwareco.intellij.plugin.managers;

import com.intellij.openapi.project.Project;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.models.CommitChangeStats;
import com.softwareco.intellij.plugin.repo.GitUtil;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReportManager {

    private static final int DASHBOARD_LABEL_WIDTH = 28;
    private static final int DASHBOARD_VALUE_WIDTH = 36;
    private static final int DASHBOARD_COL_WIDTH = 21;
    private static final int DASHBOARD_LRG_COL_WIDTH = 38;
    private static final int TABLE_WIDTH = 80;

    private static final SimpleDateFormat formatDayTime = new SimpleDateFormat("EEE, MMM d h:mma");
    private static final SimpleDateFormat formatDayYear = new SimpleDateFormat("MMM d, YYYY");

    public static void displayProjectContributorSummaryDashboard(String identifier) {
        StringBuffer sb = new StringBuffer();
        String file = FileUtilManager.getProjectContributorSummaryFile();

        Project p = SoftwareCoUtils.getFirstActiveProject();
        if (p != null) {
            UtilManager.TimesData timesData = UtilManager.getTimesData();
            String email = GitUtil.getUsersEmail(p.getBasePath());
            CommitChangeStats usersTodaysCommits = GitUtil.getTodaysCommits(p.getBasePath(), email);
            CommitChangeStats contribTodaysCommits = GitUtil.getTodaysCommits(p.getBasePath(), null);

            CommitChangeStats usersYesterdaysCommits = GitUtil.getYesterdaysCommits(p.getBasePath(), email);
            CommitChangeStats contribYesterdaysCommits = GitUtil.getYesterdaysCommits(p.getBasePath(), null);

            CommitChangeStats usersThisWeeksCommits = GitUtil.getThisWeeksCommits(p.getBasePath(), email);
            CommitChangeStats contribThisWeeksCommits = GitUtil.getThisWeeksCommits(p.getBasePath(), null);

            String lastUpdatedStr = formatDayTime.format(new Date());
            sb.append(getTableHeader("PROJECT SUMMARY", " (Last updated on " + lastUpdatedStr + ")", true));
            sb.append("\n\n Project: ").append(identifier).append("\n\n");

            // TODAY
            String projectDate = formatDayYear.format(timesData.local_start_today_date);
            sb.append(getRightAlignedTableHeader("Today (" + projectDate + ")"));
            sb.append(getColumnHeaders(Arrays.asList("Metric", "You", "All Contributors")));
            sb.append(getRowNumberData("Commits", usersTodaysCommits.getCommitCount(), contribTodaysCommits.getCommitCount()));
            sb.append(getRowNumberData("Files changed", usersTodaysCommits.getFileCount(), contribTodaysCommits.getFileCount()));
            sb.append(getRowNumberData("Insertions", usersTodaysCommits.getInsertions(), contribTodaysCommits.getInsertions()));
            sb.append(getRowNumberData("Deletions", usersTodaysCommits.getDeletions(), contribTodaysCommits.getDeletions()));
            sb.append("\n");

            // YESTERDAY
            String yesterday = formatDayYear.format(timesData.local_start_of_yesterday_date);
            sb.append(getRightAlignedTableHeader("Yesterday (" + yesterday + ")"));
            sb.append(getColumnHeaders(Arrays.asList("Metric", "You", "All Contributors")));
            sb.append(getRowNumberData("Commits", usersYesterdaysCommits.getCommitCount(), contribYesterdaysCommits.getCommitCount()));
            sb.append(getRowNumberData("Files changed", usersYesterdaysCommits.getFileCount(), contribYesterdaysCommits.getFileCount()));
            sb.append(getRowNumberData("Insertions", usersYesterdaysCommits.getInsertions(), contribYesterdaysCommits.getInsertions()));
            sb.append(getRowNumberData("Deletions", usersYesterdaysCommits.getDeletions(), contribYesterdaysCommits.getDeletions()));
            sb.append("\n");

            // THIS WEEK
            String startOfWeek = formatDayYear.format(timesData.local_start_of_week_date);
            sb.append(getRightAlignedTableHeader("This week (" + startOfWeek + " to " + projectDate + ")"));
            sb.append(getColumnHeaders(Arrays.asList("Metric", "You", "All Contributors")));
            sb.append(getRowNumberData("Commits", usersThisWeeksCommits.getCommitCount(), contribThisWeeksCommits.getCommitCount()));
            sb.append(getRowNumberData("Files changed", usersThisWeeksCommits.getFileCount(), contribThisWeeksCommits.getFileCount()));
            sb.append(getRowNumberData("Insertions", usersThisWeeksCommits.getInsertions(), contribThisWeeksCommits.getInsertions()));
            sb.append(getRowNumberData("Deletions", usersThisWeeksCommits.getDeletions(), contribThisWeeksCommits.getDeletions()));
            sb.append("\n");

        } else {
            sb.append("Project information not found");
        }

        // write the summary content
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write(sb.toString());
        } catch (IOException ex) {
            // Report
        } finally {
            try {writer.close();} catch (Exception ex) {/*ignore*/}
        }

        SoftwareCoUtils.launchFile(file);
    }

    private static String getRowNumberData(String title, long userStat, long contribStat) {
        String userStatStr = UtilManager.humanizeLongNumbers(userStat);
        String contribStatStr = UtilManager.humanizeLongNumbers(contribStat);
        List<String> labels = Arrays.asList(title, userStatStr, contribStatStr);
        return getRowLabels(labels);
    }

    private static String getSpaces(int spacesRequired) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < spacesRequired; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static String getBorder(int borderLen) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < borderLen; i++) {
            sb.append("-");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String getDashboardRow(String label, String value, boolean isSectionHeader) {
        int spacesRequired = DASHBOARD_LABEL_WIDTH - label.length();
        String spaces = getSpaces(spacesRequired);
        String dashboardVal = getDashboardValue(value, isSectionHeader);
        StringBuffer sb = new StringBuffer();
        sb.append(label).append(spaces).append(dashboardVal).append("\n");
        int currLen = sb.length();
        sb.append(getBorder(currLen));
        return sb.toString();
    }

    private static String getDashboardValue(String value, boolean isSectionHeader) {
        int spacesRequired = DASHBOARD_VALUE_WIDTH - value.length() - 2;
        String spaces = getSpaces(spacesRequired);
        if (!isSectionHeader) {
            // show the : divider
            return ": " + spaces + "" + value;
        }
        // we won't show the column divider
        return "  " + spaces + "" + value;
    }

    private static String getDashboardBottomBorder() {
        int borderLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH;
        String border = getBorder(borderLen);
        // add an additional newline
        return border + "\n";
    }

    private static String getSectionHeader(String label) {
        StringBuffer sb = new StringBuffer();
        sb.append(label).append("\n");
        int borderLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH;
        sb.append(getBorder(borderLen));
        return sb.toString();
    }

    private static String getRightAlignedTableLabel(String label, int colWidth) {
        int spacesRequired = colWidth - label.length();
        String spaces = getSpaces(spacesRequired);
        return spaces + "" + label;
    }

    private static String getTableHeader(String leftLabel, String rightLabel, boolean isFullTable) {
        int fullLen = !isFullTable ? TABLE_WIDTH - DASHBOARD_COL_WIDTH : TABLE_WIDTH;
        int spacesRequired = fullLen - leftLabel.length() - rightLabel.length();
        String spaces = getSpaces(spacesRequired);
        return leftLabel + "" + spaces + "" + rightLabel;
    }

    private static String getRightAlignedTableHeader(String label) {
        StringBuffer sb = new StringBuffer();
        String alignedHeader = getRightAlignedTableLabel(label, TABLE_WIDTH);
        sb.append(alignedHeader).append("\n");
        sb.append(getBorder(TABLE_WIDTH));
        return sb.toString();
    }

    private static String getRowLabels(List<String> labels) {
        StringBuffer sb = new StringBuffer();
        int spacesRequired = 0;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (i == 0) {
                sb.append(label);
                spacesRequired = DASHBOARD_COL_WIDTH - sb.length() - 1;
                sb.append(getSpaces(spacesRequired)).append(":");
            } else if (i == 1) {
                spacesRequired = DASHBOARD_LRG_COL_WIDTH + DASHBOARD_COL_WIDTH - sb.length() - label.length() - 1;
                sb.append(getSpaces(spacesRequired)).append(label).append(" ");
            } else {
                spacesRequired = DASHBOARD_COL_WIDTH - label.length() - 2;
                sb.append("| ").append(getSpaces(spacesRequired)).append(label);
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String getColumnHeaders(List<String> labels) {
        StringBuffer sb = new StringBuffer();
        int spacesRequired = 0;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (i == 0) {
                sb.append(label);
            } else if (i == 1) {
                spacesRequired = DASHBOARD_LRG_COL_WIDTH + DASHBOARD_COL_WIDTH - sb.length() - label.length() - 1;
                sb.append(getSpaces(spacesRequired)).append(label).append(" ");
            } else {
                spacesRequired = DASHBOARD_COL_WIDTH - label.length() - 2;
                sb.append("| ").append(getSpaces(spacesRequired)).append(label);
            }
        }
        sb.append("\n");
        sb.append(getBorder(TABLE_WIDTH));
        return sb.toString();
    }

}
