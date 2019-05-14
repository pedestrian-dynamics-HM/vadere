package org.vadere.simulator.projects.migration.jsontranformation;


import org.vadere.simulator.projects.migration.jsontranformation.jolt.JoltTransformV6toV7;
import org.vadere.simulator.projects.migration.jsontranformation.json.JsonTransformation_Default;
import org.vadere.util.factory.BaseFactory;
import org.vadere.util.factory.FactoryObject;
import org.vadere.util.logging.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

/**
 * This is the Base version for the JsonTransformationFactory which automatically build by
 * a Annotation Processor at build time. Do not directly use this class
 */
public class JsonTransformationBaseFactory extends BaseFactory<JsonTransformation, FactoryObject<JsonTransformation>> {

	private final static Logger logger = Logger.getLogger(JsonTransformationBaseFactory.class);

	HashMap<String, JsonTransformation> transformationMap;

	public JsonTransformationBaseFactory(){
		this.transformationMap = new LinkedHashMap<>();
		addMember("default", JsonTransformation_Default.class, this::getJsonTransformation_Default);
	}

	public void addMember(String versionLabel, Class<?> clazz, Supplier supplier){
		supplierMap.put(versionLabel, new FactoryObject<>(clazz, supplier));
	}

	// Getters
	public JsonTransformation_Default getJsonTransformation_Default(){
		return new JsonTransformation_Default();
	}


	@Override
	public JsonTransformation getInstanceOf(String key) throws ClassNotFoundException {
		if (transformationMap.containsKey(key)){
			return transformationMap.get(key);
		} else {
			if (supplierMap.containsKey(key)){
				JsonTransformation tmp = supplierMap.get(key).getSupplier().get();
				transformationMap.put(key, tmp);
				return tmp;
			} else if (supplierMap.containsKey("default")){
				logger.infof("No specific transformation fount for key: %s. Using default transformation (version increment only)", key);
				JsonTransformation tmp = supplierMap.get("default").getSupplier().get();
				transformationMap.put(key, tmp); // save default transformation for other occurrences for given version.
				return tmp;
			}
			throw new ClassNotFoundException("No class associated With Key: " + key + " in this Factory");
		}
	}
}
