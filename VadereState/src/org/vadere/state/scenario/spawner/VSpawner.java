package org.vadere.state.scenario.spawner;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.AttributesAttached;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;

import java.util.Random;
import java.util.function.Supplier;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegularSpawner.class, name = "org.vadere.state.scenario.spawner.impl.RegularSpawner")
})
public abstract class VSpawner<T extends AttributesSpawner>  extends AttributesAttached<T> {
    protected int dynamicElementsCreatedTotal = 0;
    private final VDistribution<?> distribution;

    public VSpawner(T attributes, Random random) {
        this.attributes= attributes;
        try {
            this.distribution = DistributionFactory.create(attributes.getDistributionAttributes(), new JDKRandomGenerator(random.nextInt()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes the number of elements that should be spawned or generated at a given simulation time.
     * <p>
     * This abstract method must be implemented by subclasses to determine how many elements
     * are expected to be created at the specified simulation time.
     * </p>
     *
     * @param timeCurrentEvent The current time of the event in seconds within the simulation.
     * @return The number of elements to be spawned at the given simulation time.
     */
    public abstract int getEventElementCount(double timeCurrentEvent);

    /**
     * Retrieves the number of agents or elements that are yet to be spawned in the simulation or spawning process.
     * <p>
     * This abstract method must be implemented by subclasses to provide the remaining count of agents or elements
     * that are scheduled or allowed to be spawned based on the specific rules or constraints of the subclass.
     * </p>
     *
     * @return The number of agents or elements that are still to be spawned.
     */
    public abstract int getRemainingSpawnAgents();

    /**
     * Sets the number of agents or elements that are yet to be spawned in the simulation or spawning process.
     * <p>
     * This abstract method must be implemented by subclasses to define how to update the remaining count
     * of agents or elements that are scheduled or allowed to be spawned.
     * </p>
     *
     * @param remainingAgents The number of agents or elements that should be set as remaining to be spawned.
     */
    public abstract void setRemainingSpawnAgents(int remainingAgents);

    /**
     * Determines if the spawning or simulation process is complete based on the current simulation time,
     * the maximum number of elements spawned, and whether a specified queue is empty.
     * <p>
     * The method checks three conditions to determine if the process is finished:
     * <ul>
     *   <li>If the maximum number of spawned elements has been reached, the process is considered finished.</li>
     *   <li>If the spawner is configured to perform only a single spawn event, it checks if the number of dynamically created
     *       elements matches the expected count at the given simulation time.</li>
     *   <li>If the current simulation time is after the spawner's end time and the queue is empty, the process is considered finished.</li>
     * </ul>
     * </p>
     *
     * @param simTimeInSec The current simulation time in seconds.
     * @param isQueueEmpty A {@link Supplier} that returns a {@code boolean} indicating whether the relevant queue is empty.
     * @return {@code true} if the spawning or simulation process is finished; {@code false} otherwise.
     */
    public boolean isFinished(double simTimeInSec, Supplier<Boolean> isQueueEmpty) {
        if (isMaximumNumberOfSpawnedElementsReached()) {
            return true;
        }
        if (isSpawnerWithOneSingleSpawnEvent()) {
            return dynamicElementsCreatedTotal == getEventElementCount(simTimeInSec);
        }
        return isAfterSpawnerEndTime(simTimeInSec) && isQueueEmpty.get();
    }

    /**
     * Checks whether the maximum number of spawned elements has been reached.
     * <p>
     * This method retrieves the maximum allowable number of spawned elements from the spawner's attributes.
     * If there is no limit defined (indicated by {@code AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL}),
     * the method returns {@code false}. Otherwise, it checks if the total number of dynamically created
     * elements has reached or exceeded this maximum value.
     * </p>
     *
     * @return {@code true} if the maximum number of spawned elements is reached; {@code false} otherwise.
     */
    public boolean isMaximumNumberOfSpawnedElementsReached() {
        final int maxNumber = attributes.getConstraintsElementsMax();
        return maxNumber != AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL
                && dynamicElementsCreatedTotal >= maxNumber;
    }

    /**
     * Determines if the current simulation time is after the configured spawner end time.
     * <p>
     * This method checks whether the provided simulation time in seconds exceeds the end time
     * constraint defined in the spawner's attributes. The end time constraint represents the
     * point in time after which no more elements should be spawned.
     * </p>
     *
     * @param simTimeInSec The current simulation time in seconds.
     * @return {@code true} if the current simulation time is after the spawner's end time; {@code false} otherwise.
     */
    public boolean isAfterSpawnerEndTime(double simTimeInSec) {
        return simTimeInSec > attributes.getConstraintsTimeEnd();
    }

    public boolean isSpawnerWithOneSingleSpawnEvent() {
        return attributes.getConstraintsTimeEnd().equals(attributes.getConstraintsTimeStart());
    }


    @Override
    public T getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(T attributes) {
        super.setAttributes(attributes);
    }

    /**
     * Calculates the next spawn time based on the current event time and a specified (probability) distribution.
     * <p>
     * This method uses a distribution object to generate the next sample time for spawning an event.
     * It relies on the current event time to determine the timing of the next spawn.
     * </p>
     *
     * @param timeCurrentEvent The time of the current event in seconds.
     * @return The next spawn time in seconds, as determined by the distribution.
     */
    public double getNextSpawnTime(double timeCurrentEvent) {
        return distribution.getNextSample(timeCurrentEvent);
    }

    public int getDynamicElementsCreatedTotal() {
        return dynamicElementsCreatedTotal;
    }

    /**
     * Increments the total number of dynamically created elements by a specified count.
     * <p>
     * This method updates the {@code dynamicElementsCreatedTotal} by adding the given count to it.
     * It is used to keep track of the total number of elements spawned dynamically
     * during the simulation.
     * </p>
     *
     * @param count The number of elements to add to the total count of dynamically created elements.
     */
    public void incrementElementsCreatedTotal(int count) {
        dynamicElementsCreatedTotal += count;
    }

    public VDistribution<?> getDistribution() {
        return distribution;
    }
}
