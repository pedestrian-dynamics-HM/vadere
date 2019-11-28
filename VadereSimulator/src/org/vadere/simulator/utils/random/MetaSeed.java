package org.vadere.simulator.utils.random;

import java.util.Random;

public interface MetaSeed {
	Random get(int id);
}
