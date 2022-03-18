package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepIdDataKey;
import org.vadere.state.attributes.processor.AttributesAerosolCloudDataProcessor;
import org.vadere.state.scenario.AerosolCloud;

import java.util.Collection;
import java.util.Locale;

/**
 * <p>This processor writes out the shape and the pathogen load of all {@link AerosolCloud}s.
 * Each row corresponds to one aerosol cloud at a given simulation time. The shape of an
 * aerosol cloud is described by the radius and its center ([x, y]-coordinates).
 * </p>
 *
 * <p>To reduce the size of the output file, one can reduce the sample frequency, that is
 * the processor writes out only every {@param sampleEveryNthSimStep}.
 * </p>
 *
 * @author Simon Rahn
 */
@DataProcessorClass()
public class AerosolCloudDataProcessor extends DataProcessor<TimestepIdDataKey, String> {

    private int sampleEveryNthSimStep;

    public AerosolCloudDataProcessor() {
        super("pathogenLoad", "radius", "centerX", "centerY");
        setAttributes(new AttributesAerosolCloudDataProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {

        int timeStep = state.getStep();
        if (timeStep % sampleEveryNthSimStep == 0) {
            Collection<AerosolCloud> clouds = state.getTopography().getAerosolClouds();
            for (AerosolCloud cloud : clouds) {
                putValue(new TimestepIdDataKey(timeStep, cloud.getId()), dataToString(cloud));
            }
        }
    }

    @Override
    public void init(ProcessorManager manager) {
        super.init(manager);
        AttributesAerosolCloudDataProcessor attr = (AttributesAerosolCloudDataProcessor) this.getAttributes();
        sampleEveryNthSimStep = attr.getSampleEveryNthSimStep();
    }

    private String dataToString(AerosolCloud aerosolCloud) {
        String dataAsString = String.format(Locale.US, "%f %f %f %f",
                aerosolCloud.getCurrentPathogenLoad(),
                aerosolCloud.getRadius(),
                aerosolCloud.getCenter().x,
                aerosolCloud.getCenter().y
        );

        return dataAsString;
    }
}