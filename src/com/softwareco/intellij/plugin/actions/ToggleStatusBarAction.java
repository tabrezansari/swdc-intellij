package com.softwareco.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class ToggleStatusBarAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        SoftwareCoUtils.toggleStatusBar(UIInteractionType.keyboard);
    }

    @Override
    public void update(AnActionEvent event) {
        //
    }
}
