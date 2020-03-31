package com.softwareco.intellij.plugin.timedata;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.project.Project;
import com.softwareco.intellij.plugin.KeystrokeProject;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.TimeData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Time data is saved on disk as a list of time data objects
 */
public class TimeDataManager {

    private static String getTimeDataSummaryFile() {
        String file = SoftwareCoSessionManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\projectTimeData.json";
        } else {
            file += "/projectTimeData.json";
        }
        return file;
    }

    public static void clearTimeDataSummary() {
        TimeData td = new TimeData();
        List<TimeData> tdList = new ArrayList<>();
        tdList.add(td);
        FileManager.writeData(getTimeDataSummaryFile(), tdList);
    }

    public static void updateEditorSeconds(long editorSeconds) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Project activeProject = SoftwareCoUtils.getFirstActiveProject();
        if (activeProject != null) {
            KeystrokeProject project = new KeystrokeProject(activeProject.getName(), activeProject.getBasePath());
            TimeData td = getTodayTimeDataSummary(project);
            if (td != null) {
                td.setEditor_seconds(td.getEditor_seconds() + editorSeconds);
                td.setTimestamp_local(timesData.local_now);

                td.setEditor_seconds(Math.max(
                        td.getEditor_seconds(),
                        td.getSession_seconds()));

                saveTimeDataSummaryToDisk(td);
            }
        }
    }

    public static void incrementSessionAndFileSeconds(KeystrokeProject project, long minutesSincePayload) {

        TimeData td = getTodayTimeDataSummary(project);
        if (td != null) {
            long sessionSeconds = minutesSincePayload * 60;
            td.setSession_seconds(sessionSeconds);
            td.setFile_seconds(td.getFile_seconds() + 60);

            td.setEditor_seconds(Math.max(
                    td.getEditor_seconds(),
                    td.getSession_seconds()));
            td.setFile_seconds(Math.min(
                    td.getFile_seconds(),
                    td.getSession_seconds()));

            saveTimeDataSummaryToDisk(td);
        }
    }

    private static List<TimeData> getTimeDataList() {
        JsonArray jsonArr = FileManager.getFileContentAsJsonArray(getTimeDataSummaryFile());
        Type listType = new TypeToken<List<TimeData>>() {}.getType();
        List<TimeData> timeDataList = SoftwareCo.gson.fromJson(jsonArr, listType);
        if (timeDataList == null) {
            timeDataList = new ArrayList<>();
        }
        return timeDataList;
    }

    /**
     * Get the current time data info that is saved on disk. If not found create an empty one.
     * @return
     */
    public static TimeData getTodayTimeDataSummary(KeystrokeProject p) {
        if (p == null || p.getDirectory() == null) {
            return null;
        }
        String day = SoftwareCoUtils.getTodayInStandardFormat();

        List<TimeData> timeDataList = getTimeDataList();

        if (timeDataList != null && timeDataList.size() > 0) {
            for (TimeData timeData : timeDataList) {
                if (timeData.getDay().equals(day) && timeData.getProject().getDirectory().equals(p.getDirectory())) {
                    // return it
                    return timeData;
                }
            }
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

        TimeData td = new TimeData();
        td.setDay(day);
        td.setTimestamp_local(timesData.local_now);
        td.setTimestamp(timesData.now);
        td.setProject(p.clone());

        if (timeDataList == null) {
            timeDataList = new ArrayList<>();
        }

        timeDataList.add(td);
        // write it then return it
        FileManager.writeData(getTimeDataSummaryFile(), timeDataList);
        return td;
    }

    public static void sendOfflineTimeData() {
        FileManager.sendJsonArrayData(getTimeDataSummaryFile(), "/data/time");
    }

    private static void saveTimeDataSummaryToDisk(TimeData timeData) {
        if (timeData == null) {
            return;
        }
        String dir = timeData.getProject().getDirectory();

        // get the existing list
        List<TimeData> timeDataList = getTimeDataList();

        // new list to save
        List<TimeData> listToSave = new ArrayList<>();
        // add it to the new list
        listToSave.add(timeData);

        if (timeDataList != null && timeDataList.size() > 0) {
            for (TimeData td : timeDataList) {
                if (td.getProject() != null &&
                        !td.getDay().equals(timeData.getDay()) &&
                        !td.getProject().getDirectory().equals(dir)) {
                    // add it back to the list to save. it doesn't match the
                    // incoming timeData and day, and the project is also available
                    listToSave.add(td);
                }
            }
        }

        // write it all
        FileManager.writeData(getTimeDataSummaryFile(), listToSave);
    }
}
