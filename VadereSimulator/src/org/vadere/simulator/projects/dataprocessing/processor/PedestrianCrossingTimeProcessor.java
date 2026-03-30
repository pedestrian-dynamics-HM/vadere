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

	public static final String crossStartTime = "crossStartTime";
	public static final String crossEndTime = "crossEndTime";
	public static final String crossDirection = "crossDirection";

	public static class PedestrianCrossingTimeProcessorCrossInformation{
		public double enteringTime;
		@Nullable
		public Double exitingTime;
		public VPoint enteringPoint;
		public VPoint exitingPoint;

		// Used to track multiple crossings of the measurement area.
		// If a pedestrian enters the measurement area, then exits and then enters again, we want to track both crossings
		// and use the one with the longer crossing distance for the final output.
		@Nullable
		public Double enterAgainTime;
		@Nullable
		public VPoint enteringAgainPoint;

		public PedestrianCrossingTimeProcessorCrossInformation(double enteringTime, VPoint enteringPoint) {
			this.enteringTime = enteringTime;
			this.enteringPoint = enteringPoint;
			this.exitingTime = null;
			this.exitingPoint = null;
			this.enterAgainTime = null;
			this.enteringAgainPoint = null;
		}

		public PedestrianCrossingTimeProcessorCrossInformation setEnterAgain(double enteringTime, VPoint enteringPoint) {
			if(exitingTime == null || exitingPoint == null){
				throw new RuntimeException("setEnterAgain even though setEnd was not called before.");
			}

			this.enterAgainTime = enteringTime;
			this.enteringAgainPoint = enteringPoint;
			return this;
		}

		public PedestrianCrossingTimeProcessorCrossInformation setEnd(double endTime, VPoint leavingPoint) {
			if(enterAgainTime != null && enteringAgainPoint != null){
				double currentDistanceSq = exitingPoint.distanceSq(enteringPoint);
				double newDistanceSq = leavingPoint.distanceSq(enteringAgainPoint);

				boolean replaceWithLongerCrossing = newDistanceSq > currentDistanceSq;
				if(replaceWithLongerCrossing) {
					this.enteringTime = enterAgainTime;
					this.enteringPoint = enteringAgainPoint;
					this.exitingTime = endTime;
					this.exitingPoint = leavingPoint;
				}

				this.enterAgainTime = null;
				this.enteringAgainPoint = null;

				return this;
			}

			this.exitingTime = endTime;
			this.exitingPoint = leavingPoint;
			return this;
		}

		public boolean isInside() {
			return exitingTime == null || enterAgainTime != null;
		}

		public boolean isOutside() {
			return exitingTime != null && enterAgainTime == null;
		}

		@Nullable
		public Vector2D getExitDirection(){
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
		super(crossStartTime, crossEndTime, crossDirection);
		setAttributes(new AttributesCrossingTimeProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian ped : peds) {
			PedestrianIdKey key = new PedestrianIdKey(ped.getId());

			for(FootStep footStep : ped.getTrajectoryOfSimulationStep()) {

				Optional<FootStep.StepRectClippingResult> optionalFootStepClippingResult
						= footStep.computeClipping(measurementAreaVRec);

				boolean footStepClipsMeasurementArea = optionalFootStepClippingResult.isPresent();
				if(!footStepClipsMeasurementArea){
					if(isConsideredInside(key)){
						setExit(key, footStep.getStartTime(), footStep.getStart());
					}
					continue;
				}

				FootStep.StepRectClippingResult footStepClippingResult = optionalFootStepClippingResult.get();
				if(isConsideredOutside(key)){
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
		PedestrianCrossingTimeProcessorCrossInformation current = getValue(key);
		if(current == null){
			putValue(key, new PedestrianCrossingTimeProcessorCrossInformation(time, enteringPoint));
		}else{
			current.setEnterAgain(time, enteringPoint);
			putValue(key, current);
		}
	}

	private void setExit(@NotNull final PedestrianIdKey key, double time, VPoint exitingPoint) {
		PedestrianCrossingTimeProcessorCrossInformation current = getValue(key);
		if(current == null){
			throw new RuntimeException("Setting exit was called without setting the enter information first");
		}

		putValue(key, current.setEnd(time, exitingPoint));
	}

	private boolean isConsideredOutside(@NotNull final PedestrianIdKey key) {
		PedestrianCrossingTimeProcessorCrossInformation times = getValue(key);
		return times == null || times.isOutside();
	}

	private boolean isConsideredInside(@NotNull final PedestrianIdKey key) {
		PedestrianCrossingTimeProcessorCrossInformation times = getValue(key);
		return times!=null && times.isInside();
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesCrossingTimeProcessor att = (AttributesCrossingTimeProcessor) this.getAttributes();
		MeasurementArea measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), true);
		init(measurementArea);
	}

	public void init(MeasurementArea measurementArea) {
		this.measurementArea = measurementArea;
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
	public String[] toStrings(@NotNull final PedestrianIdKey key) {
		PedestrianCrossingTimeProcessorCrossInformation times = getValue(key);
		if(times == null) {
			return new String[]{"-", "-", "-"};
		}

		Vector2D exitDirection = times.getExitDirection();
		return new String[]{
				Double.toString(times.enteringTime),
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
