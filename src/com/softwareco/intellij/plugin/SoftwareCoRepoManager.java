package com.softwareco.intellij.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.softwareco.intellij.plugin.models.ResourceInfo;
import com.softwareco.intellij.plugin.repo.GitUtil;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

public class SoftwareCoRepoManager {

    public static final Logger log = Logger.getLogger("SoftwareCoRepoManager");

    private static SoftwareCoRepoManager instance = null;

    private final JsonObject currentTrack = new JsonObject();

    public static SoftwareCoRepoManager getInstance() {
        if (instance == null) {
            instance = new SoftwareCoRepoManager();
        }
        return instance;
    }

    public JsonObject getLatestCommit(String projectDir) {
        ResourceInfo resource = GitUtil.getResourceInfo(projectDir);
        if (resource != null && resource.getIdentifier() != null) {
            String identifier = resource.getIdentifier();
            String tag = resource.getTag();
            String branch = resource.getBranch();

            try {
                String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
                String encodedTag = URLEncoder.encode(tag, "UTF-8");
                String encodedBranch = URLEncoder.encode(branch, "UTF-8");

                String qryString = "identifier=" + encodedIdentifier;
                qryString += "&tag=" + encodedTag;
                qryString += "&branch=" + encodedBranch;

                SoftwareResponse responseData = SoftwareCoUtils.makeApiCall("/commits/latest?" + qryString, HttpGet.METHOD_NAME, null);
                if (responseData != null && responseData.isOk()) {
                    JsonObject payload = responseData.getJsonObj();
                    // will get a single commit object back with the following attributes
                    // commitId, message, changes, timestamp
                    JsonObject latestCommit = payload.get("commit").getAsJsonObject();
                    return latestCommit;
                } else {
                    log.info(SoftwareCoUtils.pluginName + ": Unable to fetch latest commit info");
                }
            } catch (Exception e) {
                //
            }
        }
        return null;
    }

    public void getHistoricalCommits(String projectDir) {
        ResourceInfo resource = GitUtil.getResourceInfo(projectDir);
        if (resource != null && resource.getIdentifier() != null) {
            String identifier = resource.getIdentifier();
            String tag = resource.getTag();
            String branch = resource.getBranch();
            String email = resource.getEmail();

            JsonObject latestCommit = getLatestCommit(projectDir);

            String sinceOption = "";
            if (latestCommit != null && latestCommit.has("timestamp")) {
                long unixTs = latestCommit.get("timestamp").getAsLong();
                sinceOption = "--since=" + unixTs;
            } else {
                sinceOption = "--max-count=100";
            }

            String authorOption = "--author=" + email;

            String[] cmdList = {"git", "log", "--stat", "--pretty=COMMIT:%H,%ct,%cI,%s", authorOption, sinceOption};

            List<String> results = SoftwareCoUtils.getResultsForCommandArgs(cmdList, projectDir);

            if (results == null || results.size() == 0) {
                return;
            }

            String latestCommitId = (latestCommit != null && latestCommit.has("commitId")) ?
                    latestCommit.get("commitId").getAsString() : null;

            // split the content
            JsonArray commits = new JsonArray();
            JsonObject commit = null;

            for (String line : results) {
                line = line.trim();
                if (line.indexOf("COMMIT:") == 0) {
                    line = line.substring("COMMIT:".length());
                    if (commit != null) {
                        commits.add(commit);
                    }
                    // split by comma
                    String[] commitInfos = line.split(",");
                    if (commitInfos != null && commitInfos.length > 3) {
                        String commitId = commitInfos[0].trim();
                        if (latestCommitId != null && commitId.equals(latestCommitId)) {
                            commit = null;
                            // go to the next one
                            continue;
                        }
                        long timestamp = Long.valueOf(commitInfos[1].trim());
                        String date = commitInfos[2].trim();
                        String message = commitInfos[3].trim();
                        commit = new JsonObject();
                        commit.addProperty("commitId", commitId);
                        commit.addProperty("timestamp", timestamp);
                        commit.addProperty("date", date);
                        commit.addProperty("message", message);
                        JsonObject sftwTotalsObj = new JsonObject();
                        sftwTotalsObj.addProperty("insertions", 0);
                        sftwTotalsObj.addProperty("deletions", 0);
                        JsonObject changesObj = new JsonObject();
                        changesObj.add("__sftwTotal__", sftwTotalsObj);
                        commit.add("changes", changesObj);
                    }
                } else if (commit != null && line.indexOf("|") != -1) {
                    line = line.replaceAll("\\s+"," ");
                    String[] lineInfos = line.split("|");

                    if (lineInfos != null && lineInfos.length > 1) {
                        String file = lineInfos[0].trim();
                        String metricsLine = lineInfos[1].trim();
                        String[] metricInfos = metricsLine.split(" ");
                        if (metricInfos != null && metricInfos.length > 1) {
                            String addAndDeletes = metricInfos[1].trim();
                            // count the number of plus signs and negative signs to find
                            // out how many additions and deletions per file
                            int len = addAndDeletes.length();
                            int lastPlusIdx = addAndDeletes.lastIndexOf("+");
                            int insertions = 0;
                            int deletions = 0;
                            if (lastPlusIdx != -1) {
                                insertions = lastPlusIdx + 1;
                                deletions = len - insertions;
                            } else if (len > 0) {
                                // all deletions
                                deletions = len;
                            }
                            JsonObject fileChanges = new JsonObject();
                            fileChanges.addProperty("insertions", insertions);
                            fileChanges.addProperty("deletions", deletions);
                            JsonObject changesObj = commit.get("changes").getAsJsonObject();
                            changesObj.add(file, fileChanges);

                            JsonObject swftTotalsObj = changesObj.get("__sftwTotal__").getAsJsonObject();
                            int insertionTotal = swftTotalsObj.get("insertions").getAsInt() + insertions;
                            int deletionsTotal = swftTotalsObj.get("deletions").getAsInt() + deletions;
                            swftTotalsObj.addProperty("insertions", insertionTotal);
                            swftTotalsObj.addProperty("deletions", deletionsTotal);
                        }
                    }
                }
            }

            if (commit != null) {
                commits.add(commit);
            }

            int batch_size = 10;
            if (commits != null && commits.size() > 0) {
                // send it in batches of batch_size
                JsonArray batch = new JsonArray();
                for (int i = 0; i < commits.size(); i++) {
                    batch.add(commits.get(i));
                    if (i > 0 && i % batch_size == 0) {
                        this.processCommits(batch, identifier, tag, branch);
                        batch = new JsonArray();
                    }
                }
                if (batch.size() > 0) {
                    this.processCommits(batch, identifier, tag, branch);
                }
            }

        }
    }

