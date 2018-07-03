package org.vadere.util.factory.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseModelHelper {

	protected HashMap<String,Class> mainModels;
	protected HashMap<String, HashMap<String,Class>> models;

	public BaseModelHelper(){
		this.mainModels = new HashMap<>();
		this.models = new HashMap<>();
	}

	public Stream<String> getSortedMainModel(){
		return mainModels.keySet().stream().sorted();
	}

	/**
	 *  Return subModels grouped by package and sorted by FQN
	 *  Key: Package Name
	 *  Value: QualifiedName of (Sub)-Model
 	 */

	public Stream<Map.Entry<String, List<String>>> getModelsSortedByPackageStream(){
		return  models.entrySet().stream().collect(Collectors.toMap(
				e -> e.getKey() ,
				e -> e.getValue().keySet().stream().sorted().collect(Collectors.toList())))
				.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey));
	}
}
