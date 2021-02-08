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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes the state of protective measures that an individual pedestrian carry in relation to infection simulation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedestrianProtectiveMeasures {
    private MaskType maskType;
    private PedestrianSpeakingVolume pedestrianSpeakingVolume;
}
