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

package org.vadere.simulator.control.behavior;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.List;

public interface Behavior<T> {
    void apply(final Collection<T> elements, final Topography scenario);
    void initialize(final List<Attributes> modelAttributesList, final Topography scenario);
}
