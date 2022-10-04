package org.vadere.gui.topographycreator.control;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.migration.GeometryCleaner;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class ActionMergeObstacles extends TopographyAction {

	private final UndoableEditSupport undoSupport;
	private static final Logger logger = Logger.getLogger(ActionMergeObstacles.class);

	public ActionMergeObstacles(String name, String icon,String shortDescription, IDrawPanelModel panelModel,
	                            UndoableEditSupport undoSupport) {
		super(name, icon, shortDescription, panelModel);
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

		GeometryCleaner topographyCleaner = new GeometryCleaner(
				new VRectangle(getScenarioPanelModel().getTopographyBound()), polygons, 0.01);

		logger.info("start merging, this can require some time.");
		Pair<VPolygon, List<VPolygon>> mergedPolygons = topographyCleaner.clean();
		//polygons = WeilerAtherton.magnet(polygons, new VRectangle(getScenarioPanelModel().getBounds()));
		logger.info("merging process finisehd.");



		/*WeilerAtherton weilerAtherton = new WeilerAtherton(polygons);
		List<VPolygon> mergedPolygons = weilerAtherton.cup();*/

		// remove polygon obstacles
		getScenarioPanelModel().removeObstacleIf(obstacle ->
				obstacle.getShape() instanceof VPolygon || obstacle.getShape() instanceof VRectangle);



		// add merged obstacles
		mergedPolygons.getRight()
				.stream()
				.map(polygon -> new Obstacle(new AttributesObstacle(-1, polygon)))
				.forEach(obstacle -> getScenarioPanelModel().addShape(obstacle));

		//WeilerAtherton weilerAtherton = new WeilerAtherton(Arrays.asList(mergedPolygons.getLeft(), new VPolygon(getScenarioPanelModel().getBounds())));
		//weilerAtherton.cap().get();

		//getScenarioPanelModel().addShape(new Obstacle(new AttributesObstacle(-1, weilerAtherton.cap().get())));

		List<Obstacle> after = new ArrayList<>(getScenarioPanelModel().getObstacles());

		UndoableEdit edit = new EditMergeObstacles(getScenarioPanelModel(), before, after);
		undoSupport.postEdit(edit);
		getScenarioPanelModel().notifyObservers();
	}
}
