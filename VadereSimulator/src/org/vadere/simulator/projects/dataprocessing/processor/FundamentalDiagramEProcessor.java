package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesFundamentalDiagramEProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.MeasurementArea;

/**
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class FundamentalDiagramEProcessor extends AreaDataProcessor<Pair<Double, Double>>  implements UsesMeasurementArea {

	private SumVoronoiAlgorithm sumVoronoiAlgorithm;
	private APedestrianVelocityProcessor pedestrianVelocityProcessor;

	public FundamentalDiagramEProcessor() {
		super("velocity", "density");
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesFundamentalDiagramEProcessor att = (AttributesFundamentalDiagramEProcessor) this.getAttributes();
		pedestrianVelocityProcessor = (APedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
		MeasurementArea measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), true);
		MeasurementArea voronoiMeasurementArea = manager.getMeasurementArea(att.getVoronoiMeasurementAreaId(), true);

		sumVoronoiAlgorithm = new SumVoronoiAlgorithm(
				key -> pedestrianVelocityProcessor.getValue(key),
				measurementArea,
				voronoiMeasurementArea);
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesFundamentalDiagramEProcessor());
		}
		return super.getAttributes();
	}

	@Override
	public void preLoop(SimulationState state) {
		super.preLoop(state);
	}

	@Override
	protected void doUpdate(SimulationState state) {
		putValue(new TimestepKey(state.getStep()), Pair.of(
				sumVoronoiAlgorithm.getVelocity(state),
				sumVoronoiAlgorithm.getDensity(state)));
	}

	@Override
	public String[] toStrings(@NotNull final TimestepKey key) {
		return new String[]{ Double.toString(getValue(key).getLeft()), Double.toString(getValue(key).getRight()) };
	}


	@Override
	public int[] getReferencedMeasurementAreaId() {
		AttributesFundamentalDiagramEProcessor att = (AttributesFundamentalDiagramEProcessor) this.getAttributes();

		return new int[]{att.getVoronoiMeasurementAreaId(), att.getMeasurementAreaId()};
	}
}
