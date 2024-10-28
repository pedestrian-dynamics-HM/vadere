package org.vadere.state.scenario;

import org.vadere.state.attributes.spawner.*;
import org.vadere.state.scenario.spawner.VSpawner;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;

import java.util.Random;

public class SpawnerFactory {
    public static VSpawner create(AttributesSpawner spawnerAttributes, Random random) {
        if(spawnerAttributes instanceof AttributesRegularSpawner){
            return new RegularSpawner((AttributesRegularSpawner) spawnerAttributes,random);
        }
        throw new IllegalArgumentException("There is no spawner registered for "+spawnerAttributes);
    }
}
