package org.vadere.simulator.utils.cache;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class MeshTxtCacheObject extends AbstractCacheObject implements IMeshCacheObject {
	private  static Logger logger = Logger.getLogger(MeshTxtCacheObject.class);

	public MeshTxtCacheObject(@NotNull final String cacheIdentifier, @NotNull final File cacheLocation){
		super(cacheIdentifier, cacheLocation);
	}

	public MeshTxtCacheObject(@NotNull final String cacheIdentifier, @NotNull final File cacheLocation, @NotNull final InputStream inputStream) {
		super(cacheIdentifier, cacheLocation, inputStream);
	}

	@Override
	public void initializeObjectFromCache(@NotNull final PMesh mesh) throws CacheException {
		try {
			MeshPolyReader<PVertex, PHalfEdge, PFace> meshPolyReader = new MeshPolyReader<>(() -> new PMesh());
			InputStream fastInputStream = new FastBufferedInputStream(inputStream);
			meshPolyReader.readMesh(fastInputStream, i -> MeshEikonalSolverFMM.namePotential);
		} catch (IOException e) {
			throw new CacheException("Cannot load cache from TXT InputStream", e);
		}
	}

	@Override
	public void persistObject(@NotNull final PMesh mesh) throws CacheException {
		try {
			PrintWriter writer = new PrintWriter(new FastBufferedOutputStream(new FileOutputStream(cacheLocation)));
			MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
			meshPolyWriter.to2DPoly(mesh, 1, i -> MeshEikonalSolverFMM.namePotential, v -> false, writer);
		} catch (FileNotFoundException e) {
			logger.errorf("cannot save cache %s", cacheLocation.getAbsolutePath());
		}
	}

	@Override
	public String getCacheLocation() {
		return null;
	}
}


