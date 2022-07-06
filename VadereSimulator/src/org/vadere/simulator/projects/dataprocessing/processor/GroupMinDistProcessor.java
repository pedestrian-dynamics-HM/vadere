package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.projects.dataprocessing.procesordata.AreaGroupMetaData;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesGroupMinDistProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * @author Manuel Hertle
 */

@DataProcessorClass(label = "GroupMinDistProcessor")
public class GroupMinDistProcessor extends GroupDistProcessor {

    public GroupMinDistProcessor() {
        super();
        setAttributes(new AttributesGroupMinDistProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesGroupMinDistProcessor attMinDist =
                (AttributesGroupMinDistProcessor) this.getAttributes();
        this.setHeaders("MinDist");
    }

    @Override
    public Optional<Double> getDistance(AreaGroupMetaData from, Collection<AreaGroupMetaData> to) {
        int groupId = from.getGroup().getID();
        if (from.getCentroid().isPresent()) {
            VPoint currGroupCenter = from.getCentroid().get();
            List<VPoint> allgroupCenters = to.stream()
                    .filter(data -> data.getGroup().getID() != groupId && data.getCentroid().isPresent()) //ignore groups without centroid
                    .map(data -> data.getCentroid().get())
                    .collect(Collectors.toList());

            //calcute minimum distance from all group centers to the current group center
            OptionalDouble minDistance = allgroupCenters.stream()
                    .mapToDouble(currGroupCenter::distance)
                    .min();
            return minDistance.isPresent() ? Optional.of(minDistance.getAsDouble()) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesGroupMinDistProcessor());
        }

        return super.getAttributes();
    }
}
