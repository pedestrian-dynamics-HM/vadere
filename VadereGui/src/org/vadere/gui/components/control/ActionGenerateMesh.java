package org.vadere.gui.components.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.topographycreator.view.ActionTranslateTopographyDialog;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.utils.MeshConstructor;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.simulator.utils.pslg.PSLGConverter;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ActionGenerateMesh extends AbstractAction {
	private static final Logger logger = Logger.getLogger(ActionGenerateMesh.class);
	private final ProjectViewModel model;
	private static final Resources RESOURCE = Resources.getInstance("global");
	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

	public ActionGenerateMesh(final ProjectViewModel model) {
		super(Localization.getString("ProjectView.btnGenerateMesh.tooltip"), RESOURCE.getIconSVG("triangulation", ICON_SIZE,ICON_SIZE));
		putValue(SHORT_DESCRIPTION, Localization.getString(ResourceStrings.TOPOGRAPHY_CREATOR_BTN_GENERATE_MESH_TOOLTIP));
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		/*JFileChooser fileChooser = new JFileChooser(CONFIG.getString("SettingsDialog.snapshotDirectory.path"));

		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);*/

		ActionTranslateTopographyDialog dialog = new ActionTranslateTopographyDialog(0.5, 5.0, "hmin, hmax");

		if (dialog.getValue()){
			double hmin = dialog.getX();
			double hmax = dialog.getY();

			PSLGConverter pslgConverter = new PSLGConverter();
			PSLG pslg = pslgConverter.toPSLG(model.getCurrentScenario().getTopography());
			logger.info("generate poly");

			MeshConstructor constructor = new MeshConstructor();

			CompletableFuture.supplyAsync(
					() -> constructor.pslgToAdaptivePMesh(pslg, hmin, hmax, true)).thenAccept(mesh -> saveFloorFieldMesh(mesh,""))
					.exceptionally( ex ->  {
						ex.printStackTrace();
						return null;
					});

			/*CompletableFuture.supplyAsync(
					() -> constructor.pslgToUniformOptimalPMesh(pslg, hmin,true)).thenAccept(mesh -> saveFloorFieldMesh(mesh,""))
					.exceptionally( ex ->  {
						ex.printStackTrace();
						return null;
					});*/
			CompletableFuture.supplyAsync(
					() -> constructor.pslgToCoarsePMesh(pslg, p -> Double.POSITIVE_INFINITY,true)).thenAccept(mesh -> saveFloorFieldMesh(mesh,IOUtils.BACKGROUND_MESH_ENDING))
					.exceptionally( ex ->  {
						ex.printStackTrace();
						return null;
					});
		}
	}

	private void saveFloorFieldMesh(@NotNull final IMeshWithDataStorage<PVertex, PHalfEdge, PFace> meshWithDataStorage, final String ending) {
		logger.info("generate mesh (" + meshWithDataStorage.getMesh().getMinEdgeLen() + ", " + meshWithDataStorage.getMesh().getMaxEdgeLen() + ")");

		File meshDir = new File(model.getCurrentProjectPath().concat("/" + IOUtils.SCENARIO_DIR + "/" + IOUtils.MESH_DIR));
		File outputFile = new File(meshDir.getAbsoluteFile() + "/" +  model.getCurrentScenario().getName() + ending + ".poly");

		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
		String meshString = meshPolyWriter.to2DPoly(meshWithDataStorage);

		if(!meshDir.exists()) {
			meshDir.mkdir();
		}

		if(!outputFile.exists()) {
			try {
				outputFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		try(FileWriter fileWriter = new FileWriter(outputFile)) {
			fileWriter.write(meshString);
			logger.info("generate new mesh file: " + outputFile.getAbsolutePath());
		} catch (IOException ex) {
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
	}
}
