import numpy as np
import gmsh
import meshio
import os

def build_mesh(geom_data, mesh_filename="temp_mesh"):
    """
    Generates a 2D triangular mesh with refinement on walls, inlets and outlets.
    Saves output to .xdmf for FEniCS.
    """
    # --- 1. INITIALIZATION ---
    if gmsh.is_initialized():
        gmsh.finalize()

    gmsh.initialize()
    gmsh.model.add("airflow_model")

    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # --- 2. SIZING LOGIC (Your exact refinement parameters) ---
    h_max = geom_data['max_triangle_edge_len']
    refinement_factor = 3.0
    h_wall = h_max / refinement_factor
    dist_min = h_wall * 2.0
    dist_max = h_wall * 15.0

    # --- 3. GEOMETRY CONSTRUCTION ---
    domain_tag = gmsh.model.occ.addRectangle(x_min, y_min, 0, x_max - x_min, y_max - y_min)

    # Add points at inlet/outlet locations to force mesh nodes there
    feature_points = []
    all_features = geom_data['inlets'] + geom_data['outlets']

    for feat in all_features:
        s, c = feat['side'], feat['coords']
        pts_to_add = []
        if s == 'bottom':   pts_to_add = [(c[0], y_min), (c[1], y_min)]
        elif s == 'top':    pts_to_add = [(c[0], y_max), (c[1], y_max)]
        elif s == 'left':   pts_to_add = [(x_min, c[0]), (x_min, c[1])]
        elif s == 'right':  pts_to_add = [(x_max, c[0]), (x_max, c[1])]

        for (px, py) in pts_to_add:
            pt_tag = gmsh.model.occ.addPoint(px, py, 0)
            feature_points.append((0, pt_tag))

    # Fragment domain to include these points on the boundary
    if feature_points:
        occ_res, _ = gmsh.model.occ.fragment([(2, domain_tag)], feature_points)
        for dim, tag in occ_res:
            if dim == 2:
                domain_tag = tag
                break

    # Add Obstacles
    obstacle_tags = []
    for obs_pts in geom_data['obstacles']:
        p_tags = [gmsh.model.occ.addPoint(px, py, 0) for px, py in obs_pts]
        l_tags = []
        for i in range(len(p_tags)):
            l_tags.append(gmsh.model.occ.addLine(p_tags[i], p_tags[(i+1)%len(p_tags)]))

        wire_tag = gmsh.model.occ.addCurveLoop(l_tags)
        surf_tag = gmsh.model.occ.addPlaneSurface([wire_tag])
        obstacle_tags.append((2, surf_tag))

    # Cut Obstacles from Domain
    if obstacle_tags:
        occ_res, _ = gmsh.model.occ.cut([(2, domain_tag)], obstacle_tags)
        if occ_res:
            for dim, tag in occ_res:
                if dim == 2:
                    domain_tag = tag
                    break

    gmsh.model.occ.synchronize()

    # --- 4. MESH REFINEMENT FIELDS ---
    boundary_curves = [tag for dim, tag in gmsh.model.getEntities(dim=1)]

    # Distance field
    dist_field = gmsh.model.mesh.field.add("Distance")
    gmsh.model.mesh.field.setNumbers(dist_field, "CurvesList", boundary_curves)
    gmsh.model.mesh.field.setNumber(dist_field, "Sampling", 100)

    # Threshold field
    thresh_field = gmsh.model.mesh.field.add("Threshold")
    gmsh.model.mesh.field.setNumber(thresh_field, "InField", dist_field)
    gmsh.model.mesh.field.setNumber(thresh_field, "SizeMin", h_wall)
    gmsh.model.mesh.field.setNumber(thresh_field, "SizeMax", h_max)
    gmsh.model.mesh.field.setNumber(thresh_field, "DistMin", dist_min)
    gmsh.model.mesh.field.setNumber(thresh_field, "DistMax", dist_max)

    gmsh.model.mesh.field.setAsBackgroundMesh(thresh_field)

    # --- 5. GENERATION ---
    gmsh.option.setNumber("Mesh.Algorithm", 6) # Frontal-Delaunay

    # Prevent GMSH from optimizing away the points we added
    gmsh.option.setNumber("Mesh.CharacteristicLengthExtendFromBoundary", 0)
    gmsh.option.setNumber("Mesh.CharacteristicLengthFromPoints", 0)
    gmsh.option.setNumber("Mesh.CharacteristicLengthFromCurvature", 0)

    gmsh.model.mesh.generate(2)

    # --- 6. EXPORT TO XDMF (Replaces SfePy extraction) ---
    # Save generic .msh
    msh_path = f"{mesh_filename}.msh"
    gmsh.write(msh_path)
    gmsh.finalize()

    # Convert to .xdmf for FEniCS
    msh = meshio.read(msh_path)

    # Extract only triangles (2D domain)
    # Note: FEniCS requires a clean triangle mesh without line elements mixed in the same file usually
    triangle_cells = [c.data for c in msh.cells if c.type == "triangle"][0]
    triangle_mesh = meshio.Mesh(points=msh.points[:, :2], cells={"triangle": triangle_cells})

    xdmf_path = f"{mesh_filename}.xdmf"
    meshio.write(xdmf_path, triangle_mesh)

    # Clean up temp .msh file
    if os.path.exists(msh_path):
        os.remove(msh_path)

    # We return the filename so the main script knows what to load
    return xdmf_path