package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processor.AttributesPedestrianLineCrossProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;

@DataProcessorClass()
public class PedestrianLineCrossProcessor extends DataProcessor<PedestrianIdKey, Double> {

	private VLine line;
	private int lastTimeStep = -1;

	public PedestrianLineCrossProcessor() {
		super("crossTime");
		setAttributes(new AttributesPedestrianLineCrossProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		if(state.getStep() > lastTimeStep) {
			Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

			for(Pedestrian ped : peds) {
				PedestrianIdKey key = new PedestrianIdKey(ped.getId());
				for(FootStep footStep : ped.getFootSteps()) {
					VPoint start = footStep.getStart();
					VPoint end = footStep.getEnd();

					if(GeometryUtils.intersectLineSegment(new VPoint(line.getP1()), new VPoint(line.getP2()), start,end)) {
						VPoint intersectionPoint = GeometryUtils.intersectionPoint(line.getX1(), line.getY1(), line.getX2(), line.getY2(), start.getX(), start.getY(), end.getX(), end.getY());

						double dStart = intersectionPoint.distance(start);
						double stepLength = start.distance(end);
						double duration = footStep.getEndTime() - footStep.getStartTime();
						double intersectionTime = footStep.getStartTime() + duration * (dStart / stepLength);


						assert !hasValue(key);
						this.putValue(key, intersectionTime);
					}
				}
			}
		}

		lastTimeStep = state.getStep();
	}

	public VLine getLine() {
		return line;
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianLineCrossProcessor att = (AttributesPedestrianLineCrossProcessor) this.getAttributes();
		this.line = new VLine(att.getP1(), att.getP2());
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianLineCrossProcessor());
		}
		return super.getAttributes();
	}
}
