package org.vadere.util.triangulation.triangulator;

/**
 * @author Benedikt Zoennchen
 *
 * A triangle generator creates a triangulation using a certain strategy.
 */
@FunctionalInterface
public interface ITriangulator {
    void generate();
}
