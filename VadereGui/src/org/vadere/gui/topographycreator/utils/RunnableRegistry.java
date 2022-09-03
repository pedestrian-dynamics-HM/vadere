package org.vadere.gui.topographycreator.utils;

import java.util.HashMap;
import java.util.Optional;

public class RunnableRegistry {
    private HashMap<Object, Runnable> runnableRegistry = new HashMap<>();
    private Optional<Runnable> defaultRunnable = Optional.empty();;
    public RunnableRegistry(){}

    public  void registerAction(Object o,Runnable r){
        this.runnableRegistry.put(o,r);
    }

    public void registerDefault(Runnable r){
        this.defaultRunnable = Optional.of(r);
    }

    public void apply(Object o) {
        if (this.runnableRegistry.containsKey(o)) {
            this.runnableRegistry.get(o).run();
            return;
        }
        this.defaultRunnable.ifPresent(r -> r.run());
    }
}
