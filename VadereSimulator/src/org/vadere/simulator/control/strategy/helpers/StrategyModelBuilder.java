package org.vadere.simulator.control.strategy.helpers;


import org.vadere.simulator.control.strategy.models.IStrategyModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.util.reflection.DynamicClassInstantiator;

public class StrategyModelBuilder {

    public static final String JAVA_PACKAGE_SEPARATOR = ".";

    public static IStrategyModel instantiateModel(ScenarioStore scenarioStore) {
        String simpleClassName = scenarioStore.getStrategyModel();
        if (simpleClassName != null) {
            String classSearchPath = "org.vadere.simulator.models.strategy";
            String fullyQualifiedClassName = classSearchPath + JAVA_PACKAGE_SEPARATOR + simpleClassName.replaceAll("\"","");

            DynamicClassInstantiator<IStrategyModel> instantiator = new DynamicClassInstantiator<>();
            IStrategyModel strategyModel = instantiator.createObject(fullyQualifiedClassName);

            return strategyModel;
        }
        else{
            return null;
        }
    }
}
