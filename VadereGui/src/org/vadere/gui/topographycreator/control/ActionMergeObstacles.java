package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyElementFactory;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.WeilerAtherton;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

public class ActionMergeObstacles extends TopographyAction {

	private final UndoableEditSupport undoSupport;

	public ActionMergeObstacles(String name, ImageIcon icon, IDrawPanelModel panelModel,
	                            UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		List<Obstacle> obstacleList = getScenarioPanelModel().getTopography().getObstacles();
		List<VPolygon> polygons = obstacleList.stream()
				.map(obstacle -> obstacle.getShape())
				.map(shape -> shape instanceof VRectangle ? new VPolygon(shape) : shape)
				.filter(shape -> shape instanceof VPolygon)
				.map(shape -> ((VPolygon)shape))
				.collect(Collectors.toList());

		WeilerAtherton weilerAtherton = new WeilerAtherton(polygons);
		List<VPolygon> mergedPolygons = weilerAtherton.execute();

		// remove polygon obstacles
		getScenarioPanelModel().removeObstacleIf(obstacle ->
				obstacle.getShape() instanceof VPolygon || obstacle.getShape() instanceof VRectangle);

		// add merged obstacles
		mergedPolygons
				.stream()
				.map(polygon -> new Obstacle(new AttributesObstacle(-1, polygon)))
				.forEach(obstacle -> getScenarioPanelModel().addShape(obstacle));


		/*ScenarioElementType type = getScenarioPanelModel().getCurrentType();
		UndoableEdit edit = new EditDrawShape(getScenarioPanelModel(), type);
		undoSupport.postEdit(edit);

		IDrawPanelModel model = getScenarioPanelModel();

		model.getCurrentType();
		model.hideSelection();
		ScenarioElement element = TopographyElementFactory.getInstance().createScenarioShape(model.getCurrentType(),
				model.getSelectionShape());
		model.addShape(element);
		model.setSelectedElement(element);*/
		getScenarioPanelModel().notifyObservers();
	}
}
