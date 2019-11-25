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
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MovRecorder<P extends EikMeshPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> {
	private MeshRenderer<V, E, F> meshRenderer;
	private GenEikMesh<V, E, F> eikMesh;
	private int initImageCount = 10;
	private int refineImageCount = 1;
	private final int width;
	private final int height;
	private SequenceEncoder enc;
	private VRectangle bound;
	private Date todaysDate;

	public MovRecorder(
			@NotNull final GenEikMesh<V, E, F> eikMesh,
			@NotNull final MeshRenderer<V, E, F> meshRenderer,
			final int width,
			final int height,
			final VRectangle bound) throws IOException {
		this.eikMesh = eikMesh;
		this.bound = bound;
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
			BufferedImage bi = meshRenderer.renderImage(width, height, bound);
			for(int i = 0; i < initImageCount; i++) {
				enc.encodeImage(bi);
			}
		}

		// record refine
		while (!eikMesh.isFinished()) {
			eikMesh.improve();
			BufferedImage bi = meshRenderer.renderImage(width, height, bound);
			for(int i = 0; i < refineImageCount; i++) {
				enc.encodeImage(bi);
			}
		}

		BufferedImage bi = meshRenderer.renderImage(width, height, bound);
		File outputFile = new File("./eikmesh_last"+todaysDate.toString()+".png");
		ImageIO.write(bi, "png", outputFile);
	}

	public void finish() throws IOException {
		enc.finish();
	}
}