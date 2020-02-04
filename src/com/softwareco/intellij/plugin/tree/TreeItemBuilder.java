package com.softwareco.intellij.plugin.tree;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBList;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TreeItemBuilder {

    public static JBList<JLabel> buildCodeTimeLabels() {
        DefaultListModel listModel = new DefaultListModel();

        // add the code time labels
        listModel.add(0, buildWebDashboardLabel());
        listModel.add(1, buildGenerateDashboardLabel());
        listModel.add(2, buildToggleStatusTextLabel());
        listModel.add(3, buildLearnMoreLabel());
        listModel.add(4, buildSubmitFeedbackLabel());

        JBList<JLabel> jbList = new JBList<>(listModel);
        jbList.setCellRenderer(new ListRenderer());
        jbList.setVisibleRowCount(1);
        jbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jbList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                JBList<JLabel> lst = (JBList<JLabel>) e.getSource();
                JLabel lbl = lst.getSelectedValue();
                if (lbl.getName().equals("webdashboard")) {
                    SoftwareCoSessionManager.launchWebDashboard();
                } else if (lbl.getName().equals("editordashboard")) {
                    SoftwareCoUtils.launchCodeTimeMetricsDashboard();
                } else if (lbl.getName().equals("submitfeedback")) {
                    SoftwareCoUtils.submitFeedback();
                } else if (lbl.getName().equals("learnmore")) {
                    // show the readme
                } else if (lbl.getName().equals("togglestatus")) {
                    SoftwareCoUtils.toggleStatusBar();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                JBList<JLabel> lst = (JBList<JLabel>) e.getSource();
                lst.clearSelection();
            }
        });
        jbList.updateUI();

        return jbList;
    }

    private static JLabel buildWebDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("See advanced metrics");
        label.setName("webdashboard");
        return label;
    }

    private static JLabel buildGenerateDashboardLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("Generate dashboard");
        label.setName("editordashboard");
        return label;
    }

    private static JLabel buildToggleStatusTextLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("Hide status bar metrics");
        label.setName("togglestatus");
        return label;
    }

    private static JLabel buildLearnMoreLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.findIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("Learn more");
        label.setName("learnmore");
        return label;
    }

    private static JLabel buildSubmitFeedbackLabel() {
        JLabel label = new JLabel();
        Icon icon = IconLoader.getIcon("/com/softwareco/intellij/plugin/assets/dashboard.png");
        label.setIcon(icon);
        label.setText("Submit feedback");
        label.setName("submitfeedback");
        return label;
    }

    public static JSeparator getSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentY(0.0f);
        separator.setForeground(new Color(58, 86, 187));
        return separator;
    }
}
