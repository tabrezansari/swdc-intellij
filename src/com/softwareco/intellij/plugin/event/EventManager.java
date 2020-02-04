package com.softwareco.intellij.plugin.event;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.CodeTimeEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EventManager {

    private static String getPluginEventsFile() {
        String file = SoftwareCoSessionManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\events.json";
        } else {
            file += "/events.json";
        }
        return file;
    }

    private static List<CodeTimeEvent> getTimeDataList() {
        JsonArray jsonArr = FileManager.getFileContentAsJsonArray(getPluginEventsFile());
        Type listType = new TypeToken<List<CodeTimeEvent>>() {}.getType();
        List<CodeTimeEvent> list = SoftwareCo.gson.fromJson(jsonArr, listType);
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public static void createCodeTimeEvent(String type, String name, String description) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        CodeTimeEvent event = new CodeTimeEvent();
        event.setTimestamp(timesData.now);
        event.setTimestamp_local(timesData.local_now);
        event.setType(type);
        event.setName(name);
        event.setDescription(description);
        FileManager.appendData(getPluginEventsFile(), event);
    }

    public static void sendOfflineEvents() {
        FileManager.sendBatchData("/data/event", getPluginEventsFile());
    }
}
