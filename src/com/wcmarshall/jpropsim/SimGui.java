package com.wcmarshall.jpropsim;

import javax.swing.*;
import java.awt.*;
import com.intellij.uiDesigner.core.*;

/**
 * Created by wm on 5/29/16.
 */
public class SimGui extends JPanel {

    public SimGui() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Will Marshall
        tabbedPane1 = new JTabbedPane();
        JPanel hubTab = new JPanel();
        HubRamView = new JScrollPane();
        JPanel CogTab = new JPanel();
        JToolBar toolBar1 = new JToolBar();
        resetButton = new JButton();

        //======== this ========

        // JFormDesigner evaluation mark
        setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
                "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
                java.awt.Color.red), getBorder())); addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});

        setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));

        //======== tabbedPane1 ========
        {

            //======== hubTab ========
            {
                hubTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
                hubTab.add(HubRamView, new GridConstraints(0, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    null, null, null));
            }
            tabbedPane1.addTab("Hub", hubTab);

            //======== CogTab ========
            {
                CogTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
            }
            tabbedPane1.addTab("Cogs", CogTab);
        }
        add(tabbedPane1, new GridConstraints(1, 0, 1, 2,
            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            null, new Dimension(200, 200), null));

        //======== toolBar1 ========
        {

            //---- resetButton ----
            resetButton.setText("Reset");
            toolBar1.add(resetButton);
        }
        add(toolBar1, new GridConstraints(0, 0, 1, 2,
            GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_FIXED,
            null, null, null));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Will Marshall
    private JTabbedPane tabbedPane1;
    private JScrollPane HubRamView;
    private JButton resetButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
