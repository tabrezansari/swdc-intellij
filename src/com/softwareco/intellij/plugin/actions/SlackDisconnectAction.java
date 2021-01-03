package com.softwareco.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import org.jetbrains.annotations.NotNull;
import swdc.java.ops.manager.SlackManager;

public class SlackDisconnectAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        SlackManager.disconnectSlackWorkspace(() -> { CodeTimeToolWindow.rebuildTree();});
    }

    @Override
    public void update(AnActionEvent event) {
        boolean showMenuItem = SlackManager.hasSlackWorkspaces();
        event.getPresentation().setVisible(showMenuItem);
    }
}
