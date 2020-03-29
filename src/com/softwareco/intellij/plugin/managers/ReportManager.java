package com.softwareco.intellij.plugin.managers;

import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;

public class ReportManager {

    public static String getProjectContributorSummaryFile() {
        String file = SoftwareCoSessionManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\ProjectContributorCodeSummary.txt";
        } else {
            file += "/ProjectContributorCodeSummary.txgt";
        }
        return file;
    }

    public static void displayProjectContributorSummaryDashboard(String identifier) {
        //
    }
}
