/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class SoftwareDashboardAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.keyboard);
    }

    @Override
    public void update(AnActionEvent event) {
        boolean sessionFileExists = SoftwareCoSessionManager.softwareSessionFileExists();
        boolean hasJwt = SoftwareCoSessionManager.jwtExists();
        boolean isLoggedIn = sessionFileExists && hasJwt && SoftwareCoUtils.isLoggedIn();
        event.getPresentation().setVisible(isLoggedIn);
        event.getPresentation().setEnabled(true);
    }
}
