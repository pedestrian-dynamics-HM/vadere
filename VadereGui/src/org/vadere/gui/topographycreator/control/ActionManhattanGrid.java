package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.model.TopographyElementFactory;
import org.vadere.gui.topographycreator.view.ActionTranslateTopographyDialog;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class ActionManhattanGrid extends TopographyAction {

    private final UndoableEditSupport undoableEditSupport;

    public ActionManhattanGrid(String name, ImageIcon icon, IDrawPanelModel<?> panelModel,
                               UndoableEditSupport undoableEditSupport) {
        super(name, icon, panelModel);
        this.undoableEditSupport = undoableEditSupport;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActionTranslateTopographyDialog dialog = new ActionTranslateTopographyDialog(20.0, 5.0, "d, space");

        if (dialog.getValue()) {
            double d = dialog.getX();
            double space = dialog.getY();

            TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
            TopographyElementFactory f = TopographyElementFactory.getInstance();
            Rectangle2D.Double bound = model.getBounds();
            int numX = (int)Math.floor(bound.width/(d+space));
            double last_width = Math.min(d, bound.width - (numX-1)*(d +space));
            int numY = (int)Math.floor(bound.height/(d+space));
            double last_height = Math.min(d , bound.height - (numY-1)*(d +space));

            ArrayList<Obstacle> obs = new ArrayList<>();
            for(int x = 0; x < numX; x++){
                double x0 = x*(d+space);
                for(int y = 0; y < numY; y++){
                    double y0 = y*(d+space);
                    double dy = (y == numY-1) ? last_height : d;
                    double dx = (x == numX-1) ? last_width : d;

                    if(dy <= 0 || dx <= 0.0)
                        continue;

                    // ensure point is inside of bound
                    if ((dy+y0) > bound.height)
                        dy = bound.height - y0;

                    if ((dx+x0) > bound.width)
                        dx = bound.width - x0;

                    ScenarioElement element = f.createScenarioShape(
                            ScenarioElementType.OBSTACLE,
                            new VRectangle(x0, y0, dx, dy));
                    obs.add((Obstacle)element);
                    getScenarioPanelModel().addShape(element);
                }
            }

            // before: empty list. We didn't removed any obstacles.
            UndoableEdit edit = new EditMergeObstacles(getScenarioPanelModel(), new ArrayList<>(), obs);
            undoableEditSupport.postEdit(edit);
            getScenarioPanelModel().notifyObservers();
        }
    }
}
