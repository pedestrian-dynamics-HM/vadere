package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
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

/**
 * In this mode VPolygons will be generated.
 *
 *
 */
public class DrawSimplePolygonMode extends DefaultSelectionMode {

	enum DrawPathState {
		START, ADD
	}

	private final UndoableEditSupport undoSupport;
	private Path2D.Double path;
	private Line2D.Double line;
	private DrawPathState state = DrawPathState.START;
	private int lineCount = 0;
	private final IDrawPanelModel panelModel;
	private final List<VPoint> pointList;

	public DrawSimplePolygonMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.panelModel = panelModel;
		this.undoSupport = undoSupport;
		this.pointList = new ArrayList<>();
		// panelModel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
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
		if (SwingUtilities.isRightMouseButton(event) && state == DrawPathState.START) {
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
	public void mouseClicked(final MouseEvent event) {
		if (!SwingUtilities.isRightMouseButton(event)) {
			if (SwingUtilities.isLeftMouseButton(event)) {
				if (event.getClickCount() <= 1 && state == DrawPathState.START) { // add the first point
					panelModel.setMousePosition(event.getPoint());
					panelModel.setStartSelectionPoint(event.getPoint());
					path = new Path2D.Double(Path2D.WIND_NON_ZERO);
					path.moveTo(panelModel.getMousePosition().x, panelModel.getMousePosition().y);
					path.lineTo(panelModel.getMousePosition().x, panelModel.getMousePosition().y);
					line = new VLine(panelModel.getMousePosition().x, panelModel.getMousePosition().y,
							panelModel.getMousePosition().x, panelModel.getMousePosition().y);

					/*
					 * panelModel.addShape(ScenarioShapeFactory.getInstance().createScenarioShape(
					 * panelModel.getCurrentType(), new VPolygon(path)));
					 */
					panelModel.setSelectionShape(new VPolygon(path));
					pointList.add(panelModel.getMousePosition());

					state = DrawPathState.ADD;
					panelModel.showSelection();
				} else if (event.getClickCount() <= 1 && state == DrawPathState.ADD) { // add a new line
					if(lineCount <= 1 || isValidNextPoint(new VPoint(line.x2, line.y2)))	{
						path.lineTo(line.x2, line.y2);
						pointList.add(new VPoint(line.x2, line.y2));
						if (lineCount <= 1) {
							// dirty trick to see the first line!
							VPolygon poly = new VPolygon(path);
							poly.moveTo(line.x2, line.y2 + 0.0001 * panelModel.getScaleFactor());
							panelModel.setSelectionShape(poly);
						} else {
							panelModel.setSelectionShape(new VPolygon(path));
						}

						line = new Line2D.Double(panelModel.getMousePosition().x, panelModel.getMousePosition().y,
								panelModel.getMousePosition().x, panelModel.getMousePosition().y);
						panelModel.showSelection();
						lineCount++;
					}
				} else { // finish the draw
					panelModel.hideSelection();
					if (lineCount <= 1) {
						// panelModel.deleteLastShape();
					} else {
						path.closePath();
						VPolygon polygon = new VPolygon(path);
						panelModel.setSelectionShape(polygon);

						if (isSimplePolygon(pointList)) {
							new ActionAddElement("add element", panelModel, undoSupport).actionPerformed(null);
						}

						panelModel.notifyObservers();
					}
					state = DrawPathState.START;
					lineCount = 0;
					pointList.clear();
				}
			} else {
				if (state == DrawPathState.ADD) {
					panelModel.hideSelection();
					state = DrawPathState.START;
					lineCount = 0;
				}
			}

			panelModel.notifyObservers();
		}
	}

	// this can be done more efficiently by: http://geomalgorithms.com/a09-_intersect-3.html#simple_Polygon()
	private boolean isSimplePolygon(final List<VPoint> pointList) {
		for(int i = 0; i < pointList.size(); i++) {
			VPoint p1 = pointList.get(i % pointList.size());
			VPoint p2 = pointList.get((i+1) % pointList.size());

			VPoint pref = pointList.get((i + pointList.size() - 1) % pointList.size());
			VPoint next = pointList.get((i+2) % pointList.size());

			if(pref.equals(p1) || next.equals(p2)) {
				return false;
			}

			VLine line = new VLine(p1, p2);
			for(int j = 0; j < pointList.size()-3; j++) {
				VPoint q1 = pointList.get((i+2+j) % pointList.size());
				VPoint q2 = pointList.get((i+2+j+1) % pointList.size());

				if(line.intersectsLine(new VLine(q1, q2))) {
					return false;
				}
			}
		}
		return true;
	}

	// this can be done more efficiently by: http://geomalgorithms.com/a09-_intersect-3.html#simple_Polygon()
	private boolean isValidNextPoint(final VPoint point) {
		if(pointList.size() == 1) {
			return !point.equals(pointList.get(pointList.size()-1));
		}
		else if (pointList.size() >= 2) {
			VLine line = new VLine(pointList.get(pointList.size()-1), point);

			if(point.equals(pointList.get(pointList.size()-1)) || point.equals(pointList.get(pointList.size()-2))) {
				return false;
			}

			for(int i = 0; i < pointList.size()-2; i++) {
				VPoint p1 = pointList.get(i);
				VPoint p2 = pointList.get(i+1);

				if(line.intersectsLine(new VLine(p1, p2))) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);

		if (state == DrawPathState.ADD) {
			VPoint cursorPosition = panelModel.getMousePosition();
			line.x2 = cursorPosition.x;
			line.y2 = cursorPosition.y;

			/*if (pointList.size() >= 2) {
				List<VPoint> cloneList = new ArrayList<>(pointList);
				cloneList.add(new VPoint(line.x2, line.y2));
				GrahamScan scan = new GrahamScan(cloneList);
				panelModel.setSelectionShape(scan.getPolytope());
			} else {*/
				VPolygon poly = new VPolygon(path);
				poly.append(line, false);
				// poly.lineTo(line.x2, line.y2);
				panelModel.setSelectionShape(poly);
			//}
		}
	}

	@Override
	public IMode clone() {
		return new DrawSimplePolygonMode(panelModel, undoSupport);
	}
}
