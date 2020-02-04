package com.softwareco.intellij.plugin.models;

public class FileChangeInfo {
    private String name = "";
    private String fsPath = "";
    private String projectDir = "";
    private long kpm = 0L;
    private long keystrokes = 0L;
    private long add = 0L;
    private long netkeys = 0L;
    private long paste = 0L;
    private long open = 0L;
    private long close = 0L;
    private long delete = 0L;
    private long length = 0L;
    private long lines = 0L;
    private long linesAdded = 0L;
    private long linesRemoved = 0L;
    private String syntax = "";
    private long fileAgeDays = 0L;
    private long repoFileContributorCount = 0L;
    private long start = 0L;
    private long end = 0L;
    private long local_start = 0L;
    private long local_end = 0L;
    private long update_count = 0L;
    private long duration_seconds = 0L;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFsPath() {
        return fsPath;
    }

    public void setFsPath(String fsPath) {
        this.fsPath = fsPath;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public long getKpm() {
        return kpm;
    }

    public void setKpm(long kpm) {
        this.kpm = kpm;
    }

    public long getKeystrokes() {
        return keystrokes;
    }

    public void setKeystrokes(long keystrokes) {
        this.keystrokes = keystrokes;
    }

    public long getAdd() {
        return add;
    }

    public void setAdd(long add) {
        this.add = add;
    }

    public long getNetkeys() {
        return netkeys;
    }

    public void setNetkeys(long netkeys) {
        this.netkeys = netkeys;
    }

    public long getPaste() {
        return paste;
    }

    public void setPaste(long paste) {
        this.paste = paste;
    }

    public long getOpen() {
        return open;
    }

    public void setOpen(long open) {
        this.open = open;
    }

    public long getClose() {
        return close;
    }

    public void setClose(long close) {
        this.close = close;
    }

    public long getDelete() {
        return delete;
    }

    public void setDelete(long delete) {
        this.delete = delete;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getLines() {
        return lines;
    }

    public void setLines(long lines) {
        this.lines = lines;
    }

    public long getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(long linesAdded) {
        this.linesAdded = linesAdded;
    }

    public long getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(long linesRemoved) {
        this.linesRemoved = linesRemoved;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public long getFileAgeDays() {
        return fileAgeDays;
    }

    public void setFileAgeDays(long fileAgeDays) {
        this.fileAgeDays = fileAgeDays;
    }

    public long getRepoFileContributorCount() {
        return repoFileContributorCount;
    }

    public void setRepoFileContributorCount(long repoFileContributorCount) {
        this.repoFileContributorCount = repoFileContributorCount;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getLocal_start() {
        return local_start;
    }

    public void setLocal_start(long local_start) {
        this.local_start = local_start;
    }

    public long getLocal_end() {
        return local_end;
    }

    public void setLocal_end(long local_end) {
        this.local_end = local_end;
    }

    public long getUpdate_count() {
        return update_count;
    }

    public void setUpdate_count(long update_count) {
        this.update_count = update_count;
    }

    public long getDuration_seconds() {
        return duration_seconds;
    }

    public void setDuration_seconds(long duration_seconds) {
        this.duration_seconds = duration_seconds;
    }
}
