package org.vadere.util.observer;

public class NotifyContext {
    private Class clazz;
    public NotifyContext(Class clazz){
        this.clazz = clazz;
    }

    public Class getNotifyContext(){
        return clazz;
    }
}
