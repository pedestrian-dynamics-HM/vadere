package org.vadere.gui.topographycreator.control;

import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.geometry.shapes.VCircle;

/**
 * In this mode VCircles will be generated.
 * 
 * 
 */
public class DrawDotMode extends DefaultSelectionMode {
	private static Resources resources = Resources.getInstance("topographycreator");

	private final UndoableEditSupport undoSupport;
	private final double dotRadius;
	private IDrawPanelModel panelModel;

	public DrawDotMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.panelModel = panelModel;
		this.undoSupport = undoSupport;
		this.dotRadius = VadereConfig.getConfig().getDouble( "TopographyCreator.dotRadius");
	}

	@Override
	public void mousePressed(MouseEvent event) {
		if (SwingUtilities.isRightMouseButton(event)) {
			super.mousePressed(event);
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (!SwingUtilities.isRightMouseButton(event)) {
			panelModel.setSelectionShape(new VCircle(panelModel.getMousePosition().x, panelModel.getMousePosition().y,
					this.dotRadius));
			new ActionAddElement("add action", panelModel, undoSupport).actionPerformed(null);

			panelModel.notifyObservers();
		} else {
			super.mouseReleased(event);
		}
	}

	@Override
	public IMode clone() {
		return new DrawDotMode(panelModel, undoSupport);
	}
}
