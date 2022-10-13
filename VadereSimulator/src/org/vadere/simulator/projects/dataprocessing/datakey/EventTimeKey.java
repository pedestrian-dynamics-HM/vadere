package org.vadere.simulator.projects.dataprocessing.datakey;
import org.vadere.simulator.projects.dataprocessing.outputfile.EventTimeOutputFile;

/**
 * This key consists of the event time ("eventTime") which is the *exact* time at which an
 * event occured (which can be any positive real value). In between simulation time steps (see parameter
 * "simTimeStepLength" (currently default at 0.4) there can be multiple events. The event time therefore allows to
 * have more (accurate) data (all the events in between simulation time steps).
 */
@OutputFileMap(outputFileClass = EventTimeOutputFile.class)
public class EventTimeKey implements DataKey<EventTimeKey> {
    private final double simTime;

    public EventTimeKey(double simTime) {
        this.simTime = simTime;

    }

    public Double getSimTime() {
        return simTime;
    }



    @Override
    public int compareTo(EventTimeKey o) {
        return Double.compare(simTime, o.simTime);
    }

    public static String[] getHeaders() {
        // TODO: if there are more keys using simTime then there should be a separate key.
        return new String[] { "simTime"};
    }

    @Override
    public String toString() {
        return "EventTimePedestrianIdKey{" +
                "simTime=" + this.simTime +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)simTime;
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
        EventTimeKey other = (EventTimeKey) obj;
        return simTime == other.simTime;
    }
}
