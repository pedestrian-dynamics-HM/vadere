package org.vadere.simulator.control;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TestSourceController {
    ArrayList<TestSourceControllerUsingConstantSpawnRate.SourceTestData> sourceTestData;

    @Before
    public void init() {
        sourceTestData = new ArrayList<>();
    }

    public SourceControllerFactory getSourceControllerFactory(SourceTestData d) {
        return new SingleSourceControllerFactory();
    }

    protected SourceTestData first() {
        return sourceTestData.get(0);
    }

    protected SourceTestData second() {
        return sourceTestData.get(1);
    }

    protected int countPedestrians(int source) {
        return sourceTestData.get(source).topography.getElements(Pedestrian.class).size();
    }

    protected int countPedestriansAndRemove(int source) {
        int ret =  sourceTestData.get(source).topography.getElements(Pedestrian.class).size();
        sourceTestData.get(source).topography.getPedestrianDynamicElements().clear();
        return ret;
    }

    protected void pedestrianCountEquals(int expected) {
        assertEquals(expected, countPedestrians(0));
    }



    public void initialize(SourceTestAttributesBuilder builder) throws IOException {

        SourceTestData d = new SourceTestData();

        d.attributesSource = builder.getResult();
        d.attributesPedestrian = new AttributesAgent();

        d.random = new Random(builder.getRandomSeed());

        d.source = new Source(d.attributesSource);
        d.pedestrianFactory = new TestDynamicElementFactory(d);
        d.sourceControllerFactory = getSourceControllerFactory(d);

        d.sourceController = d.sourceControllerFactory.create(d.topography, d.source,
                d.pedestrianFactory, d.attributesPedestrian, d.random);

        sourceTestData.add(d);
    }

    class TestDynamicElementFactory implements DynamicElementFactory {

        public int pedestrianIdCounter = 0;
        public  SourceTestData d;

        public TestDynamicElementFactory(SourceTestData d) {
            this.d = d;
        }

        @Override
        public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {
            AttributesAgent att = new AttributesAgent(
                    d.attributesPedestrian, registerDynamicElementId(null, id));
            Pedestrian ped = new Pedestrian(att, d.random);
            ped.setPosition(position);
            return ped;
        }

        @Override
        public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Attributes attr, Class<T> type) {
            AttributesAgent att = new AttributesAgent(
                    d.attributesPedestrian, registerDynamicElementId(null, id));
            Pedestrian ped = new Pedestrian(att, d.random);
            ped.setPosition(position);
            return ped;
        }

        @Override
        public int registerDynamicElementId(Topography topography, int id) {
            return id > 0 ? id : ++pedestrianIdCounter;
        }

        @Override
        public int getNewDynamicElementId(Topography topography) {
            return registerDynamicElementId(topography, AttributesAgent.ID_NOT_SET);
        }

        @Override
        public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
            return createElement(position, AttributesAgent.ID_NOT_SET, Pedestrian.class).getShape();
        }

    }

    class SourceTestData {
        public Random random;
        public AttributesAgent attributesPedestrian;
        public TestDynamicElementFactory pedestrianFactory;
        public Source source;
        public Topography topography = new Topography();
        public SourceController sourceController;
        public AttributesSource attributesSource;
        public SourceControllerFactory sourceControllerFactory;
        public long randomSeed = 0;
    }
}
