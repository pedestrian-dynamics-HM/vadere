package org.vadere.state.attributes.models.psychology;

import org.vadere.state.attributes.models.psychology.HelperAttributes.AttributesRouteChoiceDefinition;

import java.util.LinkedList;
import java.util.List;

public class AttributesProbabilisticCognitionModel extends AttributesCognitionModel {
    List<AttributesRouteChoiceDefinition> routeChoices;

    public AttributesProbabilisticCognitionModel(){
        routeChoices = new LinkedList<>();
    }

    public List<AttributesRouteChoiceDefinition> getRouteChoices() {
        return routeChoices;
    }

    public void setRouteChoices(List<AttributesRouteChoiceDefinition> routeChoices) {
        this.routeChoices = routeChoices;
    }


}
