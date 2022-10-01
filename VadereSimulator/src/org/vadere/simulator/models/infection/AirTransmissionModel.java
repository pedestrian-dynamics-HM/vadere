package org.vadere.simulator.models.infection;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel;
import org.vadere.state.attributes.models.infection.AttributesExposureModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.health.AirTransmissionModelHealthStatus;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AirTransmissionModel describes the transmission of pathogen from one
 * <code>Pedestrian</code> to another via <code>ParticleDispersion</code> that
 * move through the air.
 * <p>
 *     This particle dispersion can either be described as {@link AerosolCloud
 *     AerosolClouds}, which are carried by air for a longer period, or by {@link
 *     Droplets}, which remain in the air only for short.
 *     Whether aerosol clouds and/or droplets are considered, is defined in
 *     {@link AttributesAirTransmissionModel}.
 * </p>
 * <p> <code>AirTransmissionModel</code> contains the logic, that is:
 * <ul>
 *     <li>Each pedestrian obtains a {@link AirTransmissionModelHealthStatus health
 *     status} after being inserted into the topography.</li>
 *     <li>Infectious pedestrians emit pathogen contained in aerosol
 *     clouds or droplets.</li>
 *     <li>Pedestrians health status, aerosol clouds, droplets</li>
 *     <li>The <code>AirTransmissionModel</code> deletes aerosol clouds and
 *     droplets once they have reached a minimum pathogen concentration.</li>
 * </ul>
 */
@ModelClass
public class AirTransmissionModel extends AbstractExposureModel {

	protected static Logger logger = Logger.getLogger(AirTransmissionModel.class);

	protected AttributesAirTransmissionModel attrAirTransmissionModel;
	protected double simTimeStepLength;
	Topography topography;
	int aerosolCloudIdCounter;

	private Map<Integer, VPoint> lastPedestrianPositions;
	private Map<Integer, Vector2D> viewingDirections;
	private Map<Integer, Double> nextDropletsExhalationTime;
	protected static final double MIN_PED_STEP_LENGTH = 0.1;

	/**
	 * Key that is used for initializeVadereContext in ScenarioRun
	 */
	public static final String simStepLength = "simTimeStepLength";

	/**
	 * constant that results from exponential decay of pathogen concentration: C(t) = C_init * exp(-lambda * t),
	 * lambda = exponentialDecayFactor / halfLife
	 */
	private static final double exponentialDecayFactor = Math.log(2.0);

