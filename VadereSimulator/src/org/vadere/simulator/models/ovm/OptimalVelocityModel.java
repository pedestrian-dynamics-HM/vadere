/*
  This class implements the optimal velocity model for car traffic
 */
package org.vadere.simulator.models.ovm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.ode.IntegratorFactory;
import org.vadere.simulator.models.ode.ODEModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesOVM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.parallel.ParallelWorkerUtil;
import org.w3c.dom.Attr;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@ModelClass(isMainModel = true)
public class OptimalVelocityModel extends ODEModel<Car, AttributesCar> {

	private AttributesOVM attributesOVM;
	private OVMEquations ovmEquations;
	private List<Model> models;

	/**
	 * Constructor for OptimalVelocityModel used in the ModelCreator
	 */
	/*@Deprecated
	public OptimalVelocityModel(Topography scenario,
								OVMEquations ovmEquations,
								AttributesOVM attributesOVM,
								AttributesCar elementAttributes,
								boolean pedestrianInteraction,
								Random random) {

		super(Car.class, scenario,
				IntegratorFactory.createFirstOrderIntegrator(attributesOVM.getAttributesODEIntegrator()),
				ovmEquations,
				elementAttributes,
				random);

		this.attributesOVM = attributesOVM;
		this.ovmEquations = ovmEquations;

		ovmEquations.setPedestrianInteraction(pedestrianInteraction);

		ovmEquations.setModelAttributes(attributesOVM);
		ovmEquations.setGradients(null, null, null, scenario);
	}*/

	public OptimalVelocityModel() {
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.domain = domain;
		this.attributesOVM = Model.findAttributes(modelAttributesList, AttributesOVM.class);
		this.elementAttributes = domain.getTopography().getAttributesCar();// Model.findAttributes(modelAttributesList, AttributesCar.class);

		this.ovmEquations = new OVMEquations();

		super.initializeODEModel(Car.class,
				IntegratorFactory.createFirstOrderIntegrator(
						attributesOVM.getAttributesODEIntegrator()),
				ovmEquations, elementAttributes, domain, random);

		ovmEquations.setPedestrianInteraction(false);
		ovmEquations.setModelAttributes(attributesOVM);
		ovmEquations.setGradients(null, null, null, domain.getTopography());

		models = Collections.singletonList(this);
	}

	@Override
	/*
	  Creates a single car with given attributes

	  @param store
	 * @return single Car-Object
	 */
	public <T extends DynamicElement> Agent createElement(VPoint position, int id, Class<T> type) {
		return createElement(position, id, this.elementAttributes, type);
	}

	public <T extends DynamicElement> Agent createElement(VPoint position, int id, Attributes attr, Class<T> type) {

		AttributesCar aAttr = (AttributesCar)attr;

		if (!Car.class.isAssignableFrom(type))
			throw new IllegalArgumentException("OVM cannot initialize " + type.getCanonicalName());
		AttributesCar carAttributes = new AttributesCar(aAttr, registerDynamicElementId(domain.getTopography(), id));
		Car result = new Car(carAttributes, random);
		result.setPosition(position);
		// result.setVelocity(result.getCarAttrributes().getDirection());
		return result;
	}


	@Override
	public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
		return new Car(new AttributesCar(elementAttributes, -1), random).getShape();
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		super.preLoop(simTimeInSec);

		// setup thread pool if it is not setup already
		int WORKERS_COUNT = 16;// pedestrians.keySet().size();
		ParallelWorkerUtil.setup(WORKERS_COUNT);
	}

	@Override
	public void postLoop(final double simTimeInSec) {
		super.postLoop(simTimeInSec);
	}

	@Override
	public void update(final double simTimeInSec) {
		// Get all cars in the topography
		Collection<Car> cars = domain.getTopography().getElements(Car.class);
		// Give cars to the OVMEquations-Object
		ovmEquations.setElements(cars);
		super.update(simTimeInSec);
	}

	/**
	 * @return the attributesOVM
	 */
	public AttributesOVM getAttributesOVM() {
		return attributesOVM;
	}

	@Override
	public List<Model> getSubmodels() {
		return models;
	}

}
