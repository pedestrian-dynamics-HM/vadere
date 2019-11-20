package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesFundamentalDiagramDProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.MeasurementArea;

/**
 * <p>This processor computes the fundamental diagram by computing at a certain time the
 * <tt>density</tt> and the <tt>velocity</tt> defined by the Voronoi-diagram where the points
 * are the pedestrian positions. The density is equal to the sum of areas of all Voronoi-cells
 * intersecting the <tt>measurementArea</tt> divided by the area of the <tt>measurementArea</tt>.
 * The velocity of a pedestrian is defined by its velocity times the area of its Voronoi-cell.
 * Therefore the <tt>velocity</tt> is the sum of all those pedestrian velocities of Voronoi-cells
 * intersecting the <tt>measurementArea</tt> divided by the <tt>measurementArea</tt>.</p>
 *
 * <p>For more details see zhang-2011 (doi:10.1088/1742-5468/2011/06/P06004) Method D.</p>
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class FundamentalDiagramDProcessor extends AreaDataProcessor<Pair<Double, Double>>  implements UsesMeasurementArea {

	private IntegralVoronoiAlgorithm integralVoronoiAlgorithm;
	private APedestrianVelocityProcessor pedestrianVelocityProcessor;

	public FundamentalDiagramDProcessor() {
		super("velocity", "density");
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesFundamentalDiagramDProcessor att = (AttributesFundamentalDiagramDProcessor) this.getAttributes();
		pedestrianVelocityProcessor = (APedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
		MeasurementArea measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), true);
		MeasurementArea voronoiMeasurementArea = manager.getMeasurementArea(att.getVoronoiMeasurementAreaId(), true);

		integralVoronoiAlgorithm = new IntegralVoronoiAlgorithm(
				key -> pedestrianVelocityProcessor.getValue(key),
				measurementArea,
				voronoiMeasurementArea);
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesFundamentalDiagramDProcessor());
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
				integralVoronoiAlgorithm.getVelocity(state),
				integralVoronoiAlgorithm.getDensity(state)));
	}

	@Override
	public String[] toStrings(@NotNull final TimestepKey key) {
		return new String[]{ Double.toString(getValue(key).getLeft()), Double.toString(getValue(key).getRight()) };
	}


	@Override
	public int[] getReferencedMeasurementAreaId() {
		AttributesFundamentalDiagramDProcessor att = (AttributesFundamentalDiagramDProcessor) this.getAttributes();
		return new int[]{att.getVoronoiMeasurementAreaId(), att.getMeasurementAreaId()};
	}
}
