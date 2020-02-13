package com.softwareco.intellij.plugin.event;

import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.fs.FileManager;
import com.softwareco.intellij.plugin.models.CodeTimeEvent;

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
