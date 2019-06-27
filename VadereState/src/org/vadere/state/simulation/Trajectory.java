package org.vadere.state.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * A Trajectory is a list of {@link org.vadere.state.scenario.Pedestrian} objects, that can be seen
 * as pedestrian states of the same pedestrian. The representing pedestrian is the same, so all
 * {@link org.vadere.state.scenario.Pedestrian} objects has
 * the same id but the state of a pedestrian changes over time.
 *
 * @author Benedikt Zoennchen
 *
 */
public class Trajectory {

	private final Map<Step, Agent> trajectoryPoints;

	private Optional<Step> firstStep;

	private Optional<Step> lastStep;

	private int pedestrianId;

	private double simStepLengthInSec;

	/**
	 * Constructs an empty {@link Trajectory} for a pedestrian defined by <tt>pedestrianId</tt>.
	 *
	 * @param pedestrianId
	 */
	public Trajectory(final int pedestrianId, final double simStepLengthInSec) {
		this.pedestrianId = pedestrianId;
		this.trajectoryPoints = new HashMap<>();
		this.firstStep = Optional.empty();
		this.lastStep = Optional.empty();
		this.simStepLengthInSec = simStepLengthInSec;
	}

	/**
	 * Constructs a {@link Trajectory} for a pedestrian defined by <tt>pedestrianId</tt>
	 * by extracting the required trajectory points from <tt>pedestrianByStep</tt>.
	 *
	 * Note that calling this constructor for each pedestrian (id) is rather expensive,
	 * since the complete {@link Map} <tt>pedestrianByStep</tt> has to be iterated for each
	 * pedestrian. A better way is to construct all {@link Trajectory} objects at once.
	 *
	 * @param pedestrianByStep  container for a set of trajectories
	 * @param pedestrianId      a specific and unique pedestrian id
	 */
	public Trajectory(final Map<Step, List<Agent>> pedestrianByStep, final int pedestrianId) {
		this.pedestrianId = pedestrianId;

		// create for each step that contains an pedestrian with the specific pedestrianId
		this.trajectoryPoints = pedestrianByStep.entrySet().stream()
				.filter(entry -> containsAgent(entry.getValue()))
				.collect(Collectors.toMap(e -> e.getKey(), e -> findAnyAgent(e.getValue())));

		this.firstStep = pedestrianByStep.keySet().stream().filter(step -> trajectoryPoints.containsKey(step)).min(Step::compareTo);
		this.lastStep = pedestrianByStep.keySet().stream().filter(step -> trajectoryPoints.containsKey(step)).max(Step::compareTo);

		if (!isEmpty()) {
			fill();
		}
	}

	private boolean containsAgent(final List<Agent> agents) {
		return agents.stream().anyMatch(agent -> agent.getId() == pedestrianId);
	}

	private Agent findAnyAgent(final List<Agent> agents) {
		return agents.stream().filter(agent -> agent.getId() == pedestrianId).findAny().get();
	}

	private Step last() {
		return lastStep.get();
	}

	private Step first() {
		return firstStep.get();
	}

	private boolean hasFirstStep() {
		return firstStep.isPresent();
	}

	private boolean hasLastStep() {
		return lastStep.isPresent();
	}

	private boolean isMissing(final Step step) {
		return !contains(step);
	}

	private boolean contains(final Step step) {
		return trajectoryPoints.containsKey(step);
	}

	/**
	 * Fills in missing positions e.g. if there is are positions for steps 1, 2, 4, 7, 11 after fill
	 * there will be positions for steps 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 where position 3 = 2, 5 = 6 = 4,
	 * 8 = 9 = 10 = 7.
	 */
	public void fill() {
		Stream.iterate(first(), s -> s.isSmallerEqThan(last()), s -> s.increment())
				.filter(s -> isMissing(s))
				.forEachOrdered(s -> addStep(s, getAgent(s.decrement()).get()));
	}

	public void addStep(final Step step, @NotNull final Agent agent) {
		if(!hasFirstStep() || first().getStepNumber() > step.getStepNumber()) {
			firstStep = Optional.of(step);
		}

		if(!hasLastStep() || last().getStepNumber() < step.getStepNumber()) {
			lastStep = Optional.of(step);
		}

		trajectoryPoints.put(step, agent);
	}

	public Optional<Integer> getLifeTime() {
		return getEndStep().isPresent() && getStartStep().isPresent() ?
				Optional.of(getEndStep().get().getStepNumber() - getStartStep().get().getStepNumber()) :
				Optional.empty();
	}

	/**
	 * Returns the pedestrian id that specified this trajectory.
	 * 
	 * @return the pedestrian id that specified this trajectory
	 */
	public int getPedestrianId() {
		return pedestrianId;
	}

	/**
	 * Returns true if the pedestrian is alive at the specific time step, alive means the pedestrain
	 * appeared and does not jet
	 * disappeared.
	 * 
	 * @param step the time step
	 * @return true if the pedestrian is alive at the specific time step
	 */
	public boolean isAlive(final Step step) {
		return contains(step);
	}

	public boolean isAlive(final double simTimeInSec) {
		return isAlive(Step.toCeilStep(simTimeInSec, simStepLengthInSec));
	}

