package org.vadere.gui.topographycreator.control.attribtable.util;

import java.awt.*;

public final class Layouts {
    public static GridBagConstraints initGridBagConstraint(double weighty) {
        var gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = weighty;
        return gbc;
    }
}
