package com.softwareco.intellij.plugin.repo;


import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.models.CommitChangeStats;
import com.softwareco.intellij.plugin.models.CommitInfo;
import com.softwareco.intellij.plugin.models.ResourceInfo;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GitUtil {

    public static CommitChangeStats accumulateStatChanges(List<String> results, boolean committedChanges) {
        CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

        if (results != null) {
            for (String line : results) {
                line = line.trim();
                if (line.indexOf("changed") != -1 &&
                        (line.indexOf("insertion") != -1 || line.indexOf("deletion") != -1)) {
                    String[] parts = line.split(" ");
                    // the 1st element is the number of files changed
                    int fileCount = Integer.parseInt(parts[0]);
                    changeStats.setFileCount(fileCount);
                    changeStats.setCommitCount(changeStats.getCommitCount() + 1);
                    for (int x = 1; x < parts.length; x++) {
                        String part = parts[x];
                        if (part.indexOf("insertion") != -1) {
                            int insertions = Integer.parseInt(parts[x - 1]);
                            changeStats.setInsertions(changeStats.getInsertions() + insertions);
                        } else if (part.indexOf("deletion") != -1) {
                            int deletions = Integer.parseInt(parts[x - 1]);
                            changeStats.setDeletions(changeStats.getDeletions() + deletions);
                        }
                    }
                }
            }
        }

        return changeStats;
    }

    public static CommitChangeStats getChangeStats(List<String> cmdList, String projectDir, boolean committedChanges) {
        CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

        if (projectDir == null) {
            return changeStats;
        }

        /**
         * example:
         * -mbp-2:swdc-vscode xavierluiz$ git diff --stat
         lib/KpmProviderManager.ts | 22 ++++++++++++++++++++--
         1 file changed, 20 insertions(+), 2 deletions(-)

         for multiple files it will look like this...
         7 files changed, 137 insertions(+), 55 deletions(-)
         */
        List<String> resultList = SoftwareCoUtils.getCommandResult(cmdList, projectDir);

        if (resultList == null || resultList.size() == 0) {
            // something went wrong, but don't try to parse a null or undefined str
            return changeStats;
        }

        // just look for the line with "insertions" and "deletions"
        changeStats = accumulateStatChanges(resultList, committedChanges);

        return changeStats;
    }

    public static CommitChangeStats getUncommitedChanges(String projectDir) {
        CommitChangeStats changeStats = new CommitChangeStats(false);

        if (projectDir == null) {
            return changeStats;
        }

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("git");
        cmdList.add("diff");
        cmdList.add("--stat");

        return getChangeStats(cmdList, projectDir, false);
    }

    public static CommitChangeStats getTodaysCommits(String projectDir) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (projectDir == null) {
            return changeStats;
        }

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        long starOfDay = timesData.local_start_day;

        ResourceInfo resourceInfo = SoftwareCoUtils.getResourceInfo(projectDir);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("git");
        cmdList.add("log");
        cmdList.add("--stat");
        cmdList.add("--pretty=COMMIT:%H,%ct,%cI,%s");
        cmdList.add("--since=" + starOfDay);
        if (resourceInfo != null && resourceInfo.getEmail() != null && !resourceInfo.getEmail().isEmpty()) {
            cmdList.add("--author=" + resourceInfo.getEmail());
        }

        return getChangeStats(cmdList, projectDir, true);
    }

    public static String getRepoUrlLink(String projectDir) {
        String[] cmdList = { "git", "config", "--get", "remote.origin.url" };

        // should only be a result of 1
        List<String> resultList = SoftwareCoUtils.getCommandResultForCmd(cmdList, projectDir);
        String url = resultList != null && resultList.size() > 0 ? resultList.get(0) : null;
        if (url != null) {
            url = url.substring(0, url.lastIndexOf(".git"));
        }
        return url;
    }

    public static CommitInfo getLastCommitInfo(String projectDir, String email) {
        if (projectDir == null) {
            return null;
        }
        if (email == null) {
            ResourceInfo resourceInfo = SoftwareCoUtils.getResourceInfo(projectDir);
            email = resourceInfo != null ? resourceInfo.getEmail() : null;
        }
        CommitInfo commitInfo = new CommitInfo();
        List<String> cmdList = new ArrayList<String>();
        cmdList.add("git");
        cmdList.add("log");
        cmdList.add("--pretty=%H,%s");
        if (email != null) {
            cmdList.add("--author=" + email);
        }
        cmdList.add("--max-count=1");

        // should only be a result of 1
        List<String> resultList = SoftwareCoUtils.getCommandResult(cmdList, projectDir);
        if (resultList != null && resultList.size() > 0) {
            String[] parts = resultList.get(0).split(",");
            if (parts != null && parts.length == 2) {
                commitInfo.setCommitId(parts[0]);
                commitInfo.setComment(parts[1]);
                commitInfo.setEmail(email);
            }
        }

        return commitInfo;
    }

}
