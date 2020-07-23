package org.vadere.state.attributes;

import java.util.ArrayList;
import java.util.List;

import org.vadere.util.reflection.DynamicClassInstantiator;

/**
 * POJO for the model definition.
 *
 */
public class ModelDefinition {

	private String mainModel;
	private String strategyModel;
	private List<Attributes> attributesModel;

	public ModelDefinition(String mainModel, String strategyModel, List<Attributes> attributesModel) {
		this.mainModel = mainModel;
		this.strategyModel = strategyModel;
		this.attributesModel = attributesModel;
	}



	public void createAndSetDefaultAttributes(List<Class<? extends Attributes>> attributesClasses) {
		DynamicClassInstantiator<Attributes> instantiator = new DynamicClassInstantiator<>();
		attributesModel = new ArrayList<>(attributesClasses.size());
		for (Class<? extends Attributes> clazz : attributesClasses) {
			attributesModel.add(instantiator.createObject(clazz));
		}
	}

	public String getMainModel() {
		return mainModel;
	}

	public String getStrategyModel() {
		return strategyModel;
	}

	public void setMainModel(String mainModel) {
		this.mainModel = mainModel;
	}
	public void setStrategyModel(String strategyModel) {
		this.strategyModel = strategyModel;
	}


	public List<Attributes> getAttributesList() {
		return attributesModel;
	}

	public void setAttributesList(List<Attributes> attributesList) {
		this.attributesModel = attributesList;
	}

}