	/**
	 * Returns true if the pedestrian appeared, otherwise false.
	 * 
	 * @param step the time step
	 * @return true if the pedestrian appeared, otherwise false
	 */
	public boolean hasAppeared(final Step step) {
		return contains(step) || hasFirstStep() && first().isGreaterEqThan(step);
	}

	public boolean hasAppeared(final double simTimeInSec) {
		Step base = Step.toFloorStep(simTimeInSec,simStepLengthInSec);
		return contains(base) || hasFirstStep() && first().isGreaterEqThan(base);
	}

	/**
	 * Returns true if the pedestrian disappeared, otherwise false.
	 * 
	 * @param step the time step
	 * @return true if the pedestrian disappeared, otherwise false
	 */
	public boolean hasDisappeared(final Step step) {
		return isMissing(step) && (!hasLastStep() || last().isGreaterThan(step));
	}

	public boolean hasDisappeared(final double simTimeInSec) {
		return hasDisappeared(Step.toCeilStep(simTimeInSec, simStepLengthInSec));
	}

	private boolean isEmpty() {
		return !getStartStep().isPresent() || !getEndStep().isPresent();
	}

	/**
	 * Returns an Optional<Pedestrian> object. If the pedestrain has not appeared at step, the
	 * method will return the pedestrian at the
	 * first step it is alive. If the pedestrain has disappeared at step, this method return the
	 * pedestrian at the last step it is alive.
	 * 
	 * @param step the time step that specify the pedestrian
	 * @return an Optional<Pedestrian> object which is empty if the trajectory is completely empty.
	 */
	public Optional<Agent> getAgent(final Step step) {
		if (!isEmpty()) {
			if (isAlive(step)) {
				return Optional.of(trajectoryPoints.get(step));
			} else if (step.isSmallerEqThan(first())) {
				return Optional.of(trajectoryPoints.get(first()));
			} else if (step.isGreaterEqThan(last())) {
				return Optional.of(trajectoryPoints.get(last()));
			} else {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	public Optional<Agent> getAgent(final double simTimeInSec) {
		if(!isEmpty()) {
			Step base = Step.toFloorStep(simTimeInSec, simStepLengthInSec);
			Step next = Step.toCeilStep(simTimeInSec, simStepLengthInSec);

			if(base.equals(next) || base.equals(last())) {
				return getAgent(base);
			} else {
				double r = simTimeInSec - Step.toSimTimeInSec(base, simStepLengthInSec);
				Optional<Agent> optionalAgent1 = getAgent(base);
				Optional<Agent> optionalAgent2 = getAgent(base.increment());
				Agent agent1 = optionalAgent1.get();
				Agent agent2 = optionalAgent2.get();
				VPoint position1 = agent1.getPosition();
				VPoint position2 = agent2.getPosition();
				VPoint position = position1.add(position2.subtract(position1).scalarMultiply(r / simStepLengthInSec));
				Agent agent = agent1.clone();
				agent.setPosition(position);
				return Optional.of(agent);
			}
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Return a {@link java.util.stream.Stream<>} stream of
	 * {@link org.vadere.util.geometry.shapes.VPoint} pedestrian positions
	 * from the first step (1) to the (step.getStepNumber()) in reverse order.
	 *
	 * @param step the step of the last pedestrian position
	 * @return a stream of pedestrian positions to from 1 to step.getStepNumber() in reverse order
	 */
	public Stream<VPoint> getPositionsReverse(final Step step) {
		return getPositionsReverse(Step.toSimTimeInSec(step, simStepLengthInSec));
	}

	public Stream<VPoint> getPositionsReverse(final double simTimeInSec) {
		Step tail = Step.toFloorStep(simTimeInSec, simStepLengthInSec);
		Step head = Step.toCeilStep(simTimeInSec, simStepLengthInSec);

		Stream<VPoint> headStream = Stream.empty();
		if(!tail.equals(head)) {
			Optional<Agent> optionalAgent = getAgent(simTimeInSec);
			if(optionalAgent.isPresent()) {
				headStream = Stream.of(optionalAgent.get().getPosition());
			}
		}

		Stream<VPoint> tailStream = Stream.iterate(tail, s -> s.isGreaterEqThan(first()), s -> s.decrement())
				.map(s -> getAgent(s))
				.filter(optAgent -> optAgent.isPresent())
				.map(optAgent -> optAgent.get())
				.flatMap(agent -> toPointStream(agent));

		return Stream.concat(headStream, tailStream);
	}

	private Stream<VPoint> toPointStream(@NotNull final Agent agent) {
		// use the foot step information if available
		if(agent instanceof Pedestrian) {
			Pedestrian pedestrian = (Pedestrian)agent;
			if(!pedestrian.getFootSteps().isEmpty()) {
				Iterable<FootStep> iterable = () -> pedestrian.getFootSteps().descendingIterator();
				return StreamSupport.stream(iterable.spliterator(), false).map(footStep -> footStep.getEnd());
			}
		}
		return Stream.of(agent.getPosition());
	}

	public Optional<Step> getStartStep() {
		return firstStep;
	}

	public Optional<Step> getEndStep() {
		return lastStep;
	}
}
