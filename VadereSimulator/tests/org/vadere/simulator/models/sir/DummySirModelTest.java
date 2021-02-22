package org.vadere.simulator.models.sir;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * BaseSirModelTest will have helpers for different SIR implementations
 */
public class DummySirModelTest extends BaseSirModelTest {

	List<Attributes> attributeList;
	DummySirModel sir;

	// will run before each test to setup test environment
	@Before
	public void setup(){
		attributeList = getSimpleState(); //rValue = 2
		sir = new DummySirModel();
	}

	// will run after each test to cleanup test environment (mostly not necessary)
	@After
	public void after(){
		attributeList.clear();
	}


	// each test must be public and have a @Test
	@Test
	public void testInitializeOk(){

		// initialize must find the AttributeSIR from the  attributeList // initialR = 2.0
		sir.initialize(attributeList, null, null, null);
		assertEquals(attributeList.get(0), sir.getAttributeSIR());

		// someModelState must be initialized
		assertEquals(2.0 * 1, sir.getSomeModelState(), 0.001);

	}

	@Test(expected = AttributesNotFoundException.class)
	public void testInitializeFail(){

		// if  AttributeSIR  not found error must be thrown
		sir.initialize(new ArrayList<>(), null, null, null);
	}


	@Test()
	public void checkModelWorksProbably(){

		// initialize // initialR = 2.0
		sir.initialize(attributeList, null, null, null);

		sir.preLoop(0.0);

		// with each update the modelState must be correct
		sir.update(0.4);
		assertEquals(2.0 * 0.4, sir.getSomeModelState(), 0.001);
		sir.update(5.4);
		assertEquals(2.0 *5.4 , sir.getSomeModelState(), 0.001);

		sir.postLoop(0.0);

	}

}