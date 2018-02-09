package org.vadere.tests.reflection;

import java.lang.reflect.Field;

public class ReflectionHelper {

	private Class<?> concreetClass;
	private Object o;

	public ReflectionHelper(Object o){
		this.o = o;
		this.concreetClass  = o.getClass();
	}

	public static ReflectionHelper create(Object o){
		return new ReflectionHelper(o);
	}

	public <T> T valOfField(String member) throws NoSuchFieldException, IllegalAccessException {
		Field field = null;
		Class c = concreetClass;
		T val;

		while ((field == null) && (c != null)) {
			try {
				field = c.getDeclaredField(member);
			} catch (NoSuchFieldException e) {
				field = null;
				c = c.getSuperclass();
			}
		}

		if (field == null)
			throw new NoSuchFieldException(concreetClass.getCanonicalName() + "has not member: " + member);

		if (field.isAccessible()) {
			val = (T) field.get(o);
		} else {
			field.setAccessible(true);
			val = (T) field.get(o);
			field.setAccessible(false);
		}

		return val;
	}
}
