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

public enum MaskType {
    NO_MASK(0), SURGICAL_MASK(1), FFP2(1.5);
    private final double value;

    MaskType(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
