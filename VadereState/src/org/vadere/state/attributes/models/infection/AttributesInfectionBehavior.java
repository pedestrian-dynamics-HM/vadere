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
 * Hesham Hussen
 * HAW Hamburg, Germany
 *
 * This software is licensed under the GNU Lesser General Public License (LGPL).
 */

package org.vadere.state.attributes.models.infection;

import lombok.Getter;
import org.vadere.state.attributes.Attributes;

public class AttributesInfectionBehavior extends Attributes {
    @Getter private float minInfectionDistance = 0f;
    @Getter private int immunityDurationInSteps = 20;
    @Getter private int durationToTransmitInSteps = 14;
    @Getter private int recoveryDurationInSteps = 14;
    // TODO!! do we need it ?
    // @Getter private int infectionNumberPerStep = 0;


    private double infectionPercentage = 0;
    public double getInfectionPercentage() {
        return infectionPercentage  / 100;
    }
}
