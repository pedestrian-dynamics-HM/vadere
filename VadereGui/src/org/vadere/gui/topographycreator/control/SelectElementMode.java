package org.vadere.gui.topographycreator.control;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * In this mode the user can select a ScenarioElement with his mouse (click) and he can move
 * elements around (press -> drag -> release).
 * 
 */
public class SelectElementMode extends DefaultSelectionMode {
	private final UndoableEditSupport undoSupport;
	private final IDrawPanelModel panelModel;

	public SelectElementMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.undoSupport = undoSupport;
		this.panelModel = panelModel;
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
		} else {
			super.mousePressed(e);
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (isMouseOnPrototypeShape()) {
			for(Object element : panelModel.getSelectedElements()) {
				VShape shape =
						panelModel.translate((ScenarioElement) element, new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));
				panelModel.addPrototypeShape(shape);
				panelModel.showPrototypeShape();
			}
		} else {
			panelModel.hidePrototypeShape();
		}
		super.mouseDragged(e);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		if (isMouseOnPrototypeShape()) {
			for(Object object : panelModel.getSelectedElements()) {
				ScenarioElement element = (ScenarioElement)object;
				VShape oldShape = element.getShape();
				VShape newShape =
						panelModel.translate(element, new Point(e.getPoint().x - startPoint.x, e.getPoint().y - startPoint.y));

				AttributeModifier.setShapeToAttributes(element, newShape);

				// tell the panelModel that the selected element has changed!
				panelModel.addSelectedElements(element);
				UndoableEdit edit = new EditUpdateElementShape(panelModel, element, oldShape);
				undoSupport.postEdit(edit);
			}
		} else {
			super.mouseReleased(e);
		}
		panelModel.hidePrototypeShape();
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

	@Override
	public IMode clone() {
		return new SelectElementMode(panelModel, undoSupport);
	}
}
