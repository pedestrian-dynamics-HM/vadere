package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerManager;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesInfectionModel;
import org.vadere.state.attributes.models.InfectionModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.*;

import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.vadere.state.attributes.Attributes.ID_NOT_SET;

@ModelClass
public class InfectionModel extends AbstractSirModel {


	// keep attributes here and not in AbstractSirModel becase the may change based on
	// implementation (AttributesInfectionModel is the base class for all SIR models used here for simplicity)
	private AttributesInfectionModel attributesInfectionModel;

	private int counter;

	private double nextAerosolCloudUpdateTime;
	private double startBreatheInUpdateTime;
	private double endBreatheInUpdateTime;
	private double lastSimTimeInSec;

	private ControllerManager controllerManager;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributesInfectionModel = Model.findAttributes(attributesList, AttributesInfectionModel.class);
			this.counter = 0;
			this.nextAerosolCloudUpdateTime = 0;
			this.startBreatheInUpdateTime = 0;
			this.endBreatheInUpdateTime = this.startBreatheInUpdateTime + this.attributesInfectionModel.getInfectionModelUpdateStepLength() / 2.0;
			this.lastSimTimeInSec = 0;
	}

	@Override
	public void registerToScenarioElementControllerEvents(ControllerManager controllerManager) {
		this.controllerManager = controllerManager; // ToDo: controllerManager should be handled by initialize method (this requires changes in all models)
		for (var controller : controllerManager.getSourceControllers()){
			controller.register(this::sourceControllerEvent);
		}
	}

	@Override
	public void preLoop(double simTimeInSec) { logger.infof(">>>>>>>>>>>InfectionModelModel preLoop %f", simTimeInSec); }

	@Override
	public void postLoop(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>InfectionModelModel postLoop %f", simTimeInSec);
	}

	@Override
	public void update(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>InfectionModelModel update  %f", simTimeInSec);

		if (simTimeInSec >= nextAerosolCloudUpdateTime) {
			nextAerosolCloudUpdateTime = nextAerosolCloudUpdateTime + this.attributesInfectionModel.getInfectionModelUpdateStepLength() / 2.0;

			Collection<Pedestrian> infectedPedestrians = this.domain.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
					.collect(Collectors.toSet());

			for (Pedestrian ped : infectedPedestrians) {
				if (ped.getStartBreatheOutPosition() == null) {
					// step 1: store position when pedestrian starts breathing out -> v1
					ped.setStartBreatheOutPosition(ped.getPosition());
				} else {
					// step 2: get position when pedestrian stops breathing out -> v2
					// create ellipse with vertices v1 and v2
					VPoint v1 = ped.getStartBreatheOutPosition();
					ped.setStartBreatheOutPosition(null); // reset startBreatheOutPosition
					VPoint v2 = ped.getPosition();

					double area = Math.pow(attributesInfectionModel.getAerosolCloudInitialRadius(), 2) * Math.PI;
					VShape shape = createTransformedShape(v1, v2, area);

					// assumption: aerosolCloud has a constant vertical extent (in m). The height corresponds to a
					// cylinder whose volume equals the
					// - sphere with radius = initialAerosolCloudRadius
					// - ellipsoid with principal diameters a, b, c where cross-sectional
					// area (in the x-y-plane) = a * b * PI and c = initialAerosolCloudRadius
					double height = 4 / 3 * attributesInfectionModel.getAerosolCloudInitialRadius();
					double volume = area * height;

					// assumption: only a part of the emitted pathogen remains in the x-y-plane
					// (at z ~ height of the pedestrians' faces) due to effects such as
					// declining "pathogen activity", evaporation, gravitation/sedimentation.
					// These effects actually play a role over time -> remainingPathogenFraction or pathogenDensity2D
					// should be updated over time (see AerosolCloudController)
					double remainingPathogenFraction = 1;

					AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET,
							shape,
							simTimeInSec,
							ped.emitPathogen() / volume * remainingPathogenFraction,
							attributesInfectionModel.getAerosolCloudLifeTime(),
							false));
					this.controllerManager.registerAerosolCloud(aerosolCloud);
				}
			}
		}


		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption must be adapted with normalizationFactor:
		// Agents breathe in for time interval [startBreatheInUpdateTime, endBreatheInUpdateTime]
		double deltaTime = (simTimeInSec - lastSimTimeInSec) / (this.attributesInfectionModel.getInfectionModelUpdateStepLength() / 2.0);
		lastSimTimeInSec = simTimeInSec;

		if (simTimeInSec > endBreatheInUpdateTime) {
			startBreatheInUpdateTime = startBreatheInUpdateTime + this.attributesInfectionModel.getInfectionModelUpdateStepLength();
			endBreatheInUpdateTime = startBreatheInUpdateTime + this.attributesInfectionModel.getInfectionModelUpdateStepLength() / 2.0;
		}
		if ((simTimeInSec >= startBreatheInUpdateTime) & (simTimeInSec <= endBreatheInUpdateTime) & (deltaTime > 0)) {
			Collection<AerosolCloud> updatedAerosolClouds = this.domain.getTopography().getAerosolClouds();
			for (AerosolCloud aerosolCloud : updatedAerosolClouds) {
				Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(aerosolCloud);
				for (Pedestrian pedestrian : pedestriansInsideCloud) {
					updatePedestrianPathogenAbsorbedLoad(pedestrian, aerosolCloud.getPathogenDensity() * deltaTime);
				}
			}
		}

		// update pedestrian infection statuses
		Collection<Pedestrian> allPedestrians = this.domain.getTopography().getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			updatePedestrianInfectionStatus(pedestrian, simTimeInSec);
		}
	}

	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		// SourceControllerListener. This will be called  *after* a pedestrians is inserted into the
		// topography by the given SourceController. Change model state on Agent here
		InfectionModelSourceParameters sourceParameters = defineSourceParameters(controller);

		Pedestrian ped = (Pedestrian) scenarioElement;
		ped.setInfectionStatus(sourceParameters.getInfectionStatus());
		ped.setPathogenEmissionCapacity(attributesInfectionModel.getPedestrianPathogenEmissionCapacity());
		ped.setPathogenAbsorptionRate(attributesInfectionModel.getPedestrianPathogenAbsorptionRate());
		ped.setSusceptibility(attributesInfectionModel.getPedestrianSusceptibility());
		ped.setExposedPeriod(attributesInfectionModel.getExposedPeriod());
		ped.setInfectiousPeriod(attributesInfectionModel.getInfectiousPeriod());
		ped.setRecoveredPeriod(attributesInfectionModel.getRecoveredPeriod());

		logger.infof(">>>>>>>>>>>sourceControllerEvent at time: %f  agentId: %d", simTimeInSec, scenarioElement.getId());
		return ped;
	}

	private InfectionModelSourceParameters defineSourceParameters(SourceController controller) {
		int sourceId = controller.getSourceId();
		int defaultSourceId = -1;
		Optional<InfectionModelSourceParameters> sourceParameters = getAttributesInfectionModel()
				.getInfectionModelSourceParameters().stream().filter(s -> s.getSourceId() == sourceId).findFirst();

		// if sourceId not set by user, check if the user has defined default attributes by setting sourceId = -1
		if (sourceParameters.isEmpty()) {
			sourceParameters = getAttributesInfectionModel().getInfectionModelSourceParameters().stream().filter(s -> s.getSourceId() == defaultSourceId).findFirst();

			// if no user defined default values: use attributesInfectionModel default values
			if (sourceParameters.isPresent()) {
				logger.infof(">>>>>>>>>>>defineSourceParameters: sourceId %d not set explicitly in infectionModelSourceParameters. Source uses default infectionModelSourceParameters defined for sourceId: %d", sourceId, defaultSourceId);
			} else {
				logger.errorf(">>>>>>>>>>>defineSourceParameters: sourceId %d is not set in infectionModelSourceParameters", sourceId);
			}
		}
			return sourceParameters.get();
	}

	public AttributesInfectionModel getAttributesInfectionModel() {
		return attributesInfectionModel;
	}

	public Collection<Pedestrian> getDynamicElementsNearAerosolCloud(AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth());

		return this.domain.getTopography().getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);
	}

	public boolean isPedestrianInAerosolCloud(AerosolCloud aerosolCloud, Pedestrian pedestrian) {
		VShape aerosolCloudShape = aerosolCloud.getShape();
		VPoint pedestrianPosition = pedestrian.getPosition();
		return aerosolCloudShape.contains(pedestrianPosition);
	}

	public Collection<Pedestrian> getPedestriansInsideAerosolCloud(AerosolCloud aerosolCloud) {
		Collection<Pedestrian> pedestriansInsideAerosolCloud = new LinkedList<>();

		Collection<Pedestrian> pedestriansNearAerosolCloud = getDynamicElementsNearAerosolCloud(aerosolCloud);
		for (Pedestrian pedestrian : pedestriansNearAerosolCloud) {
			if (isPedestrianInAerosolCloud(aerosolCloud, pedestrian)){
				pedestriansInsideAerosolCloud.add(pedestrian);
			}
		}
		return pedestriansInsideAerosolCloud;
	}

	public VShape createTransformedShape(VPoint vertex1, VPoint vertex2, double area) {
		VPoint centerVertex = new VPoint((vertex1.x + vertex2.x) / 2.0, (vertex1.y + vertex2.y) / 2.0);
		double majorAxis = vertex1.distance(vertex2);
		double minorAxis = 4 * area / (majorAxis * Math.PI);

		// ellipse parameters
		double a = majorAxis / 2.0;
		double b = minorAxis / 2.0;
		double c = Math.sqrt(a * a - b * b);
		double e = c / a; // eccentricity
		VShape shape;

		if (majorAxis < minorAxis) {
 			// return ellipse with (a'=b') -> circle
 			shape = new VCircle(new VPoint(centerVertex.getX(), centerVertex.getY()), Math.sqrt(area / Math.PI));
 		} else {
			// return polygon (approximated ellipse with edges)
			Path2D path = new Path2D.Double();
			path.moveTo(a, 0); // define stating point
			for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 50.0) {
				double radius = b / Math.sqrt(1 - Math.pow(e * Math.cos(angle), 2)); // radius(angle) from ellipse center to its bound
				path.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius); // convert polar to cartesian coordinates
			}
			path.closePath();
			VShape polygon = new VPolygon(path);
			double theta = Math.atan2(vertex2.y - vertex1.y, vertex2.x - vertex1.x); // get orientation of shape
			AffineTransform transform = new AffineTransform();
			transform.translate(centerVertex.getX(), centerVertex.getY());
			transform.rotate(theta);

			// shape = new VPolygon(transform.createTransformedShape(shape1));
			shape = new VPolygon(transform.createTransformedShape(polygon));
		}
		return shape;
	}
}
