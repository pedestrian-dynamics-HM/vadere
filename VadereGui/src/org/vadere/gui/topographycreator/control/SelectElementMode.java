package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * In this mode the user can select a ScenarioElement with his mouse (click) and he can move
 * elements around (press -> drag -> release).
 * 
 */
public class SelectElementMode extends DefaultSelectionMode {
	private boolean isModifying;
	private boolean persistentSelection;
	private boolean resizeElement;
	private static final int[] DIRECTIONAL_CURSOR_CODES;
	private final IDrawPanelModel panelModel;
	private final UndoableEditSupport undoSupport;


	static{
		DIRECTIONAL_CURSOR_CODES = new int[8];
		DIRECTIONAL_CURSOR_CODES[0] = Cursor.E_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[1] = Cursor.NE_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[2] = Cursor.N_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[3] = Cursor.NW_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[4] = Cursor.W_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[5] = Cursor.SW_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[6] = Cursor.S_RESIZE_CURSOR;
		DIRECTIONAL_CURSOR_CODES[7] = Cursor.SE_RESIZE_CURSOR;
	}

    public SelectElementMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.undoSupport = undoSupport;
		this.panelModel = panelModel;
		this.resizeElement = false;
		this.persistentSelection = false;
	}

	private Point startPoint;

	/*
	 * @Override
	 * public void mouseClicked(final MouseEvent event) {
	 * panelModel.setMousePosition(event.getPoint());
	 * ScenarioElement element = panelModel.setSelectedElement(panelModel.getMousePosition());
	 * 
	 * if(element != null)
	 * {
	 * //setJSONContent(element);
	 * }
	 * 
	 * panelModel.notifyObservers();
	 * }
	 */

	@Override
	public void mousePressed(final MouseEvent e) {
		if (isMouseOnSelectedElement()) {
			startPoint = e.getPoint();
			panelModel.getSelectedElements()
					.forEach(element  -> panelModel.addPrototypeShape(((ScenarioElement)element).getShape()));
			panelModel.showPrototypeShape();
			resizeElement = (Boolean)panelModel.getSelectedElements().stream()
					.map(element -> ((ScenarioElement)element).getShape().atBorder(panelModel.translateVectorCoordinates(startPoint)))
					.reduce(false, (first, second) -> (Boolean)first || (Boolean)second);
			isModifying = true;
		} else {
			super.mousePressed(e);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		panelModel.setMouseSelectionMode(this);
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (isMouseOnPrototypeShape()) {
			panelModel.getPrototypeShapes().clear();
			for(Object element : panelModel.getSelectedElements()) {
				VShape shape =
						panelModel.translate((ScenarioElement) element, new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));
				panelModel.addPrototypeShape(shape);
				panelModel.showPrototypeShape();
			}
		} else if(!persistentSelection) {
			panelModel.hidePrototypeShape();
		}
		super.mouseDragged(e);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		if (isMouseOnPrototypeShape()) {
			for (Object object : panelModel.getSelectedElements()) {
				ScenarioElement element = (ScenarioElement) object;
				VShape oldShape = element.getShape();
				VShape newShape =
						panelModel.translate(element, new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));
				AttributeModifier.setShapeToAttributes(element, newShape);
				UndoableEdit edit = new EditUpdateElementShape(panelModel, element, oldShape);
				undoSupport.postEdit(edit);
			}
		} else {
			super.mouseReleased(e);
		}
		if(!persistentSelection) {
            resizeElement = false;
            isModifying = false;
            startPoint = null;
            panelModel.getPrototypeShapes().clear();
            panelModel.hidePrototypeShape();
        }
		panelModel.notifyObservers();
	}

	private boolean isMouseOnSelectedElement() {
		for(Object select : panelModel.getSelectedElements()) {
			ScenarioElement element = (ScenarioElement) select;
			VPoint cursor = panelModel.getMousePosition();
			if(element.getShape().intersects(cursor.x, cursor.y, 0.001, 0.001)){
				return true;
			}
		}
		return false;
	}

	private boolean isMouseOnPrototypeShape() {
		if (panelModel.arePrototypesVisible()) {
			VPoint cursor = panelModel.getMousePosition();
			return (Boolean)panelModel.getPrototypeShapes().stream()
					.map(prototype -> ((VShape)prototype).intersects(cursor.x, cursor.y, 0.001, 0.001))
					.reduce(false, (first, second) -> (Boolean)first || (Boolean)second);
		}
		return false;
	}

	public void setPersistentSelection(boolean persistentSelection) {
		this.persistentSelection = persistentSelection;
	}
	@Override
	public IMode clone() {
		return new SelectElementMode(panelModel, undoSupport);
	}

}
