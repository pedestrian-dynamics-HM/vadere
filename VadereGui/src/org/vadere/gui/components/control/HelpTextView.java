package org.vadere.gui.components.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class HelpTextView extends JEditorPane {

	private ArrayList<String> filenames;

	public static HelpTextView create(String className){
		HelpTextView view = new HelpTextView();
		view.loadHelpFromClass(className);

		return view;
	}

	public HelpTextView() {
		setContentType("text/html");
		setEditable(false);
		addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
				String link = e.getDescription();
				if (link.startsWith("/helpText/rel_/")){
					String clsName = link.split("/")[3].strip();
					for(String f : filenames){
						if (f.endsWith(clsName)){
							link = f;
							break;
						}
					}
				}
				loadHelpText(link);
			}
		});
		filenames  = new ArrayList<>();

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

	public void loadHelpFromClass(String fullClassName){
		loadHelpText("/helpText/" + fullClassName + ".html");
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
