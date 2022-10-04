package org.vadere.gui.topographycreator.control.attribtable.util;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;

public class ManualAttributeTableFocus {

    private ArrayList<Component> focusList = new ArrayList<>();

    public void add(Component c){
        focusList.add(c);
    }
    public void add(java.util.List<Component> c){
        focusList.addAll(c);
    }

    public void addListener(){
        for(var c : focusList){
            c.addKeyListener( new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_TAB){
                        if (e.isShiftDown()){
                            var c =getComponentBefore(e.getComponent());
                            var ret = c.requestFocusInWindow();
                            System.out.println("Last");
                        } else {
                            var c =getComponentAfter(e.getComponent());
                            var ret = c.requestFocusInWindow();
                            System.out.println("Next");
                        }
                        e.consume();
                    }
                }

                public Component getComponentAfter(Component component) {
                    int newIndex = (focusList.indexOf(component) + 1)% focusList.size();
                    return focusList.get(newIndex);
                }


                public Component getComponentBefore(Component component) {
                    var nextIndex = focusList.indexOf(component)-1;
                    if (nextIndex < 0){
                        nextIndex = focusList.size() -1;
                    }
                    return  focusList.get(nextIndex);
                }
            });
        }
    }



}
