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
            file += "\\timeDataSummary.json";
        } else {
            file += "/timeDataSummary.json";
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
            TimeData td = getTodayTimeDataSummary(activeProject.getBasePath());
            td.setEditor_seconds(td.getEditor_seconds() + editorSeconds);
            td.setTimestamp_local(timesData.local_now);
            saveTimeDataSummaryToDisk(td);
        }
    }

    public static void incrementSessionAndFileSeconds(long minutesSincePayload) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Project activeProject = SoftwareCoUtils.getFirstActiveProject();
        if (activeProject != null) {
            TimeData td = getTodayTimeDataSummary(activeProject.getBasePath());

            long sessionSeconds = minutesSincePayload * 60;
            td.setSession_seconds(sessionSeconds);
            td.setFile_seconds(td.getFile_seconds() + 60);

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
    public static TimeData getTodayTimeDataSummary(String directory) {
        String day = SoftwareCoUtils.getTodayInStandardFormat();


        List<TimeData> timeDataList = getTimeDataList();

        if (timeDataList != null && timeDataList.size() > 0) {
            for (TimeData timeData : timeDataList) {
                if (timeData.getDay().equals(day) && timeData.getProject().getDirectory().equals(directory)) {
                    // return it
                    return timeData;
                }
            }
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Project p = SoftwareCoUtils.getProjectForPath(directory);

        TimeData td = new TimeData();
        td.setDay(day);
        td.setTimestamp_local(timesData.local_now);
        td.setTimestamp(timesData.now);
        if (p != null) {
            td.setProject(new KeystrokeProject(directory, directory));
        } else {
            td.setProject(new KeystrokeProject(p.getName(), p.getBasePath()));
        }
        if (timeDataList == null) {
            timeDataList = new ArrayList<>();
        }
        timeDataList.add(td);
        // write it then return it
        FileManager.writeData(getTimeDataSummaryFile(), timeDataList);
        return td;
    }

    public static void sendOfflineTimeData() {
        FileManager.sendBatchData("/data/time", getTimeDataSummaryFile());
    }

    private static void saveTimeDataSummaryToDisk(TimeData timeData) {
        if (timeData.getProject() == null) {
            // set it
            Project p = SoftwareCoUtils.getFirstActiveProject();
            if (p != null) {
                timeData.setProject(new KeystrokeProject(p.getName(), p.getBasePath()));
            } else {
                timeData.setProject(new KeystrokeProject("Unammed", "Untitled"));
            }
        }
        String dir = timeData.getProject().getDirectory();

        // get the list and add or update it
        List<TimeData> timeDataList = getTimeDataList();

        // new list to save
        List<TimeData> listToSave = new ArrayList<>();
        listToSave.add(timeData);

        if (timeDataList != null && timeDataList.size() > 0) {
            for (TimeData td : timeDataList) {
                if (td.getProject() != null &&
                        !td.getProject().getDirectory().equals(dir)) {
                    listToSave.add(td);
                }
            }
        }

        FileManager.writeData(getTimeDataSummaryFile(), listToSave);
    }
}
