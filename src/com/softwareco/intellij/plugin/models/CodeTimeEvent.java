package com.softwareco.intellij.plugin.models;

import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.SoftwareCoUtils;

import java.util.TimeZone;

public class CodeTimeEvent {
    private String type = "";
    private String name = "";
    private long timestamp = 0L;
    private long timestamp_local = 0L;
    private String description = "";
    private int pluginId = SoftwareCoUtils.pluginId;
    private String os = SoftwareCoUtils.getOs();
    private String version = SoftwareCo.getVersion();
    private String hostname = SoftwareCoUtils.getHostname();
    private String timezone = TimeZone.getDefault().getID();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp_local() {
        return timestamp_local;
    }

    public void setTimestamp_local(long timestamp_local) {
        this.timestamp_local = timestamp_local;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPluginId() {
        return pluginId;
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    public String getHostname() {
        return hostname;
    }

    public String getTimezone() {
        return timezone;
    }
}
