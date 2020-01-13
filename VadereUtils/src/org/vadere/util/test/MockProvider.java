package org.vadere.util.test;

public interface MockProvider<T> {

    T get();

    void mockIt();
}
