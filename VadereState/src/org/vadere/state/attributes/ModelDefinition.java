package org.vadere.state.attributes;

import org.vadere.util.reflection.DynamicClassInstantiator;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO for the model definition.
 *
 */
public class ModelDefinition {

	private String mainModel;
	private List<Attributes> attributesModel;

	public ModelDefinition(String mainModel, List<Attributes> attributesModel) {
		this.mainModel = mainModel;
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

	public void setMainModel(String mainModel) {
		this.mainModel = mainModel;
	}

	public List<Attributes> getAttributesList() {
		return attributesModel;
	}

	public void setAttributesList(List<Attributes> attributesList) {
		this.attributesModel = attributesList;
	}

}
