package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class CodeTimeToolWindow {

    public static final Logger LOG = Logger.getLogger("CodeTimeToolWindow");
    private JPanel codetimeWindowContent;
    private JScrollPane scrollPane;
    private JPanel dataPanel;

    public CodeTimeToolWindow(ToolWindow toolWindow) {

        dataPanel.removeAll();
        dataPanel.setBackground((Color) null);
        dataPanel.updateUI();
        dataPanel.setFocusable(true);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.updateUI();
        scrollPane.setVisible(true);

        codetimeWindowContent.setBackground((Color) null);
        codetimeWindowContent.updateUI();
        codetimeWindowContent.setFocusable(true);
        codetimeWindowContent.setVisible(true);
    }

    public JPanel getContent() {
        return codetimeWindowContent;
    }

}
