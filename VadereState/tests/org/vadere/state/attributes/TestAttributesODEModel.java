package org.vadere.state.attributes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.models.AttributesODEIntegrator;
import org.vadere.state.types.IntegratorType;
import org.vadere.state.util.StateJsonConverter;

public class TestAttributesODEModel {

	private static final double delta = 1e-8;
	private AttributesODEIntegrator attributesODEModel;
	private String store;

	/**
	 * Creates a key/value store.
	 */
	@Before
	public void setUp() {
		store = "{" + "\"solverType\" : \"CLASSICAL_RK4\","
				+ "\"toleranceAbsolute\" : " + new Double(1e-5).toString() + "," + "\"toleranceRelative\" : "
				+ new Double(1e-5).toString() + "," + "\"stepSizeMin\" : " + new Double(1e-5).toString() + ","
				+ "\"stepSizeMax\" : " + new Double(1e-5).toString() + "}";
	}

	/**
	 * Test method for
	 * {@link org.vadere.state.attributes.models.AttributesODEIntegrator#AttributesODEModel(java.util.Map)}
	 * . Asserts that creating an {@link AttributesODEIntegrator} with the given
	 * store sets the correct instance variables.
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void testAttributesODEModel() throws IllegalArgumentException, IllegalAccessException {
		// correct case
		attributesODEModel = StateJsonConverter.deserializeObjectFromJson(store, AttributesODEIntegrator.class);
		assertArrayEquals(new double[] {1e-5}, new double[] {attributesODEModel.getToleranceAbsolute()}, delta);
		assertEquals("integrator type is not correct", IntegratorType.CLASSICAL_RK4.name(), attributesODEModel
				.getSolverType().name());

	}
}
