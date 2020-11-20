package org.vadere.simulator.models.bhm.helpers.navigation;

import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.Random;

/**
 * This class encapsulates the creation of a concrete {@link INavigation}
 * which is defined by the user in the JSON scenario description.
 *
 * The user provides the simple class name in the JSON scenario file.
 * I.e., no fully qualified classname.
 */
public class NavigationBuilder {

	public static final String JAVA_PACKAGE_SEPARATOR = ".";

	public static INavigation instantiateModel(String simpleClassName, PedestrianBHM pedestrianBHM, Topography topography, Random random) {
		String classSearchPath = INavigation.class.getPackageName();
		String fullyQualifiedClassName = classSearchPath + JAVA_PACKAGE_SEPARATOR + simpleClassName;

		DynamicClassInstantiator<INavigation> instantiator = new DynamicClassInstantiator<>();
		INavigation navigationModel = instantiator.createObject(fullyQualifiedClassName);

		navigationModel.initialize(pedestrianBHM, topography, random);

		return navigationModel;
	}

}
