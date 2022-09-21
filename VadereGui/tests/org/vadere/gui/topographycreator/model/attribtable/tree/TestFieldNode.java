package org.vadere.gui.topographycreator.model.attribtable.tree;

import org.junit.Test;
import org.vadere.gui.topographycreator.control.attribtable.tree.FieldNode;

public class TestFieldNode {
    @Test
    public void test(){
        FieldNode node = new FieldNode(null,null);
    }



    private class MyClass{
        private Integer myInteger;

        public Integer getMyInteger() {
            return myInteger;
        }

        public void setMyInteger(Integer myInteger) {
            this.myInteger = myInteger;
        }
    }
}
