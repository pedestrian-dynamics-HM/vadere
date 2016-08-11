package org.vadere.gui.onlinevisualization.control;

import java.awt.event.MouseEvent;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.model.IDefaultModel;

public class OnlineVisSelectionMode extends DefaultSelectionMode {
	public OnlineVisSelectionMode(final IDefaultModel model) {
		super(model);
	}

	/**
	 * Just do nothing since we have no special cursor for the OnlineVisualization!
	 * 
	 * @param e
	 */
	@Override
	public void mouseMoved(final MouseEvent e) {}
}
