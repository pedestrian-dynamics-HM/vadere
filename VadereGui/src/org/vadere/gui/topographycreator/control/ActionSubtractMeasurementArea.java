package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class ActionSubtractMeasurementArea extends TopographyAction {

    private final UndoableEditSupport undoSupport;

    public ActionSubtractMeasurementArea(String name, String iconPath, IDrawPanelModel panelModel,
                                         UndoableEditSupport undoSupport) {
        super(name, iconPath, panelModel);
        this.undoSupport = undoSupport;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<Obstacle> obstacles = new ArrayList<>(getScenarioPanelModel().getObstacles());
        List<MeasurementArea> before = new ArrayList<>(getScenarioPanelModel().getMeasurementAreas());

        for (MeasurementArea measurementAreaOld : before) {

            List<VShape> intersectedObstecales = obstacles.stream()
                    .map(Obstacle::getShape)
                    .filter(s -> s.intersects(measurementAreaOld.getShape()))
                    .collect(Collectors.toList());

            if (intersectedObstecales.size() > 0 ){
                // remove  overlapping MeasurementArea
                getScenarioPanelModel().removeElement(measurementAreaOld);

                VShape shape = measurementAreaOld.getShape();
                List<VPoint> points = shape.getPath();

                Area thisArea = new Area(shape);

                // subtract portions of the measurement area covered by an obstacle.
                for (VShape intersectedObstecale : intersectedObstecales) {
                    Area otherArea = new Area(intersectedObstecale);
                    thisArea.subtract(otherArea);
                }

                // add new MeasurementArea
                MeasurementArea measurementAreaNew = new MeasurementArea();
                measurementAreaNew.setId(measurementAreaOld.getId());
                measurementAreaNew.setShape(getPolygonPoints(thisArea));
                getScenarioPanelModel().addShape(measurementAreaNew);
            }
        }
        List<MeasurementArea>  after = new ArrayList<>(getScenarioPanelModel().getMeasurementAreas());
        UndoableEdit edit = new EditSubtractMeasurementArea(getScenarioPanelModel(), before, after);
        undoSupport.postEdit(edit);
        getScenarioPanelModel().notifyObservers();
    }

    private VPolygon getPolygonPoints(Shape shape){

        List<VPoint> resultList = new ArrayList<>(); // use ArrayList for better index retrieval

        PathIterator iterator = shape.getPathIterator(null);
        Path2D.Double p = new Path2D.Double(shape);
        double[] coords = new double[6];
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            iterator.next();
            if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_MOVETO ) {
                resultList.add(new VPoint(coords[0], coords[1]));
            }
        }

        return GeometryUtils.polygonFromPoints2D(resultList);
    }
}
