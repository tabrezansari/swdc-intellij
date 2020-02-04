package com.softwareco.intellij.plugin.timedata;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
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

    public static void updateTimeDataSummary(long editorSeconds, long sessionSeconds, long fileSeconds) {

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        String day = SoftwareCoUtils.getTodayInStandardFormat();

        List<TimeData> timeDataList = getTimeDataList();

        // replace the current one that is found
        boolean foundIt = false;
        for (TimeData timeData : timeDataList) {
            if (timeData.getDay().equals(day)) {
                timeData.setEditor_seconds(editorSeconds);
                timeData.setFile_seconds(fileSeconds);
                timeData.setSession_seconds(sessionSeconds);
                foundIt = true;
                break;
            }
        }
        if (!foundIt) {
            TimeData td = new TimeData();
            td.setDay(day);
            td.setEditor_seconds(editorSeconds);
            td.setFile_seconds(fileSeconds);
            td.setSession_seconds(sessionSeconds);
            td.setTimestamp(timesData.utc_end_day);
            td.setTimestamp_local(timesData.local_end_day);
            timeDataList.add(td);
        }


        FileManager.writeData(getTimeDataSummaryFile(), timeDataList);
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
    public static TimeData getTodayTimeDataSummary() {
        String day = SoftwareCoUtils.getTodayInStandardFormat();

        TimeData td = null;

        List<TimeData> timeDataList = getTimeDataList();

        if (timeDataList != null && timeDataList.size() > 0) {
            for (TimeData timeData : timeDataList) {
                if (timeData.getDay().equals(day)) {
                    td = new TimeData();
                    td.clone(timeData);
                    break;
                }
            }
        }

        if (td == null) {
            td = new TimeData();
            td.setDay(day);
            if (timeDataList == null) {
                timeDataList = new ArrayList<>();
            }
            timeDataList.add(td);
            FileManager.writeData(getTimeDataSummaryFile(), timeDataList);
        }

        return td;
    }

    public static void sendOfflineTimeData() {
        FileManager.sendBatchData("/data/time", getTimeDataSummaryFile());
    }
}
