package org.vadere.gui.postvisualization.model;

import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.postvisualization.utils.PotentialFieldContainer;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TopographyIterator;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.parser.JsonLogicParser;
import org.vadere.util.io.parser.VPredicate;
import org.vadere.util.potential.CellGrid;

import com.fasterxml.jackson.databind.JsonNode;

public class PostvisualizationModel extends SimulationModel<PostvisualizationConfig> {

	private static Logger logger = LogManager.getLogger(PostvisualizationModel.class);

	private Step step;

	private int topographyId;

	private Scenario vadere;

	private PotentialFieldContainer potentialContainer;

	private final PedestrianColorTableModel pedestrianColorTableModel;

	private final Map<Integer, VPredicate<JsonNode>> colorEvalFunctions;

	private Map<Integer, Trajectory> trajectories;

	private Map<Step, List<Agent>> agentsByStep;

	private Comparator<Step> stepComparator = (s1, s2) -> s1.getStepNumber() - s2.getStepNumber();

	private List<Step> steps;

	private String outputPath;

	// public Configuration config;

	public PostvisualizationModel() {
		super(new PostvisualizationConfig());
		this.trajectories = new HashMap<>();
		this.agentsByStep = new HashMap<>();
		this.vadere = new Scenario("");
		this.topographyId = 0;
		this.colorEvalFunctions = new HashMap<>();
		this.potentialContainer = null;
		this.pedestrianColorTableModel = new PedestrianColorTableModel();
		this.steps = new ArrayList<>();

		for (int i = 0; i < 5; i++) {
			try {
				colorEvalFunctions.put(i, new JsonLogicParser("false").parse());
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		this.pedestrianColorTableModel.addTableModelListener(
				e -> {
					for (int row = e.getFirstRow(); row <= e.getLastRow(); row++) {
						if (row >= 0 && colorEvalFunctions.containsKey(row)
								&& e.getColumn() == PedestrianColorTableModel.CIRTERIA_COLUMN) {
							try {
								VPredicate<JsonNode> evaluator = new JsonLogicParser(
										pedestrianColorTableModel.getValueAt(row, e.getColumn()).toString()).parse();
								colorEvalFunctions.put(row, evaluator);
							} catch (ParseException e1) {
								colorEvalFunctions.remove(row);
								logger.warn(e1.getLocalizedMessage());
							}
						}
					}
				});
	}

	public void init(final Map<Step, List<Agent>> agentsByStep, final Scenario vadere, final String projectPath) {
		logger.info("start init postvis model");
		init(vadere, projectPath);
		this.agentsByStep = agentsByStep;
		Map<Integer, Step> map = agentsByStep
				.keySet().stream()
				.sorted(stepComparator)
				.collect(Collectors.toMap(s -> s.getStepNumber(), s -> s));


		// fill in missing steps by taking the pedestrian of the nearest step smaller than the
		// missing one.
		Optional<Step> optLastStep = map.values().stream().max(Step::compareTo);

		if (optLastStep.isPresent()) {
			for (int stepNumber = 1; stepNumber <= optLastStep.get().getStepNumber(); stepNumber++) {
				if (map.containsKey(stepNumber)) {
					steps.add(map.get(stepNumber));
				} else {
					steps.add(new Step(stepNumber));
				}
			}
		}

		this.trajectories = agentsByStep
				.entrySet()
				.stream()
				.flatMap(entry -> entry.getValue().stream())
				.map(ped -> ped.getId())
				.distinct()
				.map(id -> new Trajectory(agentsByStep, id))
				.collect(Collectors.toMap(t -> t.getPedestrianId(), t -> t));


		this.step = !steps.isEmpty() ? steps.get(0) : null;
		logger.info("finished init postvis model");
	}

	public void init(final Scenario vadere, final String projectPath) {
		this.vadere = vadere;
		this.agentsByStep = new HashMap<>();
		this.steps = new ArrayList<>();
		this.trajectories = new HashMap<>();
		this.selectedElement = null;
		this.outputPath = projectPath;
	}

	public Optional<Step> getLastStep() {
		if (!steps.isEmpty()) {
			return Optional.of(steps.get(steps.size() - 1));
		} else {
			return Optional.empty();
		}
	}

	public Optional<Step> getFirstStep() {
		if (!steps.isEmpty()) {
			return Optional.of(steps.get(0));
		} else {
			return Optional.empty();
		}
	}

	public Scenario getScenarioRunManager() {
		return vadere;
	}

	@Override
	public Optional<CellGrid> getPotentialField() {
		if (potentialContainer == null || step == null) {
			return Optional.empty();
		} else {
			try {
				return Optional.ofNullable(potentialContainer.getPotentialField(step.getStepNumber()));
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("could not load potential field "+ e.getMessage());
				return Optional.empty();
			}
		}
	}

	@Override
	public boolean isFloorFieldAvailable() {
		return getPotentialField().isPresent();
	}

	@Override
	public Collection<Agent> getAgents() {
		return getAlivePedestrians().map(t -> t.getAgent(step)).filter(ped -> ped.isPresent()).map(ped -> ped.get())
				.collect(Collectors.toList());
	}

	@Override
	public Topography getTopography() {
		return vadere.getTopography();
	}

	@Override
	public synchronized Iterator<ScenarioElement> iterator() {
		return new TopographyIterator(vadere.getTopography(), getAgents());
	}

	@Override
	public double getGridResolution() {
		return config.getGridWidth();
	}

	public synchronized Optional<Step> getStep() {
		return Optional.ofNullable(step);
	}

	public int getStepCount() {
		return steps.size();
	}

	public synchronized void setTime(final double time) {
		if (steps.size() >= 2 && steps.get(1).getSimTimeInSec().isPresent()
				&& steps.get(0).getSimTimeInSec().isPresent()) {
			double dt = steps.get(1).getSimTimeInSec().get() - steps.get(0).getSimTimeInSec().get();
			int step = (int) Math.round(time / dt) + 1;
			setStep(step);
		} else if (1.0e-10 < Math.abs(time)) {
			setStep(1);
		}
	}

	public Optional<Color> getColor(final Agent agent) {
		JsonNode jsonObj = StateJsonConverter.toJsonNode(agent);
		Optional<Map.Entry<Integer, VPredicate<JsonNode>>> firstEntry = colorEvalFunctions.entrySet().stream().filter(
				entry -> {
					try {
						return entry.getValue().test(jsonObj);
					} catch (ParseException e) {
						return false;
					}
				}).findFirst();

		if (firstEntry.isPresent()) {
			return Optional.of((Color) pedestrianColorTableModel.getValueAt(firstEntry.get().getKey(),
					PedestrianColorTableModel.COLOR_COLUMN));
		}
		return Optional.empty();
	}

	public PedestrianColorTableModel getPedestrianColorTableModel() {
		return pedestrianColorTableModel;
	}

	public synchronized void setStep(final int step) {
		// speed up the computation if every step is available, which is the default case!
		Optional<Step> optionalStep = steps.size() >= step && steps.get(step - 1).getStepNumber() == step
				? Optional.of(steps.get(step - 1)) : Optional.<Step>empty();

		if (!optionalStep.isPresent()) {
			optionalStep = steps.stream().filter(s -> s.getStepNumber() <= step).max(stepComparator);
		}

		if (!optionalStep.isPresent()) {
			logger.error("could not found step with the number: " + step);
		} else if (!this.step.equals(optionalStep.get())) {
			this.step = optionalStep.get();
			if (isVoronoiDiagramAvailable() && isVoronoiDiagramVisible()) {
				getVoronoiDiagram().computeVoronoiDiagram(
						trajectories.values().stream()
								.filter(t -> t.isPedestrianAlive(this.step))
								.map(t -> t.getAgent(this.step).get().getPosition())
								.collect(Collectors.toList()));

			}

			if (isElementSelected() && getSelectedElement() instanceof Pedestrian) {
				Trajectory trajectory = trajectories.get(getSelectedElement().getId());
				if (trajectory != null) {
					Optional<Agent> ped = trajectory.getAgent(this.step);
					setSelectedElement(ped.orElseGet(() -> null));
				}
			}

			// so the new pedestrian position is displayed!
			if (isElementSelected()) {
				notifySelectSecenarioElementListener(getSelectedElement());
			}

			/*
			 * if(isFloorFieldAvailable()) {
			 * getPotentialField();
			 * }
			 */
			setChanged();
		}
	}

	@Override
	public double getSimTimeInSec() {
		return getStep().map(this::getSimTimeInSec).orElse(0.0);
	}

	private double getSimTimeInSec(final Step step) {
		return step.getSimTimeInSec()
				.orElse(step.getStepNumber() * vadere.getScenarioStore().attributesSimulation.getSimTimeStepLength());
	}

	public synchronized void setPotentialFieldContainer(final PotentialFieldContainer container) {
		this.potentialContainer = container;
	}

	/**
	 * Returns all trajectories. E.g. also trajectories from pedestrian that are not already
	 * appeared and pedestrians that are already reach their target
	 * 
	 * @return all trajectories
	 */
	public synchronized Stream<Trajectory> getAppearedPedestrians() {
		return trajectories.values().stream().filter(t -> t.isPedestrianAppeared(this.step));
	}

	/**
	 * Returns all trajectories of pedestrians that are visible in the topography at the current
	 * time step e. g. they didn't reach their target and they already appear.
	 * 
	 * @return all trajectories of pedestrians that are visible at the current time step
	 */
	public synchronized Stream<Trajectory> getAlivePedestrians() {
		return trajectories.values().stream().filter(t -> t.isPedestrianAlive(this.step));
	}

	public boolean isEmpty() {
		return agentsByStep.size() == 0;
	}

	@Override
	public int getTopographyId() {
		return topographyId;
	}

	public String getOutputPath() {
		return outputPath;
	}

	private void clear() {
		setVoronoiDiagram(null);

		if (isFloorFieldAvailable()) {
			try {
				potentialContainer.clear();
			} catch (IOException e) {
				logger.error("could not clear potential Container: " + e.getMessage());
				e.printStackTrace();
			}
		}
		config.setShowPotentialField(false);
		potentialContainer = null;
	}
}
