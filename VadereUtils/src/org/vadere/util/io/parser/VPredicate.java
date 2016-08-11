package org.vadere.util.io.parser;

import java.text.ParseException;

/**
 * see {@link java.util.function.Predicate} + ParseException
 *
 * @param <T>
 */
@FunctionalInterface
public interface VPredicate<T> {
	Boolean test(T testObject) throws ParseException;
}

