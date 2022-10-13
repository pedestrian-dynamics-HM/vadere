package org.vadere.gui.components.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.topographycreator.view.ActionTranslateTopographyDialog;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.utils.MeshConstructor;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.simulator.utils.pslg.PSLGConverter;
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

	public ActionGenerateMesh(final String name, Icon icon,String shortDescription, final ProjectViewModel model) {
		super(name, icon);
		putValue(SHORT_DESCRIPTION, Messages.getString(shortDescription));
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

	private void saveFloorFieldMesh(@NotNull final IMesh<PVertex, PHalfEdge, PFace> mesh, final String ending) {
		logger.info("generate mesh (" + mesh.getMinEdgeLen() + ", " + mesh.getMaxEdgeLen() + ")");

		File meshDir = new File(model.getCurrentProjectPath().concat("/" + IOUtils.SCENARIO_DIR + "/" + IOUtils.MESH_DIR));
		File outputFile = new File(meshDir.getAbsoluteFile() + "/" +  model.getCurrentScenario().getName() + ending + ".poly");

		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
		String meshString = meshPolyWriter.to2DPoly(mesh);

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
