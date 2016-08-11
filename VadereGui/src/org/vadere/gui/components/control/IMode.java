package org.vadere.gui.components.control;

import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

public interface IMode extends MouseListener, MouseMotionListener, MouseWheelListener {
	Cursor getCursor();

	IMode clone();

	Color getSelectionColor();
}
