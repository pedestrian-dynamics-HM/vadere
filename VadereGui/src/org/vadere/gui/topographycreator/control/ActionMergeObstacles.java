package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

/**
 * @author Benedikt Zoennchen
 */
public class ActionMergeObstacles extends TopographyAction {

	private final UndoableEditSupport undoSupport;

	public ActionMergeObstacles(String name, ImageIcon icon, IDrawPanelModel panelModel,
	                            UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		// soft copy of the list
		List<Obstacle> before = new ArrayList<>(getScenarioPanelModel().getObstacles());

		List<VPolygon> polygons = before.stream()
				.map(obstacle -> obstacle.getShape())
				.map(shape -> shape instanceof VRectangle ? new VPolygon(shape) : shape)
				.filter(shape -> shape instanceof VPolygon)
				.map(shape -> ((VPolygon)shape))
				.collect(Collectors.toList());

		WeilerAtherton weilerAtherton = new WeilerAtherton(polygons);
		List<VPolygon> mergedPolygons = weilerAtherton.cup();

		// remove polygon obstacles
		getScenarioPanelModel().removeObstacleIf(obstacle ->
				obstacle.getShape() instanceof VPolygon || obstacle.getShape() instanceof VRectangle);

		// add merged obstacles
		mergedPolygons
				.stream()
				.map(polygon -> new Obstacle(new AttributesObstacle(-1, polygon)))
				.forEach(obstacle -> getScenarioPanelModel().addShape(obstacle));


		List<Obstacle> after = new ArrayList<>(getScenarioPanelModel().getObstacles());

		UndoableEdit edit = new EditMergeObstacles(getScenarioPanelModel(), before, after);
		undoSupport.postEdit(edit);
		getScenarioPanelModel().notifyObservers();
	}
}
