package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.EventtimePedestrianIdOutputFile;

/**
 * This key consists of the pedestrian id and the event time ("eventTime") which is the *exact* time at which an
 * event occured (which can be any positive real value). In between simulation time steps (see parameter
 * "simTimeStepLength" (currently default at 0.4) there can be multiple events. The event time therefore allows to
 * have more (accurate) data (all the events in between simulation time steps).
 */

@OutputFileMap(outputFileClass = EventtimePedestrianIdOutputFile.class)
public class EventtimePedestrianIdKey implements DataKey<EventtimePedestrianIdKey> {
    private final double simTime;
    private final int pedestrianId;

    public EventtimePedestrianIdKey(double simTime, int pedestrianId) {
        this.simTime = simTime;
        this.pedestrianId = pedestrianId;
    }

    public Double getSimtime() {
        return simTime;
    }

    public Integer getPedestrianId() {
        return pedestrianId;
    }

    @Override
    public int compareTo(EventtimePedestrianIdKey o) {
        int result = Double.compare(simTime, o.simTime);
        if (result == 0) {
            return Integer.compare(pedestrianId, o.pedestrianId);
        }
        return result;
    }

    public static String[] getHeaders() {
        // TODO: if there are more keys using simTime then there should be a separate key.
        return new String[] { PedestrianIdKey.getHeader(), "simTime"};
    }

    @Override
    public String toString() {
        return "EventtimePedestrianIdKey{" +
                "simTime=" + this.simTime +
                ", pedestrianId=" + pedestrianId +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + pedestrianId;
        result = prime * result + (int) (simTime *100);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EventtimePedestrianIdKey other = (EventtimePedestrianIdKey) obj;
        if (pedestrianId != other.pedestrianId)
            return false;
        if (simTime != other.simTime)
            return false;
        return true;
    }
}
