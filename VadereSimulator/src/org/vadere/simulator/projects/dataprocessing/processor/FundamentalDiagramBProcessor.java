package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesFundamentalDiagramBProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>This processor computes the fundamental diagram by computing an (average) velocity and the density for each
 * pedestrian over a certain area <tt>measurementArea</tt>. The <tt>velocity</tt> of a pedestrian is the distance walked inside
 * the <tt>measurementArea</tt> divided by the required time. The <tt>density</tt> of a pedestrian is the integral
 * of the number of pedestrians inside the <tt>measurementArea</tt> (integrated over the time) divided by the area
 * of the <tt>measurementArea</tt> and the time required to walk through the <tt>measurementArea</tt>. The bound of
 * integration is the time the pedestrians enters the <tt>measurementArea</tt> and the time the pedestrian exits
 * the <tt>measurementArea</tt>.</p>
 *
 * <p>For more details see zhang-2011 (doi:10.1088/1742-5468/2011/06/P06004) Method B.</p>
 *
 * <p>Note that this processor does only work if pedestrians do not move multiple times through <tt>measurementArea</tt></p>
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class FundamentalDiagramBProcessor extends DataProcessor<PedestrianIdKey, Pair<Double, Double>> implements UsesMeasurementArea {

	private static Logger logger = Logger.getLogger(Topography.class);

	private MeasurementArea measurementArea;
	private VRectangle measurementAreaVRec;
	private PedestrianTrajectoryProcessor pedestrianTrajectoryProcessor;

	public FundamentalDiagramBProcessor() {
		super("velocity", "density");
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesFundamentalDiagramBProcessor att = (AttributesFundamentalDiagramBProcessor) this.getAttributes();
		pedestrianTrajectoryProcessor = (PedestrianTrajectoryProcessor) manager.getProcessor(att.getPedestrianTrajectoryProcessorId());
		measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), false);
		measurementAreaVRec = measurementArea.asVRectangle();
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesFundamentalDiagramBProcessor());
		}
		return super.getAttributes();
	}

	@Override
	public void preLoop(SimulationState state) {
		super.preLoop(state);
	}

	@Override
	protected void doUpdate(SimulationState state) {
		pedestrianTrajectoryProcessor.update(state);
	}

	@Override
	public void postLoop(SimulationState state) {
		super.postLoop(state);
		pedestrianTrajectoryProcessor.postLoop(state);
		Map<PedestrianIdKey, VTrajectory> trajectoryMap = pedestrianTrajectoryProcessor.getData();
		Map<PedestrianIdKey, VTrajectory> cutTrajectoryMap = new HashMap<>();

		/**
		 * (1) Cut the trajectories due to the measurement area.
		 */
		for(Map.Entry<PedestrianIdKey, VTrajectory> trajectoryEntry : trajectoryMap.entrySet()) {
			PedestrianIdKey key = trajectoryEntry.getKey();
			VTrajectory trajectory = trajectoryEntry.getValue();
			VTrajectory clone = trajectory.cut(measurementAreaVRec);
			cutTrajectoryMap.put(key, clone);
		}

		/**
		 * (2) Compute for each pedestrian its velocity v_i and its density \rho_i see zhang-2011 Method B
		 */
		for(Map.Entry<PedestrianIdKey, VTrajectory> trajectoryEntry : cutTrajectoryMap.entrySet()) {
			PedestrianIdKey key = trajectoryEntry.getKey();
			VTrajectory trajectory = trajectoryEntry.getValue();
			if(!trajectory.isEmpty()) {
				double density = density(key, cutTrajectoryMap);
				double velocity = trajectory.speed().orElse(0.0);
				putValue(key, Pair.of(velocity, density));
			}
		}
	}

	private double density(@NotNull final PedestrianIdKey key, @NotNull final Map<PedestrianIdKey, VTrajectory> cutTrajectoryMap) {
		VTrajectory pedTrajectory = cutTrajectoryMap.get(key);
		Optional<Double> duration = pedTrajectory.duration();

		double densityIntegral = cutTrajectoryMap.values()
				.stream()
				.map(trajectory -> trajectory.cut(pedTrajectory.getStartTime().get(), pedTrajectory.getEndTime().get()))
				.filter(trajectory -> !trajectory.isEmpty())
				//.filter(trajectory -> trajectory.isInBetween(pedTrajectory))
				//.sorted(Comparator.comparingDouble(t -> t.getStartTime().get()))
				.mapToDouble(trajectory -> (trajectory.getEndTime().get() - trajectory.getStartTime().get()))
				.sum();

		densityIntegral /= duration.get();
		densityIntegral /= measurementAreaVRec.getArea();

		return densityIntegral;

		/*List<Triple<Double, Double, Integer>> integralValues = new LinkedList<>();

		double start;
		double end;
		int i = 0;
		PriorityQueue<VTrajectory> integralElements = new PriorityQueue<>(Comparator.comparingDouble(o -> o.getEndTime().get()));

		while (i < sortedTrajectories.size()) {
			start = sortedTrajectories.get(i).getStartTime().get();
			integralElements.add(sortedTrajectories.get(i));

			boolean hasNext = i < sortedTrajectories.size() - 1;
			VTrajectory next = hasNext ? sortedTrajectories.get(i+1) : null;
			double nextStartTime = hasNext ? next.getStartTime().get() : -1.0;

			while(!integralElements.isEmpty() && (!hasNext || integralElements.peek().getEndTime().get() <= nextStartTime)) {
				double endTime = integralElements.peek().getEndTime().get();
				end = endTime;
				integralValues.add(Triple.of(start, end, integralElements.size()));
				integralElements.poll();
				start = end;
			}

			if(hasNext) {
				end = nextStartTime;
				integralValues.add(Triple.of(start, end, integralElements.size()));
				integralElements.add(next);
			}
			i++;
		}

		double densityIntegral = 0.0;
		for(Triple<Double, Double, Integer> entry : integralValues) {
			double tStart = entry.getLeft();
			double tEnd = entry.getMiddle();
			int N = entry.getRight();
			densityIntegral += (N * (tEnd - tStart));
		}
		densityIntegral /= duration;
		densityIntegral /= measurementArea.getArea();
		return densityIntegral;*/
	}

	@Override
	public String[] toStrings(@NotNull final PedestrianIdKey key) {
		return new String[]{ Double.toString(getValue(key).getLeft()), Double.toString(getValue(key).getRight()) };
	}


	@Override
	public int[] getReferencedMeasurementAreaId() {
		AttributesFundamentalDiagramBProcessor att = (AttributesFundamentalDiagramBProcessor) this.getAttributes();
		return new int[]{att.getMeasurementAreaId()};
	}
}
