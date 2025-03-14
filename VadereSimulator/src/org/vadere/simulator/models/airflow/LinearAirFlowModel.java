package org.vadere.simulator.models.airflow;


import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesLinearAirFlowModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.logging.Logger;

import java.util.List;
import java.util.Random;

@ModelClass
public class LinearAirFlowModel extends AbstractAirFlowModel {

    private static final Logger logger = Logger.getLogger(LinearAirFlowModel.class);

    protected AttributesLinearAirFlowModel attributesLinearAirFlowModel;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        super.initialize(attributesList, domain, attributesPedestrian, random);
        this.attributesLinearAirFlowModel = Model.findAttributes(attributesList, AttributesLinearAirFlowModel.class);
    }

    @Override
    public void setupAirFlow() {
        double xVelocity = Math.cos(attributesLinearAirFlowModel.getWindDirection()) * attributesLinearAirFlowModel.getWindSpeed();
        double yVelocity = Math.sin(attributesLinearAirFlowModel.getWindDirection()) * attributesLinearAirFlowModel.getWindSpeed();
        airFlow.setX_velocity(new double[][]{{xVelocity}});
        airFlow.setY_velocity(new double[][]{{yVelocity}});
        airFlow.setGridSize(Double.POSITIVE_INFINITY);
    }
}
