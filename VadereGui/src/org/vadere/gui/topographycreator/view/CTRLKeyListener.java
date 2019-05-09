package org.vadere.gui.topographycreator.view;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

public class CTRLKeyListener implements KeyListener {

    private final Consumer<Boolean> action;

    public CTRLKeyListener(Consumer<Boolean> action) {
        super();
        this.action = action;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F11) {
            action.accept(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getExtendedKeyCode() == InputEvent.CTRL_DOWN_MASK) {
            action.accept(false);
        }
    }
}
