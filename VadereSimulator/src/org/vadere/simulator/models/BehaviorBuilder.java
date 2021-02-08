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

package org.vadere.simulator.models;

import org.vadere.simulator.control.behavior.Behavior;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to build behaviors for the main model
 */
public class BehaviorBuilder {

	public static List<Behavior> buildBehaviors(List<String> behaviorsClassNames, final List<Attributes> modelAttributesList, Topography topography) {
		var result = new ArrayList<Behavior>();
		for (String behaviorClassName : behaviorsClassNames) {
			final DynamicClassInstantiator<Behavior> behaviorInstantiator = new DynamicClassInstantiator<>();
			Behavior behavior = behaviorInstantiator.createObject(behaviorClassName);
			behavior.initialize(modelAttributesList, topography );
			result.add(behavior);
		}

		return result;
	}
}
