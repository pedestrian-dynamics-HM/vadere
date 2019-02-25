package org.vadere.util.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * For example, this class can be used for creating {@link attributes.Attributes} or
 * {@link models.Model}s.
 * 
 *
 * @param <T> Objects created by this class should be subclasses of T but it cannot be guaranteed!
 *        The user should catch {@link java.lang.ClassCastException}s.
 */
public class DynamicClassInstantiator<T> {

	public T createObject(String className)
			throws VadereClassNotFoundException, CouldNotInstantiateException {

		return createObject(getClassFromName(className));
	}

	public T createObject(Class<? extends T> clazz)
			throws CouldNotInstantiateException {

		try {
			// Surprisingly, type correctness cannot be checked here.
			// I cannot provoke a ClassCastException by doing:
			// T result = clazz.newInstance();
			// This is an issue that comes back to the user of this API.
			return clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new CouldNotInstantiateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public Class<? extends T> getClassFromName(String className) throws VadereClassNotFoundException {
		try {
			return (Class<? extends T>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new VadereClassNotFoundException(e);
		}
	}

}
