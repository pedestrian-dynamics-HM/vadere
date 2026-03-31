package org.vadere.state.attributes.models.restaurant;


import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;
import java.util.LinkedList;

@ModelAttributeClass
public class AttributesRestaurantModel extends Attributes{
    private ArrayList<AttributesSeatGroup> attrsSeatGroup;

    public static final int INVALID_ID = -1;

    public AttributesRestaurantModel() {
        this.attrsSeatGroup = new ArrayList<AttributesSeatGroup>();
        this.attrsSeatGroup.add(new AttributesSeatGroup());
    }

    public AttributesRestaurantModel(ArrayList<AttributesSeatGroup> attrsSeatGroup) {
        this.attrsSeatGroup = attrsSeatGroup;
    }

    public ArrayList<AttributesSeatGroup> getAttrsSeatGroup() { return this.attrsSeatGroup; }

    public LinkedList<Integer> getTableTargetIds() {
        LinkedList<Integer> targetIds = new LinkedList<>();
        for (AttributesSeatGroup attrtable : this.attrsSeatGroup) {
            if (attrtable.getTableTargetId() != INVALID_ID) {
                targetIds.add(attrtable.getTableTargetId());
            }
        }
        return targetIds;
    }

    public LinkedList<Integer> getSeatTargetIds() {
        LinkedList<Integer> targetIds = new LinkedList<>();
        for (AttributesSeatGroup attrtable : this.attrsSeatGroup) {
            targetIds.addAll(attrtable.getSeatTargetIds());
        }
        return targetIds;
    }
}
