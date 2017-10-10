package org.vadere.gui.components.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class JComboCheckBox<E> extends JComboBox {

	private Map<Object, Boolean> memory = new HashMap<>(); // keys are the entries of the comboBox (that get rendered via toString()), values are true or false (=checkBox checked or not)
	private String NO_CHECKED_ITEMS_TEXT = " ";


	public JComboCheckBox(List<E> items) {
		super(items.toArray());
		items.forEach(item -> memory.put(item, false));
		addActionListener(ae -> {
			if (!ae.getActionCommand().equals("inputComplete")) {
				memory.put(getSelectedItem(), !memory.get(getSelectedItem())); // toogle associated boolean in memory upon click on item
				hidePopup();
				setPopupVisible(true);
			}
		});

		addPopupMenuListener(new PopupMenuListener() {
			@Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {
				repaint();
				inputCompleted();
			}
		});

		setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

				if (!isPopupVisible()) {
					StringBuilder sb = new StringBuilder();
					getCheckedItemsStream().forEach(key -> sb.append(", ").append(key));
					JLabel label = new JLabel(sb.length() > 2 ? sb.substring(2) : NO_CHECKED_ITEMS_TEXT);
					label.setBorder(new EmptyBorder(4, 0, 4, 0)); // empirical values, lead to the popup having the correct height
					return label;
				}

				if(value != null) {
					JCheckBox cb = new JCheckBox(value == null ? "" : value.toString());

					if(value != null) {
						cb.setSelected(memory.get(value));
					}


					if (isSelected) {
						cb.setBackground(list.getSelectionBackground());
						cb.setForeground(list.getSelectionForeground());
					} else {
						cb.setBackground(list.getBackground());
						cb.setForeground(list.getForeground());
					}

					return cb;
				}
				else {
					return this;
				}

			}
		});
	}

	@Override
	public void setPopupVisible(boolean v) {
		if (v) {
			super.setPopupVisible(v);
		}
	}

	@Override
	public void hidePopup() {
		super.setPopupVisible(false);
	}

	private void inputCompleted() {
		SwingUtilities.invokeLater(() -> {
			String oldCommand = getActionCommand();
			setActionCommand("inputComplete");
			fireActionEvent();
			setActionCommand(oldCommand);
		});
	}

	private Stream<Object> getCheckedItemsStream() {
		return memory.keySet().stream().filter(item -> memory.get(item));
	}

	public void setCheckedItems(List<E> checkedItems) {
		checkedItems.forEach(checkedItem -> memory.put(checkedItem, true));
	}

	public List<E> getCheckedItems() {
		return getCheckedItemsStream().map(item -> (E) item).collect(Collectors.toList());
	}

	public void setNoCheckedItemsText(String text) {
		NO_CHECKED_ITEMS_TEXT = text;
	}
}
