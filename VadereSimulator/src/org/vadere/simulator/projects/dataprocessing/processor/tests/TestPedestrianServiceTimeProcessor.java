package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventTimeKey;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianElementCountingProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianLifetimeProcessor;
import org.vadere.state.attributes.distributions.AttributesPoissonDistribution;
import org.vadere.state.attributes.processor.AttributesTestServiceTimeProcessor;
import org.vadere.state.scenario.Target;
import org.vadere.util.logging.Logger;

import java.util.Map;

/**
 * This test is designed for queues
 * This test uses Little's Law N = l * S
 * N : avg agents in the system
 * S : avg waiting time in the system.
 * l : avg arrival rate
 * N is represented by the lower/upper bound taken from the processor attributes
 */
@DataProcessorClass()
public class TestPedestrianServiceTimeProcessor extends TestProcessor {
    private final PedestrianElementCountingProcessor elementCountingProcessor;
    private final PedestrianLifetimeProcessor lifetimeProcessor;

    private AttributesTestServiceTimeProcessor attrib;
    Target target;
    public TestPedestrianServiceTimeProcessor(){
        super("test-serviceTime");
        this.elementCountingProcessor = new PedestrianElementCountingProcessor();
        this.lifetimeProcessor = new PedestrianLifetimeProcessor();
    }
    private static final Logger logger = Logger.getLogger(TestPedestrianServiceTimeProcessor.class);

    @Override
    protected void doUpdate(SimulationState state) {
        this.elementCountingProcessor.update(state);
        this.lifetimeProcessor.update(state);
    }

    @Override
    public void postLoop(SimulationState state) {
        super.postLoop(state);

        var source = state.getTopography().getSource(1);
        var sourceAttrib = source.getAttributes();
        var distrbAttrib = sourceAttrib.getSpawnerAttributes().getDistributionAttributes();
        var numberPedsPerSec = ((AttributesPoissonDistribution)distrbAttrib).getNumberPedsPerSecond();

        Map<EventTimeKey, Integer> countProcessorData =
                this.elementCountingProcessor.getData();

        /* way to get the avg agents per simstep
        double countavg = countProcessorData.values().stream()
                .mapToInt(i->i)
                .sum()
                /countProcessorData.values().size();
        */

        Map<PedestrianIdKey, Double> lifeProcessorData =
                this.lifetimeProcessor.getData();
        double liveavg = lifeProcessorData .values().stream()
                .mapToDouble(d->d)
                .sum()
                /lifeProcessorData.values().size();

        var lowerbound = attrib.getLowerBound();
        var upperbound = attrib.getUpperBound();
        var actualval = numberPedsPerSec*liveavg;
        handleAssertion(actualval > lowerbound
        && actualval < upperbound,"lowerbound "+lowerbound+", upperbound "+upperbound+",actual value "+actualval);

    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        this.attrib = (AttributesTestServiceTimeProcessor) this.getAttributes();
    }

}
