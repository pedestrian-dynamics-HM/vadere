package org.vadere.gui.topographycreator.control;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

/**
 * In this mode the user is able to remove ScenarioElements with his mouse.
 * 
 *
 */
public class EraserMode extends DefaultSelectionMode {
	private static Resources resources = Resources.getInstance("topographycreator");

	private Cursor cursor;
	private final UndoableEditSupport undoSupport;
	private IDrawPanelModel panelModel;

	public EraserMode(IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.panelModel = panelModel;
		this.undoSupport = undoSupport;
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		cursor = toolkit.createCustomCursor(toolkit.getImage(Resources.class.getResource("/icons/eraser_cursor.png")),
				new Point(0, 0), "eraser");
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		panelModel.setMousePosition(event.getPoint());
		ScenarioElement element = panelModel.removeElement(panelModel.getMousePosition());
		if (element != null) {
			UndoableEdit edit = new EditDeleteShape(panelModel, element);
			undoSupport.postEdit(edit);
		}
		panelModel.notifyObservers();
	}

	@Override
	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public IMode clone() {
		return new EraserMode(panelModel, undoSupport);
	}
}
