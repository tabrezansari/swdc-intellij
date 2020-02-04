package com.softwareco.intellij.plugin.tree;


import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.softwareco.intellij.plugin.SoftwareCoUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Logger;

public class CodeTimeToolWindow {

    public static final Logger LOG = Logger.getLogger("CodeTimeToolWindow");
    private JPanel codetimeWindowContent;
    private JScrollPane scrollPane;
    private JPanel dataPanel;

    public CodeTimeToolWindow(ToolWindow toolWindow) {
        codetimeWindowContent.setFocusable(true);

        this.rebuildTreeView();

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        codetimeWindowContent.setBackground((Color) null);
    }

    public synchronized void rebuildTreeView() {
        Component component = dataPanel.getComponent(dataPanel.getComponentCount() - 1);

        dataPanel.removeAll();
        dataPanel.setBackground((Color) null);
        dataPanel.setFocusable(true);

        JBList<JLabel> labels = TreeItemBuilder.buildCodeTimeLabels();

        // add the top labels
        dataPanel.add(labels, gridConstraints(dataPanel.getComponentCount(), 1, 2, 0, 3, 0));

        JSeparator separator = TreeItemBuilder.getSeparator();
        // add a separator
        dataPanel.add(separator, gridConstraints(dataPanel.getComponentCount(), 1, 6, 0, 1, 0));


        dataPanel.updateUI();
        dataPanel.setVisible(true);

        scrollPane.setFocusable(true);
        scrollPane.setVisible(true);

        codetimeWindowContent.updateUI();
        codetimeWindowContent.setFocusable(true);
        codetimeWindowContent.setVisible(true);
    }

    private GridConstraints gridConstraints(int row, int vSize, int hSize, int anchor, int fill, int indent) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(0);
        constraints.setRowSpan(1);
        constraints.setColSpan(1);
        constraints.setVSizePolicy(vSize);
        constraints.setHSizePolicy(hSize);
        constraints.setAnchor(anchor);
        constraints.setFill(fill);
        constraints.setIndent(indent);
        constraints.setUseParentLayout(false);

        return constraints;
    }

    public JPanel getContent() {
        return codetimeWindowContent;
    }

}
