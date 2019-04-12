package org.vadere.gui.topographycreator.control;

import org.lwjgl.system.CallbackI;
import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Optional;

/**
 * In this mode the user can select a ScenarioElement with his mouse (click) and he can move
 * elements around (press -> drag -> release).
 * 
 */
public class SelectElementMode extends DefaultSelectionMode {
	private final UndoableEditSupport undoSupport;
	private final IDrawPanelModel panelModel;
	private boolean resizeElement;
	private boolean isModifying;
	private static final int[] DIRECTIONAL_CURSOR_CODES;

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
			panelModel.setPrototypeShape(panelModel.getSelectedElement().getShape());
			panelModel.showPrototypeShape();
			resizeElement = panelModel.getSelectedElement().getShape().atBorder(panelModel.translateVectorCoordinates(startPoint));
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
		if (isMouseOnPrototypeShape() || isModifying) {
			//VShape shape = panelModel.translate(new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));
			VShape shape = resizeElement ?
					panelModel.resize(startPoint, e.getPoint()) :
					panelModel.translate(new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));
			panelModel.setPrototypeShape(shape);
			panelModel.showPrototypeShape();
		} else {
			panelModel.hidePrototypeShape();
		}
		super.mouseDragged(e);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		ScenarioElement element = panelModel.getSelectedElement();
		if (isMouseOnPrototypeShape() || isModifying) {
			VShape oldShape = element.getShape();
			VShape newShape = resizeElement ?
					panelModel.resize(startPoint, e.getPoint()) :
					panelModel.translate(new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));

			AttributeModifier.setShapeToAttributes(element, newShape);

			// tell the panelModel that the selected element has changed!
			panelModel.setSelectedElement(element);
			element.getId();

			UndoableEdit edit = new EditUpdateElementShape(panelModel, element, oldShape);
			undoSupport.postEdit(edit);
		} else {
			super.mouseReleased(e);
		}
		resizeElement = false;
		isModifying = false;
		startPoint = null;
		panelModel.hidePrototypeShape();
		panelModel.notifyObservers();
	}

	private boolean isMouseOnSelectedElement() {
		ScenarioElement element = panelModel.getSelectedElement();
		VPoint cursor = panelModel.getMousePosition();
		return element != null && element.getShape().intersects(cursor.x - 0.01, cursor.y - 0.01, 0.02, 0.02);
		//return element != null && element.getShape().contains(cursor);
	}

	private boolean isMouseOnPrototypeShape() {
		VShape shape = panelModel.getPrototypeShape();
		VPoint cursor = panelModel.getMousePosition();
		return panelModel.isPrototypeVisble() && shape.intersects(cursor.x - 0.01, cursor.y - 0.01, 0.02, 0.02);
	}

	@Override
	public Cursor getCursor(){
	    VShape selectedShape = panelModel.getSelectedElement() == null ? null : panelModel.getSelectedElement().getShape();
	    VPoint mousePosition = panelModel.getMousePosition();
	    boolean directionalCursorCondition = isMouseOnSelectedElement() && (resizeElement || selectedShape.atBorder(mousePosition));
		if (directionalCursorCondition) {
			return Cursor.getPredefinedCursor(
					DIRECTIONAL_CURSOR_CODES[
							selectedShape.getDirectionalCode(
									startPoint == null ? mousePosition : new VPoint(startPoint),
									DIRECTIONAL_CURSOR_CODES.length
							)]
			);
		}
		return super.getCursor();
	}

	@Override
	public IMode clone() {
		return new SelectElementMode(panelModel, undoSupport);
	}
}
