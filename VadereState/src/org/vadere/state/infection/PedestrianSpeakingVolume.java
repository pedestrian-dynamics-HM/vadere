/**
 * Author: Mina Abadeer
 * Group Parallel and Distributed Systems
 * Department of Computer Science
 * University of Muenster, Germany
 *
 * Co-author(s):
 * Sameh Magharious
 * Dell Technologies, USA
 *
 * This software is licensed under the GNU Lesser General Public License (LGPL).
 */

package org.vadere.state.infection;

public enum PedestrianSpeakingVolume {
    SILENT(1.5), WHISPERING(1.1), TALKING(1), LOUD(0.5), YELLING(0.3);

    private final double value;
    PedestrianSpeakingVolume(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
