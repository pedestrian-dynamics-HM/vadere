package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesCrossingTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.Optional;

@DataProcessorClass()
public class PedestrianCrossingTimeProcessor extends DataProcessor<PedestrianIdKey, PedestrianCrossingTimeProcessor.PedestrianCrossingTimeProcessorCrossInformation> implements UsesMeasurementArea {

	public static class PedestrianCrossingTimeProcessorCrossInformation{
		public double startTime;
		@Nullable
		public Double exitingTime;
		public VPoint enteringPoint;
		public VPoint exitingPoint;

		public PedestrianCrossingTimeProcessorCrossInformation(double startTime, VPoint enteringPoint) {
			this.startTime = startTime;
			this.enteringPoint = enteringPoint;
			this.exitingTime = null;
			this.exitingPoint = null;
		}

		public PedestrianCrossingTimeProcessorCrossInformation setEnd(double endTime, VPoint leavingPoint) {
			this.exitingTime = endTime;
			this.exitingPoint = leavingPoint;
			return this;
		}

		@Nullable
		public Vector2D GetExitDirection(){
			if(exitingPoint == null) {
				return null;
			}

			VPoint norm = exitingPoint.subtract(enteringPoint).normZeroSafe();
			return new Vector2D(norm.getX(), norm.getY());
		}
	}

	private MeasurementArea measurementArea;
	private VRectangle measurementAreaVRec;

	private static Logger logger = Logger.getLogger(PedestrianCrossingTimeProcessor.class);

	public PedestrianCrossingTimeProcessor() {
		super("crossStartTime", "crossEndTime", "crossDirection");
		setAttributes(new AttributesCrossingTimeProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian ped : peds) {
			PedestrianIdKey key = new PedestrianIdKey(ped.getId());

			boolean alreadyMeasured = hasCrossEndTime(key);
			if(alreadyMeasured){
				continue;
			}

			for(FootStep footStep : ped.getTrajectoryOfSimulationStep()) {

				Optional<FootStep.LineRectClippingResult> optionalFootStepClippingResult
						= footStep.computeClipping(measurementAreaVRec);

				boolean footStepClipsMeasurementArea = optionalFootStepClippingResult.isPresent();
				if(!footStepClipsMeasurementArea){
					if(hasCrossStartTime(key)){
						setExit(key, footStep.getStartTime(), footStep.getStart());
					}
					continue;
				}

				FootStep.LineRectClippingResult footStepClippingResult = optionalFootStepClippingResult.get();
				if(!hasCrossStartTime(key)){
					FootStep.IntersectionPointAndTime clippingStart = footStepClippingResult.clippingStart();
					setEnter(key, clippingStart.time(), clippingStart.point());
				}

				boolean footStepExitsMeasurementArea = footStepClippingResult.exitsBoundary();
				if(footStepExitsMeasurementArea){
					FootStep.IntersectionPointAndTime clippingEnd = footStepClippingResult.clippingEnd();
					setExit(key, clippingEnd.time(), clippingEnd.point());
				}
			}
		}
	}

	private void setEnter(@NotNull final PedestrianIdKey key, double time, VPoint enteringPoint) {
		putValue(key, new PedestrianCrossingTimeProcessorCrossInformation(time, enteringPoint));
	}

	private void setExit(@NotNull final PedestrianIdKey key, double time, VPoint exitingPoint) {
		putValue(key, getValue(key).setEnd(time, exitingPoint));
	}

	private boolean hasCrossStartTime(@NotNull final PedestrianIdKey key) {
		PedestrianCrossingTimeProcessorCrossInformation times = getValue(key);
		return times != null;
	}

	private boolean hasCrossEndTime(@NotNull final PedestrianIdKey key) {
		PedestrianCrossingTimeProcessorCrossInformation times = getValue(key);
		return times!=null && times.exitingTime != null;
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesCrossingTimeProcessor att = (AttributesCrossingTimeProcessor) this.getAttributes();
		this.measurementArea  = manager.getMeasurementArea(att.getMeasurementAreaId(), true);
		measurementAreaVRec = measurementArea.asVRectangle();
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesCrossingTimeProcessor());
		}
		return super.getAttributes();
	}

	@Override
	public String[] toStrings(@NotNull final  PedestrianIdKey key) {
		PedestrianCrossingTimeProcessorCrossInformation times = getValue(key);
		if(times == null) {
			return new String[]{"-", "-", "-"};
		}

		Vector2D exitDirection = times.GetExitDirection();
		return new String[]{
				Double.toString(times.startTime),
				times.exitingTime == null ? "" : Double.toString(times.exitingTime),
				exitDirection == null ? "" : exitDirection.toString()
		};
	}


	@Override
	public int[] getReferencedMeasurementAreaId() {
		AttributesCrossingTimeProcessor att = (AttributesCrossingTimeProcessor) this.getAttributes();
		return new int[]{att.getMeasurementAreaId()};
	}
}