	/**
	 * Defines a percentage of the initial pathogen concentration
	 * (pathogenLoad / aerosolCloud.volume); As soon as an aerosolCloud has reached the minimum concentration, the
	 * aerosolCloud is considered negligible and therefore deleted
	 */
	protected static final double minimumPercentage = 0.01;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
		this.domain = domain;
		this.random = random;
		this.attributesAgent = attributesPedestrian;
		this.attrAirTransmissionModel = Model.findAttributes(attributesList, AttributesAirTransmissionModel.class);
		this.topography = domain.getTopography();
		this.simTimeStepLength = VadereContext.getCtx(this.topography).getDouble(simStepLength);
		this.aerosolCloudIdCounter = 1;
		this.viewingDirections = new HashMap<>();
		this.lastPedestrianPositions = new HashMap<>();
		this.nextDropletsExhalationTime = new HashMap<>();
	}

	@Override
	public void preLoop(double simTimeInSec) {}

	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void update(double simTimeInSec) {

		if (attrAirTransmissionModel.isAerosolCloudsActive()) {
			executeAerosolCloudEmissionEvents(simTimeInSec);
			updateAerosolClouds(simTimeInSec);
			updatePedestriansExposureToAerosolClouds();
		}

		if (attrAirTransmissionModel.isDropletsActive()) {
			executeDropletEmissionEvents(simTimeInSec);
			updateDroplets(simTimeInSec);
			updatePedestriansExposureToDroplets();
		}

		if (attrAirTransmissionModel.isAerosolCloudsActive() || attrAirTransmissionModel.isDropletsActive()) {
			updatePedestriansHealthStatus(simTimeInSec);
		}
	}

	@Override
	public void updatePedestrianDegreeOfExposure(Pedestrian pedestrian, double deltaDegreeOfExposure) {
		pedestrian.incrementDegreeOfExposure(deltaDegreeOfExposure);
	}

	public void executeAerosolCloudEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectiousPedestrians = getInfectiousPedestrians(topography);
		for (Pedestrian pedestrian : infectiousPedestrians) {
			createAerosolClouds(simTimeInSec, pedestrian);
		}
	}

	public void executeDropletEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectiousPedestrians = getInfectiousPedestrians(topography);
		for (Pedestrian pedestrian : infectiousPedestrians) {
			createDroplets(simTimeInSec, pedestrian);
		}
	}

	public void updateAerosolClouds(double simTimeInSec) {
		updateAerosolCloudsPathogenLoad(simTimeInSec);
		updateAerosolCloudsExtent();
		deleteExpiredAerosolClouds();
	}

	public void updateDroplets(double simTimeInSec) {
		// dropletsPathogenLoad remains unchanged until deletion
		deleteExpiredDroplets(simTimeInSec);
	}

	public void createAerosolClouds(double simTimeInSec, Pedestrian pedestrian) {

		if (pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().isStartingExhalation()) {
			pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().setExhalationStartPosition(pedestrian.getPosition());

		} else if (pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().isStartingInhalation()) {
			VPoint startBreatheOutPosition = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().getExhalationStartPosition();
			VPoint stopBreatheOutPosition = pedestrian.getPosition();
			VLine distanceWalkedDuringExhalation = new VLine(startBreatheOutPosition, stopBreatheOutPosition);

			AerosolCloud aerosolCloud = generateAerosolCloud(simTimeInSec, distanceWalkedDuringExhalation);
			topography.addAerosolCloud(aerosolCloud);

			pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().resetStartExhalationPosition();
		}
	}

	private AerosolCloud generateAerosolCloud(double simTimeInSec, VLine distanceWalkedDuringExhalation) {
		VPoint center = distanceWalkedDuringExhalation.midPoint();

		AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(aerosolCloudIdCounter,
				attrAirTransmissionModel.getAerosolCloudInitialRadius(),
				center,
				simTimeInSec,
				attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad()));

		aerosolCloudIdCounter = aerosolCloudIdCounter + 1;

		return aerosolCloud;
	}

	private void createDroplets(double simTimeInSec, Pedestrian pedestrian) {
		int pedestrianId = pedestrian.getId();
		Vector2D viewingDirection;
		VPoint currentPosition = pedestrian.getPosition();
		VPoint lastPosition = lastPedestrianPositions.get(pedestrianId);
		if (lastPedestrianPositions.get(pedestrianId) == null) {
			viewingDirection = new Vector2D(random.nextDouble(), random.nextDouble());
		} else {
			if (lastPosition.distance(currentPosition) < MIN_PED_STEP_LENGTH) {
				viewingDirection = viewingDirections.get(pedestrianId);
			} else {
				viewingDirection = new Vector2D(currentPosition.getX() - lastPosition.getX(),
						currentPosition.getY() - lastPosition.getY());
			}
		}
		viewingDirection.normalize(1);
		viewingDirections.put(pedestrianId, viewingDirection);
		lastPedestrianPositions.put(pedestrianId, currentPosition);

		// period between two droplet generating respiratory events
		double dropletExhalationPeriod = 1 / attrAirTransmissionModel.getDropletsEmissionFrequency();

		if (nextDropletsExhalationTime.get(pedestrianId) == null) {
			nextDropletsExhalationTime.put(pedestrianId, simTimeInSec + dropletExhalationPeriod);
		} else if (simTimeInSec >= nextDropletsExhalationTime.get(pedestrianId) && !pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().isBreathingIn()) {
			Droplets droplets = new Droplets(new AttributesDroplets(1,
					simTimeInSec,
					attrAirTransmissionModel.getDropletsPathogenLoad(),
					pedestrian.getPosition(),
					viewingDirection,
					attrAirTransmissionModel.getDropletsDistanceOfSpread(),
					attrAirTransmissionModel.getDropletsAngleOfSpreadInDeg()));

			topography.addDroplets(droplets);

			nextDropletsExhalationTime.put(pedestrianId, simTimeInSec + dropletExhalationPeriod);
		}
	}

	//TODO define recursive; then, if possible, remove property initialPathogenLoad from AerosolCloud
	public void updateAerosolCloudsPathogenLoad(double simTimeInSec) {
		double lambda = exponentialDecayFactor / attrAirTransmissionModel.getAerosolCloudHalfLife();

		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			double t = simTimeInSec - aerosolCloud.getCreationTime();
			aerosolCloud.setCurrentPathogenLoad(attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad() * Math.exp(-lambda * t));
		}
	}

	public void updateAerosolCloudsExtent() {
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			double deltaRadius = 0.0;

			/*
			 * Increasing extent due to dispersion, multiplication with simTimeStepLength keeps deltaRadius independent
			 * of simulation step width
			 */
			if (attrAirTransmissionModel.getAerosolCloudAirDispersionFactor() > 0) {
				deltaRadius = attrAirTransmissionModel.getAerosolCloudAirDispersionFactor() * simTimeStepLength;
			}

			/*
			 * Increasing extent due to moving air caused by agents, multiplication with simTimeStepLength keeps
			 * deltaRadius independent of simulation step width
			 */
			if (attrAirTransmissionModel.getAerosolCloudPedestrianDispersionWeight() > 0) {
				Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);
			for (Pedestrian pedestrian : pedestriansInsideCloud) {
				deltaRadius += pedestrian.getVelocity().getLength() * attrAirTransmissionModel.getAerosolCloudPedestrianDispersionWeight() * simTimeStepLength;
			}
		}

			aerosolCloud.increaseShape(deltaRadius);
		}
	}

	/**
	 * Deletes aerosol clouds with negligible pathogen concentration, i.e. if current pathogen concentration is smaller
	 * than a threshold (minimumPercentage * initial pathogen concentration)
	 */
	public void deleteExpiredAerosolClouds() {

		double initialCloudVolume = AerosolCloud.radiusToVolume(attrAirTransmissionModel.getAerosolCloudInitialRadius());
		double initialPathogenConcentration = attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad() / initialCloudVolume;
		double minimumConcentration = minimumPercentage * initialPathogenConcentration;

		Collection<AerosolCloud> aerosolCloudsToBeDeleted = topography.getAerosolClouds()
				.stream()
				.filter(a -> a.getPathogenConcentration() < minimumConcentration)
				.collect(Collectors.toSet());
		for (AerosolCloud aerosolCloud : aerosolCloudsToBeDeleted) {
			topography.getAerosolClouds().remove(aerosolCloud);
		}
	}

	public void deleteExpiredDroplets(double simTimeInSec) {
		Collection<Droplets> dropletsToBeDeleted = topography.getDroplets()
				.stream()
				.filter(d -> attrAirTransmissionModel.getDropletsLifeTime() + d.getCreationTime() < simTimeInSec)
				.collect(Collectors.toSet());
		for (Droplets droplets : dropletsToBeDeleted) {
			topography.getDroplets().remove(droplets);
		}
	}

	protected void updatePedestriansHealthStatus(double simTimeInSec) {
		Collection<Pedestrian> allPedestrians = topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus()
					.updateRespiratoryCycle(simTimeInSec, attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	protected void updatePedestriansExposureToAerosolClouds() {
		Collection<Pedestrian> breathingInPeds = topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(p -> p.<AirTransmissionModelHealthStatus>getHealthStatus().isBreathingIn())
				.collect(Collectors.toSet());

		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption during inhalation
		// must be divided into absorption for each sim step:
		double inhalationPeriodLength = attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod() / 2.0;
		double aerosolAbsorptionRatePerSimStep = attrAirTransmissionModel.getAerosolCloudAbsorptionRate() * (simTimeStepLength / inhalationPeriodLength);

		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			Collection<Pedestrian> breathingInPedsInAerosolCloud = breathingInPeds
					.stream()
					.filter(p -> aerosolCloud.getShape().contains(p.getPosition()))
					.collect(Collectors.toSet());

			for (Pedestrian ped : breathingInPedsInAerosolCloud) {
				double deltaDegreeOfExposure = aerosolCloud.getPathogenConcentration() * aerosolAbsorptionRatePerSimStep;
				updatePedestrianDegreeOfExposure(ped, deltaDegreeOfExposure);
			}
		}
	}

	protected void updatePedestriansExposureToDroplets() {
		Collection<Pedestrian> breathingInPeds = topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(p -> p.<AirTransmissionModelHealthStatus>getHealthStatus().isBreathingIn())
				.collect(Collectors.toSet());

		/*
		 * Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption during inhalation
		 * must be divided into absorption for each sim step:
		 */
		double inhalationPeriodLength = attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod() / 2.0;
		double dropletsAbsorptionRatePerSimStep = attrAirTransmissionModel.getDropletsAbsorptionRate() * (simTimeStepLength / inhalationPeriodLength);

		/*
		 * Intake of droplets: Inhaling agents simply absorb a fraction of the pathogen from droplets they are exposed
		 * to. In contrast to intake of pathogen from aerosol clouds, we do not consider concentrations (for simplicity
		 * or to avoid further assumptions on pathogen distribution within droplets).
		 */
		Collection<Droplets> allDroplets = topography.getDroplets();
		for (Droplets droplets : allDroplets) {
			Collection<Pedestrian> breathingInPedsInDroplets = breathingInPeds
					.stream()
					.filter(p -> droplets.getShape().contains(p.getPosition()))
					.collect(Collectors.toSet());

			for (Pedestrian ped : breathingInPedsInDroplets) {
				double deltaDegreeOfExposure = attrAirTransmissionModel.getDropletsPathogenLoad() * dropletsAbsorptionRatePerSimStep;
				updatePedestrianDegreeOfExposure(ped, deltaDegreeOfExposure);
			}
		}
	}

	public Collection<Pedestrian> getInfectiousPedestrians(Topography topography) {
		return topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(Pedestrian::isInfectious)
				.collect(Collectors.toSet());
	}

	@Override
	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		AttributesExposureModelSourceParameters sourceParameters = defineSourceParameters(controller, attrAirTransmissionModel);

		Pedestrian ped = (Pedestrian) scenarioElement;
		ped.setHealthStatus(new AirTransmissionModelHealthStatus());
		ped.setInfectious(sourceParameters.isInfectious());
		ped.<AirTransmissionModelHealthStatus>getHealthStatus().setRespiratoryTimeOffset(random.nextDouble() * attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod());
		ped.<AirTransmissionModelHealthStatus>getHealthStatus().setBreathingIn(false);
		return ped;
	}

	@Override
	public Pedestrian topographyControllerEvent(TopographyController topographyController, double simTimeInSec, Agent agent) {
		Pedestrian pedestrian = (Pedestrian) agent;
		AirTransmissionModelHealthStatus defaultHealthStatus = new AirTransmissionModelHealthStatus();

		pedestrian.setHealthStatus(defaultHealthStatus);
		pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus()
				.setRespiratoryTimeOffset(random.nextDouble() * attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod());

		if (attrAirTransmissionModel.getInfectiousPedestrianIdsNoSource().contains(agent.getId())) {
			pedestrian.setInfectious(true);
		}

		return pedestrian;
	}

	public static Collection<Pedestrian> getDynamicElementsNearAerosolCloud(Topography topography, AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth());

		return topography.getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);
	}

	public static boolean isPedestrianInAerosolCloud(AerosolCloud aerosolCloud, Pedestrian pedestrian) {
		VShape aerosolCloudShape = aerosolCloud.getShape();
		VPoint pedestrianPosition = pedestrian.getPosition();
		return aerosolCloudShape.contains(pedestrianPosition);
	}

	public static Collection<Pedestrian> getPedestriansInsideAerosolCloud(Topography topography, AerosolCloud aerosolCloud) {
		Collection<Pedestrian> pedestriansInsideAerosolCloud = new LinkedList<>();

		Collection<Pedestrian> pedestriansNearAerosolCloud = getDynamicElementsNearAerosolCloud(topography, aerosolCloud);
		for (Pedestrian pedestrian : pedestriansNearAerosolCloud) {
			if (isPedestrianInAerosolCloud(aerosolCloud, pedestrian)){
				pedestriansInsideAerosolCloud.add(pedestrian);
			}
		}
		return pedestriansInsideAerosolCloud;
	}

}
