package com.softwareco.intellij.plugin.managers;


import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.models.FileChangeInfo;
import swdc.java.ops.manager.FileUtilManager;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FileAggregateDataManager {

    public static void clearFileChangeInfoSummaryData() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
    }

    public static Map<String, FileChangeInfo> getFileChangeInfo() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getFileChangeSummaryFile());
        if (jsonObj != null) {
            Type type = new TypeToken<Map<String, FileChangeInfo>>() {}.getType();
            fileInfoMap = SoftwareCo.gson.fromJson(jsonObj, type);
        } else {
            // create it
            FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
        }
        return fileInfoMap;
    }

    public static void updateFileChangeInfo(Map<String, FileChangeInfo> fileInfoMap) {
        FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
    }
}
