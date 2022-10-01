package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.reflections.Reflections;
import org.vadere.simulator.context.VadereContext;
import org.vadere.state.attributes.AttributesScenarioElement;

import java.util.HashMap;
import java.util.Set;

/**
 * Simple Cache to store Reflection calls to save time.
 */
public class TreeModelCache {

    private final HashMap<String, Set<Class<?>>> classCache = new HashMap<>();

    public static TreeModelCache buildTreeModelCache(){
        System.out.println("build tree model cache ...");
        VadereContext ctx = new VadereContext();
        ctx.put(VadereContext.TREE_NODE_CTX, new TreeModelCache());
        VadereContext.add("GUI", ctx);
        TreeModelCache cache = (TreeModelCache)VadereContext.getCtx("GUI").get(VadereContext.TREE_NODE_CTX);
        cache.getSubTypeOff(AttributesScenarioElement.class);
        return cache;
    }

    public Set<Class<?>> getSubTypeOff(Class clazz){
        if (!classCache.containsKey(clazz.getCanonicalName())){
            classCache.put(clazz.getCanonicalName(), new Reflections("org.vadere").getSubTypesOf(clazz));
        }
        return classCache.get(clazz.getCanonicalName());
    }
}
