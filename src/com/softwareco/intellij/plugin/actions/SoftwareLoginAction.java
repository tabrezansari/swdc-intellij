/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.managers.FileManager;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import org.apache.commons.lang.StringUtils;

public class SoftwareLoginAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        SoftwareCoSessionManager.launchLogin("software", UIInteractionType.keyboard, false);
    }

    @Override
    public void update(AnActionEvent event) {
        String email = FileManager.getItem("name");
        boolean isLoggedIn = StringUtils.isNotBlank(email);
        // only show the login menu item if the server is online
        // and they're not logged on
        event.getPresentation().setVisible(!isLoggedIn);
        event.getPresentation().setEnabled(true);
    }
}
