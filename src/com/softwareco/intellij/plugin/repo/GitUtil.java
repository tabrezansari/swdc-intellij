package com.softwareco.intellij.plugin.repo;


import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.models.CommitChangeStats;
import com.softwareco.intellij.plugin.models.RepoInfo;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GitUtil {

    public static CommitChangeStats accumulateStatChanges(List<String> results, boolean committedChanges) {
        CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

        if (results != null) {
            for (String line : results) {
                line = line.trim();
                if (line.indexOf("insertion") != -1 && line.indexOf("deletion") != -1) {
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

        RepoInfo repoInfo = getResourceInfo(projectDir);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("git");
        cmdList.add("log");
        cmdList.add("--stat");
        cmdList.add("--pretty=COMMIT:%H,%ct,%cI,%s");
        cmdList.add("--since=" + starOfDay);
        if (repoInfo != null && repoInfo.getEmail() != null && !repoInfo.getEmail().isEmpty()) {
            cmdList.add("--author=" + repoInfo.getEmail());
        }

        return getChangeStats(cmdList, projectDir, true);
    };

    public static RepoInfo getResourceInfo(String projectDir) {
        RepoInfo repoInfo = new RepoInfo();

        if (projectDir == null) {
            return repoInfo;
        }

        try {
            String[] branchCmd = { "git", "symbolic-ref", "--short", "HEAD" };
            String branch = SoftwareCoUtils.runCommand(branchCmd, projectDir);

            String[] identifierCmd = { "git", "config", "--get", "remote.origin.url" };
            String identifier = SoftwareCoUtils.runCommand(identifierCmd, projectDir);

            String[] emailCmd = { "git", "config", "user.email" };
            String email = SoftwareCoUtils.runCommand(emailCmd, projectDir);

            String[] tagCmd = { "git", "describe", "--all" };
            String tag = SoftwareCoUtils.runCommand(tagCmd, projectDir);

            if (StringUtils.isNotBlank(branch) && StringUtils.isNotBlank(identifier)) {
                repoInfo.setBranch(branch);
                repoInfo.setIdentifier(identifier);
                repoInfo.setEmail(email);
                repoInfo.setTag(tag);
            }
        } catch (Exception e) {
            //
        }
        return repoInfo;
    };
}
