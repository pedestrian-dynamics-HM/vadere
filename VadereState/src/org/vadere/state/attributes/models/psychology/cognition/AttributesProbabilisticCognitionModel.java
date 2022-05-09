package org.vadere.state.attributes.models.psychology.cognition;

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
