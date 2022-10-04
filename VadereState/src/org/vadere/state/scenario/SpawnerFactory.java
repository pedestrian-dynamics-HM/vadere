package org.vadere.state.scenario;

import org.vadere.state.attributes.spawner.*;
import org.vadere.state.scenario.spawner.VSpawner;
import org.vadere.state.scenario.spawner.impl.LERPSpawner;
import org.vadere.state.scenario.spawner.impl.MixedSpawner;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;
import org.vadere.state.scenario.spawner.impl.TimeSeriesSpawner;

import java.util.Random;

public class SpawnerFactory {
    public static VSpawner create(AttributesSpawner spawnerAttributes, Random random) {
        if(spawnerAttributes instanceof AttributesRegularSpawner){
            return new RegularSpawner((AttributesRegularSpawner) spawnerAttributes,random);
        }
        if(spawnerAttributes instanceof AttributesLerpSpawner){
            return new LERPSpawner((AttributesLerpSpawner) spawnerAttributes,random);
        }
        if(spawnerAttributes instanceof AttributesMixedSpawner){
            return new MixedSpawner((AttributesMixedSpawner)spawnerAttributes,random);
        }
        if(spawnerAttributes instanceof AttributesTimeSeriesSpawner){
            return new TimeSeriesSpawner((AttributesTimeSeriesSpawner) spawnerAttributes,random);
        }
        throw new IllegalArgumentException("There is no spawner registered for "+spawnerAttributes);
    }
}
