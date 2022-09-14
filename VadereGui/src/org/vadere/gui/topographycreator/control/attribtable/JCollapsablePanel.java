package org.vadere.gui.topographycreator.control.attribtable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * JCollapsablePanel implements a panel with a header which hides its content
 * when the header is clicked upon.
 */
public class JCollapsablePanel extends JPanel implements Observer {
    /**
     * contentPanel is used as a container for all added components
     */
    private final JPanel contentPanel;
    /**
     * head is used as the header displayed at the top
     */
    private final JLabel head;
    private GridBagConstraints gbc;


    /**
     * hidden is storing the visibility state of the contentPanel container
     */
    private boolean hidden = false;

    public enum Style {
        HEADER, GROUP
    }

    private final Observable observable;

    public JCollapsablePanel(String title, Style panelStyle) {
        super(new GridBagLayout());
        this.observable = new Observable();
        contentPanel = new JPanel(new GridBagLayout());
        head = new JLabel(title);


        initializeGridBagConstraint();

        if (panelStyle.equals(Style.GROUP)) {
            initializeGroupHeaderStyle(head);
            head.addMouseListener(new GroupHeaderMouseInputAdapter(head,contentPanel));
        } else {
            initializeSectionHeaderStyle(head);
            head.addMouseListener(new SectionHeaderMouseInputAdapter(contentPanel));
        }

        this.add(head,gbc);
        this.add(contentPanel,gbc);
    }

    private static void initializeSectionHeaderStyle(JLabel head) {
        var margin = new EmptyBorder(4,4,4,4);
        var color = UIManager.getColor("Component.borderColor");
        var border = new LineBorder(color,1);
        head.setFont(head.getFont().deriveFont(Font.BOLD));
        head.setBorder(new CompoundBorder(border,margin));
        head.setBackground(UIManager.getColor("Menu.background"));
    }

    private void initializeGroupHeaderStyle(JLabel head) {
        var c= UIManager.getColor("Table.selectionBackground");
        setBackground(new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(c.getAlpha()*0.5)));
        head.setBorder(new EmptyBorder(4,4,4,4));
        head.setIcon(UIManager.getIcon("Tree.expandedIcon"));
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
    }

    /**
     * This method is used for adding components directly with the wanted
     * layout contraints
     * @param comp   the component to be added
     * @return
     */
    public Component add(Component comp) {
        comp.setVisible(!hidden);
        if(comp instanceof Observer){
            this.observable.addObserver((Observer) comp);
        }
        this.contentPanel.add(comp,gbc);
        return comp;
    }

    @Override
    public void update(Observable o, Object arg) {
        this.observable.notifyObservers(arg);
    }

    /**
     * SectionHeaderMouseInputAdapter is used as a listener
     * for mouse input to hande the state switching of the
     * contents visibility if the panel is section panel
     */
    private class SectionHeaderMouseInputAdapter extends MouseInputAdapter {

        private final JPanel contentPanel;

        private SectionHeaderMouseInputAdapter(JPanel contentPanel) {
            this.contentPanel = contentPanel;
        }

        @Override
        public void mouseClicked(MouseEvent e) {

            if (hidden) {
                contentPanel.setVisible(true);
                hidden = false;

            } else {
                contentPanel.setVisible(false);
                hidden = true;
            }
            getParent().invalidate();
        }
    }

    /**
     * GroupHeaderMouseInputAdapter is used as a listener
     * for mouse input to hande the state switching of the
     * contents visibility if the panel is group panel
     */
    private class GroupHeaderMouseInputAdapter extends MouseInputAdapter {

        private final JLabel head;
        private final JPanel contentPanel;

        public GroupHeaderMouseInputAdapter(JLabel head,JPanel contentPanel) {
            this.head = head;
            this.contentPanel = contentPanel;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (hidden) {
                contentPanel.setVisible(true);
                head.setIcon(UIManager.getIcon("Tree.expandedIcon"));
                hidden = false;

            } else {
                contentPanel.setVisible(false);
                head.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
                hidden = true;
            }
            revalidate();
            repaint();
            getParent().validate();
        }
    }
}
