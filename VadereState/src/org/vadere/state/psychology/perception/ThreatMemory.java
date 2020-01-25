package org.vadere.state.psychology.perception;

import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ThreatMemory} is required to avoid following situation during simulation:
 *
 * <ol>
 *     <li>Cognition layer detects a {@link Threat}.</li>
 *     <li>In locomotion layer, a {@link Pedestrian} has no time credit to react to the {@link Threat}.</li>
 *     <li>In next simulation loop, the {@link Threat} is already gone.
 *     I.e., the {@link Threat} from previous simulation loop would be lost.</li>
 * </ol>
 *
 * Therefore, provide a {@link ThreatMemory} with a boolean flag included which
 * indicates if latest {@link Threat} was already handled by the locomotion layer.
 *
 * Note: Maybe, this memory could also be implemented as a stack. I.e., cognition layer
 * pushes {@link Threat}s to the stack while the locomotion layer pops off the {@link Threat}s.
 * But, this is ot a real "memory"!
 */
public class ThreatMemory {

    // Member Variables
    List<Threat> allThreats;
    boolean latestThreatUnhandled;

    // Constructors
    public ThreatMemory() {
        this.allThreats = new ArrayList<>();
        this.latestThreatUnhandled = false;
    }

    public ThreatMemory(ThreatMemory other) {
        this.allThreats = new ArrayList<>();

        if (other.getAllThreats() != null) {
            other.getAllThreats().stream().forEach(threat -> this.allThreats.add(threat.clone()));
        }

        this.latestThreatUnhandled = other.isLatestThreatUnhandled();
    }

    // Getter
    public List<Threat> getAllThreats() { return allThreats; }
    public boolean isLatestThreatUnhandled() { return latestThreatUnhandled; }

    // Setter
    public void setLatestThreatUnhandled(boolean latestThreatUnhandled) {
        this.latestThreatUnhandled = latestThreatUnhandled;
    }

    // Methods
    public void add(Threat threat) {
        allThreats.add(threat);
    }

    public void clear() {
        allThreats.clear();
    }

    public boolean isEmpty() {
        return allThreats.size() == 0;
    }

    public Threat getLatestThreat() {
        int totalThreats = allThreats.size();
        Threat latestThreat = (totalThreats == 0)? null : allThreats.get(totalThreats - 1);

        return latestThreat;
    }

    public ThreatMemory clone() {
        return new ThreatMemory(this);
    }

}
