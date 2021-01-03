package com.softwareco.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import org.jetbrains.annotations.NotNull;
import swdc.java.ops.manager.SlackManager;

public class SlackConnectAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        SlackManager.connectSlackWorkspace(() -> { CodeTimeToolWindow.rebuildTree();});
    }
}
