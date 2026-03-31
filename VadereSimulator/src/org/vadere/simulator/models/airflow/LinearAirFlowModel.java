package org.vadere.simulator.models.airflow;


import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesLinearAirFlowModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Random;

@ModelClass
public class LinearAirFlowModel extends AbstractAirFlowModel {

    private static final Logger logger = Logger.getLogger(LinearAirFlowModel.class);

    protected AttributesLinearAirFlowModel attributesLinearAirFlowModel;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        String scenarioPath = VadereContext.getCtx(domain.getTopography()).getString("scenarioPath");
        this.attributesLinearAirFlowModel = Model.findAttributes(attributesList, AttributesLinearAirFlowModel.class);
        Rectangle2D.Double contentRect = domain.getTopography().getContentRect();
        double xmin = contentRect.getMinX();
        double ymin = contentRect.getMinY();
        double xmax = contentRect.getMaxX();
        double ymax = contentRect.getMaxY();
        this.airFlow = new AirFlow(scenarioPath, "", xmin, ymin, xmax, ymax);
        domain.getTopography().setAirFlow(airFlow);

        // Initialize airFlow with null velocities
        airFlow.setX_velocity(null);
        airFlow.setY_velocity(null);
    }

    @Override
    public void setupAirFlow() {
        double xVelocity = Math.cos(attributesLinearAirFlowModel.getAirflowDirection()) * attributesLinearAirFlowModel.getAirflowSpeed();
        double yVelocity = Math.sin(attributesLinearAirFlowModel.getAirflowDirection()) * attributesLinearAirFlowModel.getAirflowSpeed();
 
        // Set velocity only in the center cell (1,1) because borders are not used anyway in getFlowDirection (requires 3x3 arrays)
        double[][] xVelocities = new double[3][3]; 
        double[][] yVelocities = new double[3][3]; 
        
        xVelocities[1][1] = xVelocity;
        yVelocities[1][1] = yVelocity;
        
        airFlow.setX_velocity(xVelocities);
        airFlow.setY_velocity(yVelocities);
        airFlow.setRectangularGridCellSize(Double.POSITIVE_INFINITY);
    }
}
