package com.softwareco.intellij.plugin.tree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.softwareco.intellij.plugin.SoftwareCo;
import org.jetbrains.annotations.NotNull;

public class CodeTimeToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeTimeToolWindow ctWindow = new CodeTimeToolWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(ctWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static void openToolWindow() {
        Project project = SoftwareCo.getActiveProject();
        if (project != null) {
            ToolWindowManager.getInstance(project).getToolWindow("Code Time").show(null);
        }
    }
}
