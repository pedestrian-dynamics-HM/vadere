package org.vadere.meshing.utils.io.movie;

import org.jcodec.api.awt.SequenceEncoder;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

/**
 *
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MovRecorder<V extends IVertex, E extends IHalfEdge, F extends IFace> {
	private MeshRenderer<V, E, F> meshRenderer;
	private GenEikMesh<V, E, F> eikMesh;
	private int initImageCount = 10;
	private int refineImageCount = 1;
	private final double width;
	private final double height;
	private SequenceEncoder enc;
	private Date todaysDate;

	public MovRecorder(
			@NotNull final GenEikMesh<V, E, F> eikMesh,
			@NotNull final MeshRenderer<V, E, F> meshRenderer,
			final double width,
			final double height) throws IOException {
		this.eikMesh = eikMesh;
		this.meshRenderer = meshRenderer;
		this.width = width;
		this.height = height;
		this.todaysDate = new java.util.Date();
		File outputFile = new File("./eikmesh_"+todaysDate.toString()+".mov");
		this.enc = new SequenceEncoder(outputFile);
	}

	public void record() throws IOException {
		// record init
		while (!eikMesh.isInitialized()) {
			eikMesh.initialize();
			BufferedImage bi = meshRenderer.renderImage((int)Math.ceil(width), (int)Math.ceil(height));
			for(int i = 0; i < initImageCount; i++) {
				enc.encodeImage(bi);
			}
		}

		// record refine
		while (!eikMesh.isFinished()) {
			eikMesh.improve();
			BufferedImage bi = meshRenderer.renderImage((int)Math.ceil(width), (int)Math.ceil(height));
			for(int i = 0; i < refineImageCount; i++) {
				enc.encodeImage(bi);
			}
		}

		BufferedImage bi = meshRenderer.renderImage((int)Math.ceil(width), (int)Math.ceil(height));
		File outputFile = new File("./eikmesh_last"+todaysDate.toString()+".png");
		ImageIO.write(bi, "png", outputFile);
	}

	public void finish() throws IOException {
		enc.finish();
	}
}