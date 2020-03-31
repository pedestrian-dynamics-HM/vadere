package org.vadere.gui.topographycreator.control;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyElementFactory;
import org.vadere.gui.topographycreator.view.ActionRandomPedestrianDialog;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.random.SimpleReachablePointProvider;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Random;
import java.util.function.Supplier;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

public class ActionPlaceRandomPedestrians extends TopographyAction {

	private final UndoableEditSupport undoSupport;
	private final double dotRadius;

	public ActionPlaceRandomPedestrians(String name, ImageIcon icon, IDrawPanelModel panelModel,
										 UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		this.undoSupport = undoSupport;
		this.dotRadius = new AttributesAgent().getRadius();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionRandomPedestrianDialog dialog = new ActionRandomPedestrianDialog();

		if (dialog.showDialog() && dialog.isValid()) {
			IDrawPanelModel model = getScenarioPanelModel();

			int numOfPeds = dialog.getNumOfPeds();
			Supplier<LinkedList<Integer>> targetSupplier = getTargetSupplier(dialog);
			Random random = dialog.getRandom();

			Rectangle2D.Double legalBound = dialog.getBoundaryRectangle();
			SimpleReachablePointProvider provider = SimpleReachablePointProvider.uniform(random, legalBound, model.getTopography().getObstacleDistanceFunction());

			int placedPedestrians = 0;
			while (placedPedestrians < numOfPeds) {

				IPoint p = provider.stream(dist ->  dist > 0.25).findFirst().get();
				getScenarioPanelModel().setSelectionShape(new VCircle(p.getX(), p.getY(), this.dotRadius));

				if (!checkOverlap(model.getSelectionShape())){
					// no overlap with existing pedestrians found. --> use this point

					ScenarioElementType type = getScenarioPanelModel().getCurrentType();
					UndoableEdit edit = new EditDrawShape(getScenarioPanelModel(), type);
					undoSupport.postEdit(edit);

					model.getCurrentType();
					model.hideSelection();
					AgentWrapper element = (AgentWrapper)TopographyElementFactory.getInstance().createScenarioShape(model.getCurrentType(), model.getSelectionShape());
					element.getAgentInitialStore().setTargets(targetSupplier.get());
					model.addShape(element);
					model.setSelectedElement(element);

					placedPedestrians++;
				}
			}
		}

		new ActionSelectSelectShape("select shape mode", getScenarioPanelModel(), undoSupport).actionPerformed(null);
		getScenarioPanelModel().notifyObservers();
	}

	// if no target is given via the dialog select a single random target existing in the topography.
	private Supplier<LinkedList<Integer>> getTargetSupplier(ActionRandomPedestrianDialog dialog){
		if (dialog.useRandomTargets()){
			Integer[] targets = getScenarioPanelModel().getTopography().getTargets()
					.stream()
					.map(Target::getId)
					.toArray(Integer[]::new);
			return new Supplier<LinkedList<Integer>>() {

				UniformIntegerDistribution dist =
						new UniformIntegerDistribution(new JDKRandomGenerator(dialog.getRandom().nextInt()), 0, targets.length -1);
				@Override
				public LinkedList<Integer> get() {
					LinkedList<Integer> ret = new LinkedList<>();
					ret.add(targets[dist.sample()]);
					return ret;
				}
			};
		} else {
			return dialog::getSelectedTargets;
		}
	}

	private boolean checkOverlap(VShape newPedestrian){
		boolean pedOverlap = getScenarioPanelModel().getTopography().getInitialElements(Pedestrian.class)
				.stream()
				.map(Agent::getShape)
				.anyMatch(shape -> shape.intersects(newPedestrian));
		boolean targetOverlap = getScenarioPanelModel().getTopography().getTargets()
				.stream()
				.map(Target::getShape)
				.anyMatch(shape -> shape.intersects(newPedestrian));

		return pedOverlap  || targetOverlap;
	}
}
