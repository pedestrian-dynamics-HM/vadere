package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;


/**
 * @author Daniel Lehmberg
 *
 * This processor writes density values at unstructured points of an arbitrary measurement area. It first computes
 * the (triangular) mesh, writes the desciption of this mesh (for later data processing) and then forwards the
 * the density compuation to AreaDensityMeshpointAlgorithm. Note that the points in the mesh are static over the
 * entire simulation and are independent of pedestrian positions.
 *
 * With this processor it is possible to analyze/visualize pedestrian density evolution of any shape in the domain.
 *
 * See pull request !64 and issue #123 for details.
 *
 */
@DataProcessorClass(label = "AreaDensityMeshpointProcessor")
public class AreaDensityMeshpointProcessor extends AreaDensityProcessor implements UsesMeasurementArea {

    // TODO AreaDensityProcessor extends AreaDataProcessor<Double>, I actually need something along the lines of
    //   AreaDataProcessor<Collection<Double>> (for each point the computed density).

    public AreaDensityMeshpointProcessor(){
        super();
        setAttributes(new AttributesAreaDensityVoronoiProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);

        // TODO: Compute the mesh according to the user settings (in AttributesAreaDensityMeshpointProcessor)
        // TODO: There should be *no* points in obstacles

        // TODO: save the mesh as a file (for later usage) -- the path to the directory should be accessible via the
        //   ProcessorManager

        this.setAlgorithm(new AreaDensityMeshpointAlgorithm(getMeasurementArea(), false));
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityVoronoiProcessor());
        }

        return super.getAttributes();
    }


    @Override
    public int[] getReferencedMeasurementAreaId() {
        AttributesAreaDensityVoronoiProcessor att = (AttributesAreaDensityVoronoiProcessor) this.getAttributes();
        return new int[]{att.getMeasurementAreaId(), att.getMeasurementAreaId()};
    }
}
