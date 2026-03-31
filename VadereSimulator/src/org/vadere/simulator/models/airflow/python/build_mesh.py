import numpy as np
import gmsh
from sfepy.discrete.fem import Mesh

def build_mesh(geom_data):
    """
    Generates a 2D triangular mesh with refinement on walls, inlets and outlets.
    """
    # INITIALIZATION
    gmsh.initialize()
    gmsh.model.remove()
    gmsh.model.add("airflow_model")

    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # SIZING LOGIC
    h_max = geom_data['max_triangle_edge_len']
    refinement_factor = 2.0
    h_wall = h_max / refinement_factor
    dist_min = h_wall * 2.0   # distance up to which h_wall is strictly enforced
    dist_max = h_wall * 5.0  # distance at which mesh reaches full h_max

    # GEOMETRY
    domain_tag = gmsh.model.occ.addRectangle(x_min, y_min, 0, x_max - x_min, y_max - y_min)
    feature_points = []
    all_features = geom_data['inlets'] + geom_data['outlets']

    for feat in all_features:
        s, c = feat['side'], feat['coords']
        pts_to_add = []
        if s == "south":   pts_to_add = [(c[0], y_min), (c[1], y_min)]
        elif s == "north":    pts_to_add = [(c[0], y_max), (c[1], y_max)]
        elif s == "west":   pts_to_add = [(x_min, c[0]), (x_min, c[1])]
        elif s == "east":  pts_to_add = [(x_max, c[0]), (x_max, c[1])]

        for (px, py) in pts_to_add:
            pt_tag = gmsh.model.occ.addPoint(px, py, 0)
            feature_points.append((0, pt_tag))

    if feature_points:
        occ_res, _ = gmsh.model.occ.fragment([(2, domain_tag)], feature_points)
        for dim, tag in occ_res:
            if dim == 2:
                domain_tag = tag
                break

    obstacle_tags = []
    for obs_pts in geom_data['obstacles']:
        p_tags = [gmsh.model.occ.addPoint(px, py, 0) for px, py in obs_pts]
        l_tags = []
        for i in range(len(p_tags)):
            l_tags.append(gmsh.model.occ.addLine(p_tags[i], p_tags[(i+1)%len(p_tags)]))

        wire_tag = gmsh.model.occ.addCurveLoop(l_tags)
        surf_tag = gmsh.model.occ.addPlaneSurface([wire_tag])
        obstacle_tags.append((2, surf_tag))

    if obstacle_tags:
        occ_res, _ = gmsh.model.occ.cut([(2, domain_tag)], obstacle_tags)
        # Update domain tag again
        if occ_res:
            for dim, tag in occ_res:
                if dim == 2:
                    domain_tag = tag
                    break

    gmsh.model.occ.synchronize()

    # MESH FIELDS
    boundary_curves = [tag for dim, tag in gmsh.model.getEntities(dim=1)]

    # distance to boundaries
    dist_field = gmsh.model.mesh.field.add("Distance")
    gmsh.model.mesh.field.setNumbers(dist_field, "CurvesList", boundary_curves)
    gmsh.model.mesh.field.setNumber(dist_field, "Sampling", 100)

    # applies h_wall close to curves, interpolates to h_bulk far away
    thresh_field = gmsh.model.mesh.field.add("Threshold")
    gmsh.model.mesh.field.setNumber(thresh_field, "InField", dist_field)
    gmsh.model.mesh.field.setNumber(thresh_field, "SizeMin", h_wall)
    gmsh.model.mesh.field.setNumber(thresh_field, "SizeMax", h_max)
    gmsh.model.mesh.field.setNumber(thresh_field, "DistMin", dist_min)
    gmsh.model.mesh.field.setNumber(thresh_field, "DistMax", dist_max)

    gmsh.model.mesh.field.setAsBackgroundMesh(thresh_field)

    # MESH GENERATION SETTINGS
    gmsh.option.setNumber("Mesh.Algorithm", 6) # Frontal-Delaunay
    gmsh.option.setNumber("Mesh.CharacteristicLengthExtendFromBoundary", 0)
    gmsh.option.setNumber("Mesh.CharacteristicLengthFromPoints", 0)
    gmsh.option.setNumber("Mesh.CharacteristicLengthFromCurvature", 0)

    gmsh.model.mesh.generate(2)

    # EXTRACT DATA FOR SFEPY
    node_tags, node_coords, _ = gmsh.model.mesh.getNodes()
    sfepy_coords = np.array(node_coords).reshape(-1, 3)[:, :2]

    elem_types, elem_tags, elem_node_tags = gmsh.model.mesh.getElements(dim=2)

    try:
        elem_types, elem_tags, elem_node_tags = gmsh.model.mesh.getElements(dim=2)
        tri_type_indices = np.where(elem_types == 2)[0]

        if len(tri_type_indices) == 0:
            gmsh.finalize()
            raise RuntimeError("Gmsh failed to generate 2D triangular elements.")

        tri_index = tri_type_indices[0]
        tri_nodes = np.array(elem_node_tags[tri_index], dtype=np.int32).reshape(-1, 3)
    except ValueError:
        gmsh.finalize()
        raise RuntimeError("Gmsh failed to generate 2D triangular elements.")

    tri_nodes = np.array(elem_node_tags[tri_index], dtype=np.int32).reshape(-1, 3)
    max_tag = np.max(node_tags)

    if max_tag < 2 * len(node_tags):
        tag_map_array = np.zeros(max_tag + 1, dtype=np.int32)
        tag_map_array[node_tags] = np.arange(len(node_tags))
        sfepy_conns_array = tag_map_array[tri_nodes]
    else:
        tag_map = {tag: i for i, tag in enumerate(node_tags)}
        sfepy_conns_array = np.zeros_like(tri_nodes)
        for r in range(tri_nodes.shape[0]):
            for c in range(3):
                sfepy_conns_array[r, c] = tag_map[tri_nodes[r, c]]

    sfepy_conns = [sfepy_conns_array]
    sfepy_mat_ids = [np.zeros(len(sfepy_conns_array), dtype=np.int32)]

    gmsh.finalize()

    # CREATE MESH OBJECT
    mesh = Mesh.from_data('ns_mesh', sfepy_coords, None, sfepy_conns, sfepy_mat_ids, ['2_3'])

    # IDENTIFY BOUNDARY NODES
    inlet_indices = set()
    outlet_indices = set()
    eps = 1e-5

    feats_by_side = {"west": [], "east": [], "north": [], "south": []}
    for item in all_features:
        c = sorted(item['coords'])
        is_inlet = (item in geom_data['inlets'])
        feats_by_side[item['side']].append((c[0], c[1], is_inlet))

    coords = sfepy_coords

    # helper for vectorized boundary checking
    def check_side(mask, coord_idx, side_key):
        if not np.any(mask): return
        potential_idxs = np.where(mask)[0]
        vals = coords[potential_idxs, coord_idx]

        for (c0, c1, is_inlet) in feats_by_side[side_key]:
            hits = (vals >= c0 - eps) & (vals <= c1 + eps)
            hit_indices = potential_idxs[hits]
            if is_inlet:
                inlet_indices.update(hit_indices)
            else:
                outlet_indices.update(hit_indices)

    check_side(np.abs(coords[:, 0] - x_min) < eps, 1, "west")
    check_side(np.abs(coords[:, 0] - x_max) < eps, 1, "east")
    check_side(np.abs(coords[:, 1] - y_min) < eps, 0, "south")
    check_side(np.abs(coords[:, 1] - y_max) < eps, 0, "north")

    return mesh, {
        'inlet': np.array(list(inlet_indices), dtype=np.int32),
        'outlet': np.array(list(outlet_indices), dtype=np.int32)
    }