package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianLineCrossProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;


/**
 * This processor computes the exact time a pedestrian crossed a line (last).
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class PedestrianLineCrossProcessor extends DataProcessor<PedestrianIdKey, PedestrianLineCrossProcessor.CrossInformation> {

	public class CrossInformation {
		public double crossingTime;
		public VPoint stepStart;
		public VPoint stepEnd;

		public CrossInformation(double crossingTime, FootStep step) {
			this.crossingTime = crossingTime;
			this.stepStart = step.getStart();
			this.stepEnd = step.getEnd();
		}

		public Vector2D GetExitDirection(){
			VPoint norm = stepEnd.subtract(stepStart).normZeroSafe();
			return new Vector2D(norm.getX(), norm.getY());
		}
	}

	private VLine line;

	public PedestrianLineCrossProcessor() {
		super("crossTime", "crossDirection");
		setAttributes(new AttributesPedestrianLineCrossProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian ped : peds) {
			PedestrianIdKey key = new PedestrianIdKey(ped.getId());

			for(FootStep footStep : ped.getTrajectory()) {
				if(footStep.intersects(line)) {
					double crossingTime = footStep.computeIntersectionTime(line);
					this.putValue(key, new CrossInformation(crossingTime, footStep));
				}
			}
		}
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
	public String[] toStrings(PedestrianIdKey key) {
		if(!hasValue(key)){
			return new String[]{"-", "-"};
		}

		CrossInformation crossInfo = getValue(key);
		return new String[]{Double.toString(crossInfo.crossingTime), crossInfo.GetExitDirection().toString()};
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianLineCrossProcessor());
		}
		return super.getAttributes();
	}
}