    private void processCommits(JsonArray commits, String identifier, String tag, String branch) {
        try {

            // send the commits
            JsonObject commitData = new JsonObject();
            commitData.add("commits", commits);
            commitData.addProperty("identifier", identifier);
            commitData.addProperty("tag", tag);
            commitData.addProperty("branch", branch);
            String commitDataStr = commitData.toString();

            SoftwareResponse resp = SoftwareCoUtils.makeApiCall(
                            "/commits", HttpPost.METHOD_NAME, commitDataStr);

            if (resp != null && resp.getJsonObj() != null) {

                // {"status":"success","message":"Updated commits"}
                // {"status":"failed","data":"Unable to process commits data"}
                JsonObject responseObj = resp.getJsonObj();
                String message = "";
                if (responseObj.has("data")) {
                    JsonObject data = responseObj.get("data").getAsJsonObject();
                    message = data.get("message").getAsString();
                } else if (responseObj.has("message")) {
                    message = responseObj.get("message").getAsString();
                }

                log.info(SoftwareCoUtils.pluginName + ": completed commits update - " + message);
            } else {
                log.info(SoftwareCoUtils.pluginName + ": Unable to process repo commits");
            }
        } catch (Exception e) {
            log.warning(SoftwareCoUtils.pluginName + ": Unable to process repo commits, error: " + e.getMessage());
        }
    }

    public void processRepoMembersInfo(final String projectDir) {
        ResourceInfo resource = GitUtil.getResourceInfo(projectDir);
        if (resource != null && resource.getIdentifier() != null) {

            if (resource.getMembers().size() > 0) {
                // send the members
                try {
                    JsonObject repoData = new JsonObject();
                    repoData.add("members", resource.getJsonMembers());
                    repoData.addProperty("identifier", resource.getIdentifier());
                    repoData.addProperty("tag", resource.getTag());
                    repoData.addProperty("branch", resource.getBranch());
                    String repoDataStr = repoData.toString();
                    JsonObject responseData = SoftwareCoUtils.makeApiCall(
                                    "/repo/members", HttpPost.METHOD_NAME, repoDataStr).getJsonObj();

                    // {"status":"success","message":"Updated repo members"}
                    // {"status":"failed","data":"Unable to process repo information"}
                    if (responseData != null && responseData.has("message")) {
                        log.info("Code Time: " + responseData.get("message").getAsString());
                    } else if (responseData != null && responseData.has("data")) {
                        log.info(SoftwareCoUtils.pluginName + ": " + responseData.get("data").getAsString());
                    } else {
                        log.info(SoftwareCoUtils.pluginName + ": Unable to process repo member metrics");
                    }
                } catch (Exception e) {
                    log.warning(SoftwareCoUtils.pluginName + ": Unable to process repo member metrics, error: " + e.getMessage());
                }
            }
        }
    }
}
