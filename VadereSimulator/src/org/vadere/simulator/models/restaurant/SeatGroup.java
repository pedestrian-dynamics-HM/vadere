package org.vadere.simulator.models.restaurant;
import java.util.ArrayList;

import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Source;

public class SeatGroup {
    private Target tableTarget;

    //private ArrayList<Source> sources;

    private ArrayList<Target> seatTargets;

    private int nextSeatIndex;


    // specify size of arrayLists directly for more efficiency
    public SeatGroup (Target tableTarget, int seatTargetsSize) {
        this.tableTarget = tableTarget;
        //this.sources = new ArrayList<>(sourcesSize);
        this.seatTargets = new ArrayList<>(seatTargetsSize);
        this.nextSeatIndex = 0;
    }

    public SeatGroup (Target tableTarget, ArrayList<Target> seatTargets) {
        this.tableTarget = tableTarget;
        //this.sources = sources;
        this.seatTargets = seatTargets;
        this.nextSeatIndex = 0;
    }

    public Target getTableTarget() { return this.tableTarget; }

    //public void addSource(Source source) {
    //    this.sources.add(source);
    //}

    public void addSeatTarget(Target target) {
        this.seatTargets.add(target);
    }

    // just iterate over seats
    // TODO get closest next seat or do any other kind of ordering
    public int nextSeatTargetId() {
        int nextSeatId = this.seatTargets.get(this.nextSeatIndex).getId();
        this.nextSeatIndex = (this.nextSeatIndex + 1) % this.seatTargets.size();
        return nextSeatId;
    }

    //getfreeseats
    //getnextseat
    //getdirection



}
