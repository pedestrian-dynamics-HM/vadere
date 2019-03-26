package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.undo.UndoableEditSupport;

public class DrawLineMode extends DefaultSelectionMode {

	enum DrawPathState {
		START, END, ADD
	}

	private final UndoableEditSupport undoSupport;
	private Path2D.Double path;
	private Line2D.Double line;
	private DrawPathState state = DrawPathState.START;
	private int lineCount = 0;
	private final IDrawPanelModel panelModel;
	private final List<VPoint> pointList;

	public DrawLineMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.panelModel = panelModel;
		this.undoSupport = undoSupport;
		this.pointList = new ArrayList<>();
	}


	@Override
	public void mouseDragged(MouseEvent event) {
		if (SwingUtilities.isRightMouseButton(event) && state == DrawPathState.START) {
			super.mouseDragged(event);
		} else {
			mouseMoved(event);
		}
	}

	@Override
	public void mousePressed(final MouseEvent event) {
		if (SwingUtilities.isRightMouseButton(event) && state == DrawPathState.START ) {
			super.mousePressed(event);
		}
	}

	@Override
	public void mouseReleased(final MouseEvent event) {
		if (SwingUtilities.isRightMouseButton(event) && state == DrawPathState.START) {
			super.mouseReleased(event);
		}
	}

	@Override
	public void mouseClicked(final MouseEvent event){
		if (!SwingUtilities.isRightMouseButton(event)) {
			if (SwingUtilities.isLeftMouseButton(event)) {
				if (event.getClickCount() <= 1 && state == DrawPathState.START) {
					panelModel.setMousePosition(event.getPoint());
					panelModel.setStartSelectionPoint(event.getPoint());
					path = new Path2D.Double(Path2D.WIND_NON_ZERO);
					path.moveTo(panelModel.getMousePosition().x, panelModel.getMousePosition().y);
					path.lineTo(panelModel.getMousePosition().x, panelModel.getMousePosition().y);
					line = new VLine(panelModel.getMousePosition().x, panelModel.getMousePosition().y,
							panelModel.getMousePosition().x, panelModel.getMousePosition().y);

					panelModel.setSelectionShape(new VPolygon(path));
					pointList.add(panelModel.getMousePosition());

					state = DrawPathState.END;
					panelModel.showSelection();
				} else if (event.getClickCount() <= 1 && state == DrawPathState.END) {
					path.lineTo(line.x2, line.y2);
					pointList.add(new VPoint(line.x2, line.y2));

					// dirty trick to see the first line!
					VPolygon poly = new VPolygon(path);
					poly.moveTo(line.x2, line.y2 + 0.0001 * panelModel.getScaleFactor());
					panelModel.setSelectionShape(poly);

					line = new Line2D.Double(panelModel.getMousePosition().x, panelModel.getMousePosition().y,
							panelModel.getMousePosition().x, panelModel.getMousePosition().y);
					panelModel.showSelection();
					lineCount++;
					path.closePath();
					panelModel.setSelectionShape(new VPolygon(path));
					new ActionAddElement("add element", panelModel, undoSupport).actionPerformed(null);
					pointList.forEach(p -> System.out.println(p));
					state = DrawPathState.START;
					lineCount = 0;
					pointList.clear();
					panelModel.hideSelection();
				}
				panelModel.notifyObservers();
			}
		} else {
			super.mouseClicked(event);
		}

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);

		if (state == DrawPathState.END) {
			VPoint cursorPosition = panelModel.getMousePosition();
			line.x2 = cursorPosition.x;
			line.y2 = cursorPosition.y;

			VPolygon poly = new VPolygon(path);
			poly.append(line, false);
			panelModel.setSelectionShape(poly);
		}
	}

	@Override
	public IMode clone() {
		return new DrawLineMode(panelModel, undoSupport);
	}
}
