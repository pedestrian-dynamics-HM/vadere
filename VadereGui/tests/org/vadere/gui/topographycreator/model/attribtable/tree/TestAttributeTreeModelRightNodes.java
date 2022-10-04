package org.vadere.gui.topographycreator.model.attribtable.tree;

import org.junit.Assert;
import org.junit.Test;
import org.vadere.gui.topographycreator.control.attribtable.cells.EditorRegistry;
import org.vadere.gui.topographycreator.control.attribtable.tree.*;
import org.vadere.util.reflection.VadereAttribute;

import java.util.List;

/**
 * these tests check if AttributeTree.parseClassTree(..) creates the right nodes types for the given tested class types
 */
public class TestAttributeTreeModelRightNodes {
    @Test
    public void testClassWithRegisteredAttribute(){
        TestClassA testClass = new TestClassA();
        var tree = AttributeTreeModel.parseClassTree(null,null,testClass.getClass());

        /**
         * We require the class to be registered before testing the tree children
         */
        Assert.assertTrue(EditorRegistry.getInstance().contains(Integer.class));

        Assert.assertTrue(tree.getChildren().get("myInteger").getSecond().getClass().isAssignableFrom(FieldNode.class));
    }

    @Test
    public void testClassWithListType(){
        TestClassB testClass = new TestClassB();
        var tree = AttributeTreeModel.parseClassTree(null,null,testClass.getClass());

        Assert.assertTrue(tree.getChildren().get("myInteger").getSecond().getClass().isAssignableFrom(ArrayNode.class));
    }

    @Test
    public void testClassWithUnregisteredObject(){
        TestClassC testClass = new TestClassC();
        var tree = AttributeTreeModel.parseClassTree(null,null,testClass.getClass());


        Assert.assertTrue(tree.getChildren().get("myInteger").getSecond().getClass().isAssignableFrom(ObjectNode.class));
    }

    @Test
    public void testClassWithAbstractType(){
        TreeModelCache.buildTreeModelCache();
        TestClassD testClass = new TestClassD();
        var tree = AttributeTreeModel.parseClassTree(null,null,testClass.getClass());


        Assert.assertTrue(tree.getChildren().get("myInteger").getSecond().getClass().isAssignableFrom(AbstrNode.class));
    }




    @VadereAttribute private class TestClassA {
        private Integer myInteger;
    }


    @VadereAttribute private class TestClassB {
        private List<Integer> myInteger;
    }

    @VadereAttribute private  class TestClassC {
        private TestClassA myInteger;
    }

    @VadereAttribute private class TestClassD {
        private AbstractClass myInteger;
    }

    private abstract class AbstractClass {

    }
}
