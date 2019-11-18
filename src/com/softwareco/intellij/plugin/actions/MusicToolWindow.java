package com.softwareco.intellij.plugin.actions;

import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class MusicToolWindow {
    private JButton refreshToolWindowButton;
    private JButton hideToolWindowButton;
    private JLabel firstTrack;
    private JLabel secondTrack;
    private JLabel thirdTrack;
    private JPanel playlistWindowContent;
    private JTree tree1;

    public MusicToolWindow(ToolWindow toolWindow) {
        this.currentPlayList();
        tree1.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
            }
        });
    }

    public void currentPlayList() {
//        firstTrack.setText("Kal Ho Na Ho");
//        firstTrack.setIcon(new ImageIcon(getClass().getResource("/com/softwareco/intellij/plugin/assets/play.png")));
//
//        secondTrack.setText("Channa Mereya");
//        secondTrack.setIcon(new ImageIcon(getClass().getResource("/com/softwareco/intellij/plugin/assets/play.png")));
//
//        thirdTrack.setText("Nagada Sang Dhol");
//        thirdTrack.setIcon(new ImageIcon(getClass().getResource("/com/softwareco/intellij/plugin/assets/play.png")));
    }

    public JPanel getContent() {
        return playlistWindowContent;
    }
}
