package org.vadere.gui.topographycreator.control;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.view.ActionRandomPedestrianDialog;
import org.vadere.gui.topographycreator.view.ActionRandomPedestrianDialog.TARGET_OPTION;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.random.SimpleReachablePointProvider;

import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ActionPlaceRandomPedestrians extends TopographyAction {

	private static final Logger logger = Logger.getLogger(ActionPlaceRandomPedestrians.class);
	private static final int BINOMIAL_DISTRIBUTION_SUCCESS_VALUE = 1;

	private final double agentRadius;

	public ActionPlaceRandomPedestrians(String name, String icon, IDrawPanelModel panelModel,
										 UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		this.agentRadius = new AttributesAgent().getRadius();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionRandomPedestrianDialog dialog = new ActionRandomPedestrianDialog();

		if (dialog.showDialog() && dialog.isValid()) {
			Topography topography = getScenarioPanelModel().getTopography();

			Random random = dialog.getRandom();
			Rectangle2D.Double legalBound = dialog.getBoundaryRectangle();
			SimpleReachablePointProvider pointProvider = SimpleReachablePointProvider.uniform(random, legalBound, topography.getObstacleDistanceFunction());

			double groupMembershipRatio = dialog.getGroupMembershipRatio();
			BinomialDistribution binomialDistribution = new BinomialDistribution(BINOMIAL_DISTRIBUTION_SUCCESS_VALUE, groupMembershipRatio);

			int firstPedId = dialog.getFirstPedId();
			int numOfPeds = dialog.getNumOfPeds();
			int createdPeds = 0;

			for (int i = 0; i < numOfPeds; i++) {
				IPoint point = pointProvider.stream(dist ->  dist > 0.25).findFirst().get();
				VCircle newPosition = new VCircle(point.getX(), point.getY(), this.agentRadius);

				if (!checkOverlap(newPosition)) {
					int pedId = firstPedId + i;
					Pedestrian pedestrian = createPedestrian(dialog, topography, random, binomialDistribution, point, pedId);
					addPedestrianToTopography(pedestrian);
					createdPeds++;
				}
			}

			if (numOfPeds != createdPeds) {
				showWarning(numOfPeds, createdPeds);
			}
		} else {
			logger.warn("Dialog canceled or input invalid!");
		}

		getScenarioPanelModel().notifyObservers();
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

	@NotNull
	private Pedestrian createPedestrian(ActionRandomPedestrianDialog dialog, Topography topography, Random random, BinomialDistribution binomialDistribution, IPoint point, int id) {
		AttributesAgent attributesAgent = new AttributesAgent(
				topography.getAttributesPedestrian(),
				id);

		Pedestrian pedestrian = new Pedestrian(attributesAgent, random);
		pedestrian.setPosition(new VPoint(point));
		pedestrian.setTargets(getTargetList(dialog.getTargetOption(), dialog.getTargetList()));

		if (binomialDistribution.sample() == BINOMIAL_DISTRIBUTION_SUCCESS_VALUE) {
			pedestrian.setGroupMembership(GroupMembership.IN_GROUP);
		} else {
			pedestrian.setGroupMembership(GroupMembership.OUT_GROUP);
		}

		return pedestrian;
	}

	private LinkedList<Integer> getTargetList(TARGET_OPTION selectedOption, LinkedList<Integer> dialogList) {
		LinkedList<Integer> targetList = new LinkedList<>();

		if (selectedOption == TARGET_OPTION.EMPTY) {
			// Nothing to do here.
		}  else if (selectedOption == TARGET_OPTION.RANDOM) {
			List<Target> targets = getScenarioPanelModel().getTopography().getTargets();
			Random random = new Random();
			Target randomTarget = targets.get(random.nextInt(targets.size()));

			targetList.add(randomTarget.getId());

		} else if (selectedOption == TARGET_OPTION.USE_LIST) {
			targetList.addAll(dialogList);
		}

		return targetList;
	}

	private void addPedestrianToTopography(Pedestrian pedestrian) {
		AgentWrapper agentWrapper = new AgentWrapper(pedestrian);
		getScenarioPanelModel().addShape(agentWrapper);
		getScenarioPanelModel().setElementHasChanged(agentWrapper);
	}

	private void showWarning(int numOfPeds, int createdPeds) {
		String message = String.format("%s: %d\n%s: %d",
				Messages.getString("TopographyCreator.PlaceRandomPedestrians.couldNotPlaceAllPeds.requested.text"), numOfPeds,
				Messages.getString("TopographyCreator.PlaceRandomPedestrians.couldNotPlaceAllPeds.placed.text"), createdPeds);

		VDialogManager.showWarning(
				Messages.getString("TopographyCreator.PlaceRandomPedestrians.couldNotPlaceAllPeds.title"),
				message
		);
	}


}
