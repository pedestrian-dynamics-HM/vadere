package org.vadere.state.psychology.perception.types;

import java.util.LinkedList;

public abstract class StimuliWrapper extends Stimulus {

    public StimuliWrapper(){
        super();
    }

    public StimuliWrapper(double time){
        super(time);
    }

    public StimuliWrapper(StimuliWrapper other) {
        super(other);
    }

    public abstract LinkedList<Stimulus> unpackStimuli();


}
