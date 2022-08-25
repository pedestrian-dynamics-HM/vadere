package org.vadere.simulator.projects.dataprocessing.processor;


import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.BonnMotionKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesBonnMotionTrajectoryProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.ReferenceCoordinateSystem;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This processor creates trace files based on the output of BonnMotion. BonnMotion is a mobility
 * scenario generation and analysis tool developed by the Communication Systems group at University
 * of Bonn et. al. (https://sys.cs.uos.de/bonnmotion/index.shtml)
 *
 * This format is used by the omnet++ to add pre-recorded mobility data to a network simulation.
 * https://doc.omnetpp.org/inet/api-current/neddoc/inet.mobility.single.BonnMotionMobility.html
 *
 * The trace is based on a node-by-line waypoints. Thus each line represents  one pedestrian and
 * each waypoint consist of 3-Tupel (2D) or 4-Tupel (3D) encoding (time X-Coord Y-Coord Z-Coord)
 *
 * This Processor only supports 2D waypoints.
 *
 * @author Stefan Schuhbäck
 */
@DataProcessorClass()
public class BonnMotionTrajectoryProcessor extends DataProcessor<BonnMotionKey, List<Pair<Double, VPoint>>> {

	private PedestrianPositionProcessor pedestrianPositionProcessor;
	private AttributesBonnMotionTrajectoryProcessor attr;


	public BonnMotionTrajectoryProcessor() {
		super(""); // no headers.
		setAttributes(new AttributesBonnMotionTrajectoryProcessor());
	}

	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
		this.attr =
				(AttributesBonnMotionTrajectoryProcessor) this.getAttributes();
		this.pedestrianPositionProcessor = (PedestrianPositionProcessor) manager.getProcessor(
				attr.getPedestrianPositionProcessorId()
		);
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesBonnMotionTrajectoryProcessor());
		}

		return super.getAttributes();
	}

	@Override
	protected void doUpdate(SimulationState state) {
		//ensure pedestrianPositionProcessor was updated.
		this.pedestrianPositionProcessor.doUpdate(state);
	}


	/**
	 * Use the trajectory data from the {@link PedestrianPositionProcessor} and create the
	 * BonnMotion trajectory. Apply the scale and transformation set in the attributes.
	 *
	 * @param state Last simulation state
	 */
	@Override
	public void postLoop(SimulationState state) {
		double simTimeStepLength =
				state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength();

		double boundHeight = state.getScenarioStore().getTopography().getBounds().getHeight();
		ReferenceCoordinateSystem coordRef = state.getScenarioStore().getTopography().getAttributes().getReferenceCoordinateSystem();

		// retrieve trajectory data from pedestrianPositionProcessor and transform them to
		// the BonnMotion trajectory.
		Map<TimestepPedestrianIdKey, VPoint> trajectories = this.pedestrianPositionProcessor.getData();

		for (TimestepPedestrianIdKey e : trajectories.keySet()) {
			int pedId = e.getPedestrianId();
			double time = e.getTimestep() * simTimeStepLength;
			// make copy to apply transformations
			VPoint point = new VPoint(trajectories.get(e));

			if (attr.getOrigin().equals("upper left")){
				point.y = boundHeight - point.y;
			}

			if(attr.isApplyOffset() && coordRef != null){
				point  = point.add(coordRef.getTranslation());
			}

			point = point.multiply(attr.getScale());
			point = point.add(attr.getTranslate());

			Pair<Double, VPoint> wayPoint = Pair.of(time, point);
			addWayPoint(pedId, wayPoint);}

		sortWayPoints();

	}

	@Override
	public String[] toStrings(BonnMotionKey key) {
		List<Pair<Double, VPoint>> dataList = getValue(key);
		if (dataList == null) {
			return new String[]{"0 0 0"};
		} else {
			String data = dataList.stream()
					.map(pair -> String.format("%f %f %f",
							pair.getKey(), pair.getRight().x, pair.getRight().y))
					.collect(Collectors.joining(" "));
			return new String[]{data};
		}
	}


	// ensure the correct order for each line (aka pedestrian)
	private synchronized void sortWayPoints() {
		getData().entrySet().forEach(e -> {
			List<Pair<Double, VPoint>> dataList = e.getValue();
			dataList.sort(Comparator.comparing(Pair::getLeft));
		});
	}

	// add wayPoint for given pedId to value list. If it's the first element create the list.
	private synchronized void addWayPoint(int pedId, Pair<Double, VPoint> wayPoint) {
		BonnMotionKey bonnMotionKey = new BonnMotionKey(pedId);
		List<Pair<Double, VPoint>> dataList = getValue(bonnMotionKey);
		if (dataList == null) {
			dataList = new ArrayList<>();
			dataList.add(wayPoint);
			putValue(bonnMotionKey, dataList);
		} else {
			dataList.add(wayPoint);
		}

	}
}
