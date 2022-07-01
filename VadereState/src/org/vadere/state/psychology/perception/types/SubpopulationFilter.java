package org.vadere.state.psychology.perception.types;

import java.util.LinkedList;

public class SubpopulationFilter {

    private LinkedList<Integer> affectedPedestrianIds;

    public SubpopulationFilter(){
        this.affectedPedestrianIds = new LinkedList<>();
    }

    public SubpopulationFilter(LinkedList<Integer> affectedPedestrianIds) {
        this.affectedPedestrianIds = affectedPedestrianIds;
    }

    public LinkedList<Integer> getAffectedPedestrianIds() {
        return affectedPedestrianIds;
    }

    public void setAffectedPedestrianIds(LinkedList<Integer> affectedPedestrianIds) {
        this.affectedPedestrianIds = affectedPedestrianIds;
    }





}
