package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.projects.dataprocessing.procesordata.AreaGroupMetaData;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesGroupHarmonicMeanDistProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Manuel Hertle
 */

@DataProcessorClass(label = "GroupHarmonicMeanDistProcessor")
public class GroupHarmonicMeanDistProcessor extends GroupDistProcessor {

    public GroupHarmonicMeanDistProcessor() {
        super();
        setAttributes(new AttributesGroupHarmonicMeanDistProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesGroupHarmonicMeanDistProcessor attMinDist =
                (AttributesGroupHarmonicMeanDistProcessor) this.getAttributes();
        this.setHeaders("HWMDist");
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

            //calcute weighted mean distance from all group centers to the current group center
            double weightedMeanDistance = 0;
            List<Double> distances = allgroupCenters.stream()
                    .map(currGroupCenter::distance)
                    .collect(Collectors.toList());
            for (Double d : distances) {
                weightedMeanDistance += 1 / d; // TODO not correct and better use sth else than centroid
            }
            return Optional.of(weightedMeanDistance);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesGroupHarmonicMeanDistProcessor());
        }

        return super.getAttributes();
    }
}
