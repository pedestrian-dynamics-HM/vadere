package org.vadere.simulator.control.external.models;

import org.vadere.simulator.control.external.reaction.InformationFilterSettings;
import org.vadere.simulator.control.psychology.cognition.models.ICognitionModel;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.state.scenario.Topography;
import org.vadere.util.reflection.DynamicClassInstantiator;

/**
 * This class encapsulates the creation of a concrete {@link ControlModel}
 * defined and updated by an external control unit handled by TraCI.
 *
 * The user provides the simple class name, no fully qualified classname.
 */


public class ControlModelBuilder {

    public static final String MODEL_CONTAINER = ".";


    public static IControlModel getModel(String ClassName, Topography topography, StimulusController stimulusController, double simTimeStepLength, InformationFilterSettings staticInformationFilterSettings){

        String classSearchPath = ControlModel.class.getPackageName();
        String fullyQualifiedClassName = classSearchPath + MODEL_CONTAINER + ClassName;

        DynamicClassInstantiator<ICognitionModel> instantiator = new DynamicClassInstantiator<>();
        IControlModel controlModel = (IControlModel) instantiator.createObject(fullyQualifiedClassName);

        controlModel.init(topography, stimulusController, simTimeStepLength, staticInformationFilterSettings);
        return controlModel;

    }



}