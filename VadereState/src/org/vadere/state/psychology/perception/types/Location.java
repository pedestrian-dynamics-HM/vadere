package org.vadere.state.psychology.perception.types;

import org.vadere.util.geometry.shapes.VShape;

import java.util.LinkedList;
import java.util.List;

/**
 * A location in which one ore more stimuli can be perceived.
 *
 * This information is required by a stimulus controller
 * which injects the actual stimuli into the simulation loop.
 */
public class Location {

    private List<VShape> areas;

    public Location(){
        areas = new LinkedList<>();
    }

    public Location(List<VShape> areas){
        this.areas = areas;
    }

    public Location(VShape area){
        this.areas = new LinkedList<>();
        addArea(area);
    }

    private void addArea(VShape area) {
        this.areas.add(area);
    }

    public List<VShape> getAreas(){
        return this.areas;
    }


}
