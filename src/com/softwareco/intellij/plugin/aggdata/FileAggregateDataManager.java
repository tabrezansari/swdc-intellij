package com.softwareco.intellij.plugin.aggdata;


import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.FileChangeInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FileAggregateDataManager {

    public static String getFileChangeSummaryFile() {
        String file = SoftwareCoSessionManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\fileChangeSummary.json";
        } else {
            file += "/fileChangeSummary.json";
        }
        return file;
    }

    public static void clearFileChangeInfoSummaryData() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        FileManager.writeData(getFileChangeSummaryFile(), fileInfoMap);
    }

    public static Map<String, FileChangeInfo>  getFileChangeInfo() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        JsonObject jsonObj = FileManager.getFileContentAsJson(getFileChangeSummaryFile());
        if (jsonObj != null) {
            Type type = new TypeToken<Map<String, FileChangeInfo>>() {}.getType();
            fileInfoMap = SoftwareCo.gson.fromJson(jsonObj, type);
        } else {
            // create it
            FileManager.writeData(getFileChangeSummaryFile(), fileInfoMap);
        }
        return fileInfoMap;
    }

    public static void updateFileChangeInfo(Map<String, FileChangeInfo> fileInfoMap) {
        FileManager.writeData(getFileChangeSummaryFile(), fileInfoMap);
    }
}
