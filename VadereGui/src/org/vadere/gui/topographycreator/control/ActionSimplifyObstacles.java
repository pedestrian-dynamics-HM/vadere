package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.ActionCombineDialog;
import org.vadere.util.geometry.GrahamScan;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ActionSimplifyObstacles extends TopographyAction{

	private final TopographyAction action;
	private final UndoableEditSupport undoableEditSupport;
	ActionCombineDialog dialog;
	private List<Integer> obstacleIds;

	public ActionSimplifyObstacles(final String name, final String iconPath,String shortDescription, IDrawPanelModel<?> panelModel,
									   TopographyAction action, final UndoableEditSupport undoSupport) {
		super(name, iconPath, shortDescription, panelModel);
		this.action = action;
		this.undoableEditSupport = undoSupport;
		this.obstacleIds = new ArrayList<>();
	}

	public void setIds(List<Integer> ids) {
		this.obstacleIds = ids;
	}

	public void dialogListener(ActionEvent e){
		if (obstacleIds.isEmpty())
			return;

		TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
		List<VPoint> obstaclesVpoints = model.getObstacles()
				.stream()
				.filter(o-> obstacleIds.contains(o.getId()))
				.map(o-> o.getShape().getPath())
				.flatMap(List::stream)
				.collect(Collectors.toList());
		GrahamScan scan = new GrahamScan(obstaclesVpoints);
		VPolygon newObstacle = scan.getPolytope();
		//remove old obstacles
		obstacleIds.forEach(o -> {
			model.getObstacles().stream()
					.filter(el -> el.getId() == o)
					.findAny()
					.ifPresent(el -> {
						if (model.removeElement(el)){
							undoableEditSupport.postEdit(new EditDeleteShape(model, el));
						}
					});
		});
		//add new obstacle
		model.setSelectionShape(newObstacle);
		new ActionAddElement("add element", model, undoableEditSupport).actionPerformed(null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		dialog = new ActionCombineDialog(this, this::dialogListener, getScenarioPanelModel());
	}
}
