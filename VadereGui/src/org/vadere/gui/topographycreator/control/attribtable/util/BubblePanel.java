package org.vadere.gui.topographycreator.control.attribtable.util;

import javax.swing.*;
import java.awt.*;

public class BubblePanel extends JPanel implements BubblesRevalidate {
    private final BubblesRevalidate bubbleParent;

    public BubblePanel(BubblesRevalidate parent, GridBagLayout gridBagLayout) {
        super(gridBagLayout);
        this.bubbleParent = parent;
    }

    @Override
    public void bubbleRevalidate() {
        this.revalidate();
        this.repaint();
        if (bubbleParent != null) {
            bubbleParent.bubbleRevalidate();
        }
    }
}
