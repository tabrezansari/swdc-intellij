package com.softwareco.intellij.plugin.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.softwareco.intellij.plugin.SoftwareCo;
import com.softwareco.intellij.plugin.wallclock.WallClockManager;
import org.jetbrains.annotations.NotNull;

public class CodeTimeToolWindowFactory implements ToolWindowFactory {
    private boolean initializedStatus = false;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeTimeToolWindow ctWindow = new CodeTimeToolWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(ctWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        if (!initializedStatus) {
            initializedStatus = true;
            ApplicationManager.getApplication().invokeLater(() -> {
                WallClockManager.dispatchStatusViewUpdate();
            });
        }
    }

    public static void openToolWindow() {
        Project project = SoftwareCo.getActiveProject();
        if (project != null) {
            ToolWindowManager.getInstance(project).getToolWindow("Code Time").show(null);
        }
    }
}
