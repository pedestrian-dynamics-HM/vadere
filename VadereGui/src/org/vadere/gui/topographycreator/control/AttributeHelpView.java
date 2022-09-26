package org.vadere.gui.topographycreator.control;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class AttributeHelpView extends JTextPane {

    private ArrayList<String> filenames;
    private static AttributeHelpView view;

    private AttributeHelpView(){
        filenames = new ArrayList<>();
        setContentType("text/html");
        setEditable(false);

        try (
                InputStream in = getClass().getResourceAsStream("/helpText");
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public static AttributeHelpView getInstance() {
        if(view == null)
            view = new AttributeHelpView();
        return view;
    }

    public void loadHelpFromField(Field field){
        loadHelpText("/helpText/" + field.getDeclaringClass().getName()+"VVV"+field.getName() + ".html");
    }

    public void loadHelpText(String helpTextId){
        System.out.println(helpTextId);
        String text = null;
        try {
            InputStream url = getClass().getResourceAsStream(helpTextId);
            text = new String(url.readAllBytes());
        } catch (Exception ignored) {
            text = "No Help found.";
        }
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        StyleSheet sheet = htmlEditorKit.getStyleSheet();
        sheet.addRule(".local_link {font-style: italic; text-decoration: underline;}");
        sheet.addRule(".class_link {color: blue; font-style: italic; text-decoration: underline;}");
        sheet.addRule("p { padding-bottom: 5px;}");
        Document doc = htmlEditorKit.createDefaultDocument();
        setDocument(doc);
        setText(text);
    }
}
