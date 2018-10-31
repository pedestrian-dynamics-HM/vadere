package org.vadere.simulator.utils.reflection;

import java.lang.reflect.Field;

public class ReflectionHelper {

	private Class<?> concreetClass;
	private Object o;

	private ReflectionHelper(Object o){
		this.o = o;
		this.concreetClass  = o.getClass();
	}

	public static ReflectionHelper create(Object o){
		return new ReflectionHelper(o);
	}

	public <T> void setValOfFile(String member, T value) throws NoSuchFieldException, IllegalAccessException {
		Field field = getField(member);

		if (field.isAccessible()) {
			field.set(o, value);
		} else {
			field.setAccessible(true);
			field.set(o, value);
			field.setAccessible(false);
		}
	}

	private Field getField(String member)throws NoSuchFieldException {
		Field field = null;
		Class c = concreetClass;

		while ((field == null) && (c != null)) {
			try {
				field = c.getDeclaredField(member);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}

		if (field == null)
			throw new NoSuchFieldException(concreetClass.getCanonicalName() + "has not member: " + member);

		return field;
	}

	public <T> T valOfField(String member) throws NoSuchFieldException, IllegalAccessException {
		Field field = getField(member);
		T val;

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
