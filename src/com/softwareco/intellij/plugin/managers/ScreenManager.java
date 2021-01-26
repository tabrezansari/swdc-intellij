package com.softwareco.intellij.plugin.managers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.IdeRootPane;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.tree.CodeTimeToolWindow;
import swdc.java.ops.manager.AsyncManager;

import javax.swing.*;

public class ScreenManager {

    private static IdeFrameImpl getIdeWindow() {
        // Retrieve the AWT window
        Project p = SoftwareCoUtils.getOpenProject();
        if (p == null) {
            return null;
        }

        IdeRootPane rootPane = (IdeRootPane) WindowManager.getInstance().getFrame(p).getRootPane();
        IdeFrameImpl ideWindow = null;
        try{
            ideWindow = (IdeFrameImpl) rootPane.getParent();
        } catch (Exception e) {}

        return ideWindow;
    }

    public static boolean isFullScreen() {
        IdeFrameImpl win = getIdeWindow();
        if (win != null) {
            return (win.getExtendedState() == JFrame.MAXIMIZED_BOTH);
        }
        return false;
    }

    public static boolean enterFullScreen() {
        IdeFrameImpl win = getIdeWindow();
        if (win == null) {
            return false;
        }
        int extendedState = win.getExtendedState();
        if (extendedState != JFrame.MAXIMIZED_BOTH) {
            SwingUtilities.invokeLater(() -> {
                win.setExtendedState(JFrame.NORMAL);
                win.setBounds(win.getGraphicsConfiguration().getBounds());
                win.setVisible(true);

                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> {CodeTimeToolWindow.refresh();}, 1);
            });
            return true;
        }
        return false;
    }

    public static boolean exitFullScreen() {
        IdeFrameImpl win = getIdeWindow();
        if (win == null) {
            return false;
        }
        int extendedState = win.getExtendedState();
        if (extendedState == JFrame.MAXIMIZED_BOTH) {
            SwingUtilities.invokeLater(() -> {
                win.setExtendedState(JFrame.MAXIMIZED_BOTH);
                win.setBounds(win.getGraphicsConfiguration().getBounds());
                win.setVisible(true);

                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> {CodeTimeToolWindow.refresh();}, 1);
            });
            return true;
        }
        return false;
    }

    public static void toggleFullScreenMode() {
        IdeFrameImpl win = getIdeWindow();
        if (win == null) {
            return;
        }

        int extendedState = win.getExtendedState();
        if (extendedState == JFrame.MAXIMIZED_BOTH) {
            SwingUtilities.invokeLater(() -> {
                win.setExtendedState(JFrame.NORMAL);
                win.setBounds(win.getGraphicsConfiguration().getBounds());
                win.setVisible(true);

                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> {CodeTimeToolWindow.refresh();}, 1);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                win.setExtendedState(JFrame.MAXIMIZED_BOTH);
                win.setBounds(win.getGraphicsConfiguration().getBounds());
                win.setVisible(true);

                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> {CodeTimeToolWindow.refresh();}, 1);
            });
        }
    }
}
