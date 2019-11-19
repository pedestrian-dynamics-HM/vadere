package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.model.DefaultConfig;
import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.utils.TreeElementWrapper;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class TopographyTreeView extends JPanel implements ISelectScenarioElementListener, Observer {

	private final IDrawPanelModel<DefaultConfig> panelModel;
	private JTree jTree;
	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode root;
	private HashMap<ScenarioElementType, DefaultMutableTreeNode> categories;

	public TopographyTreeView(final IDrawPanelModel<DefaultConfig> panelModel){
		this.panelModel = panelModel;
		this.panelModel.addSelectScenarioElementListener(this);
		setLayout(new BorderLayout());
		GridBagConstraints cc = new GridBagConstraints();
		this.root = new DefaultMutableTreeNode(TreeElementWrapper.root("Scenario"));
		this.jTree = new JTree(root);
		this.treeModel = (DefaultTreeModel)jTree.getModel();
		loadTree();


		this.jTree.addTreeSelectionListener(e -> {
			if (isVisible()){
				ScenarioElement curr = panelModel.getSelectedElement();
				TreePath newPath = e.getNewLeadSelectionPath();
				if (newPath != null ){
					TreeElementWrapper wrapper = unwrap(newPath);

					if (wrapper.isType(TreeElementWrapper.TreeElementType.LEAF) && !wrapper.compareWithScenarioElement(curr)){
						wrapper.getElement().ifPresent(element -> {
							SwingUtilities.invokeLater(() -> panelModel.setSelectedElement(element));
						});
					}
				}
			}
		});


		ScrollPane pane = new ScrollPane();
		pane.add(jTree);
		pane.setSize(new Dimension(300, Toolkit.getDefaultToolkit().getScreenSize().height));
		add(pane, BorderLayout.CENTER);
	}

	private TreeElementWrapper unwrap(TreePath path){
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		return  (TreeElementWrapper) node.getUserObject();
	}

	private void loadTree(){
		root.removeAllChildren();
		categories = new HashMap<>();
		for (ScenarioElementType type : ScenarioElementType.values()) {
			DefaultMutableTreeNode c = new DefaultMutableTreeNode(TreeElementWrapper.category(type.name()));
			categories.put(type, c);
			root.add(c);
		}

		for (ScenarioElement element : panelModel) {
			DefaultMutableTreeNode typeElement = categories.get(element.getType());
			typeElement.add(new DefaultMutableTreeNode(TreeElementWrapper.leaf(element)));
		}

		treeModel.reload(root);
		TreePath selected = getPath(panelModel.getSelectedElement());
		if (selected != null){
			jTree.addSelectionPath(getPath(panelModel.getSelectedElement()));
			jTree.expandPath(jTree.getSelectionPath());
		}
	}




	TreePath getPath(ScenarioElement scenarioElement) {

		if (scenarioElement == null){
			return new TreePath(new Object[]{
					root
			});

		} else {
			return new TreePath(new Object[]{
					root,
					categories.get(scenarioElement.getType()),
					new DefaultMutableTreeNode(TreeElementWrapper.leaf(scenarioElement))
			});
		}
	}

	// http://www.java2s.com/Code/Java/Swing-JFC/GettreepathfromTreeNode.htm
	TreePath getPath(TreeNode treeNode){
		ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
		if (treeNode != null) {
			nodes.add(treeNode);
			treeNode = treeNode.getParent();
			while (treeNode != null) {
				nodes.add(0, treeNode);
				treeNode = treeNode.getParent();
			}
		}
		return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
	}

	@Override
	public void selectionChange(ScenarioElement scenarioElement) {
		if (isVisible()){
			if (scenarioElement == null){
				jTree.addSelectionPath(null);

			} else {
				DefaultMutableTreeNode category = categories.get(scenarioElement.getType());
				TreeElementWrapper newNode = TreeElementWrapper.leaf(scenarioElement);

				category.children().asIterator().forEachRemaining(treeNode -> {
					DefaultMutableTreeNode node = ((DefaultMutableTreeNode)treeNode);
					TreeElementWrapper element = (TreeElementWrapper)node.getUserObject();
					if (element.equals(newNode)) {

						jTree.clearSelection();
						jTree.addSelectionPath(getPath(treeNode));
						jTree.expandPath(jTree.getSelectionPath());
					}
				});
			}
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (isVisible()){
			for (Map.Entry<ScenarioElementType, DefaultMutableTreeNode> entry : categories.entrySet()) {
				entry.getValue().removeAllChildren();
			}

			for (ScenarioElement element : panelModel) {
				DefaultMutableTreeNode typeElement = categories.get(element.getType());
				typeElement.add(new DefaultMutableTreeNode(TreeElementWrapper.leaf(element)));
			}

			Enumeration<TreePath> expanded = jTree.getExpandedDescendants(getPath(root));
			treeModel.reload(root);
			if (expanded != null){
				expanded.asIterator().forEachRemaining(p-> jTree.expandPath(p));
			}
			selectionChange(panelModel.getSelectedElement());
		}
	}
}
