package org.vadere.util.reflection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.reflection.CouldNotInstantiateException;
import org.vadere.util.reflection.DynamicClassInstantiator;
import org.vadere.util.reflection.VadereClassNotFoundException;

public class TestDynamicClassInstantiator {

	private DynamicClassInstantiator<Collection<?>> instantiator;

	@Before
	public void setUp() {
		instantiator = new DynamicClassInstantiator<>();
	}

	@Test
	public void testCreateObjectSuccessfully() {
		instantiator.createObject("java.util.ArrayList");
	}

	@Test
	public void testCreateObjectFromNonexistingClass() {
		try {
			instantiator.createObject("nonexisting.Clazz");
			fail();
		} catch (CouldNotInstantiateException e) {
			fail("unexpected exception");
		} catch (VadereClassNotFoundException e) {
			// this is expected
		}
	}

	@Test
	public void testCreateObjectWithoutDefaultConstructor() {
		try {
			instantiator.createObject(ListWithoutDefaultConstructor.class);
			fail();
		} catch (CouldNotInstantiateException e) {
			// this is expected
			assertTrue(e.getCause() instanceof NoSuchMethodException);
		}
	}

	@Test
	public void testCreateObjectWithPrivateConstructor() {
		try {
			instantiator.createObject(ListWithPrivateConstructor.class);
			fail();
		} catch (CouldNotInstantiateException e) {
			// this is expected
			assertTrue(e.getCause() instanceof IllegalAccessException);
		}
	}

	@Test
	public void testCreateDifferentObject() {
		try {
			// This assignment is necessary to provoke ClassCastException
			@SuppressWarnings("unused")
			Collection<?> dummy = instantiator.createObject("java.lang.String");
			fail("works although it should not");
		} catch (VadereClassNotFoundException e) {
			fail("unexpected exception: " + e);
		} catch (ClassCastException e) {
			// this is expected
		}
	}

	private static class ListWithoutDefaultConstructor extends ArrayList<Object> {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		public ListWithoutDefaultConstructor(String dummy) {}
	}

	private static class ListWithPrivateConstructor extends ArrayList<Object> {
		private static final long serialVersionUID = 1L;

		private ListWithPrivateConstructor() {}
	}

}
