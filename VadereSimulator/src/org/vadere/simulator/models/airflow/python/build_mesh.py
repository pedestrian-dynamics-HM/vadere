import numpy as np
import gmsh
from sfepy.discrete.fem import Mesh

def build_mesh(geom_data):
    # 1. GMSH SETUP
    gmsh.initialize()
    gmsh.model.add("airflow_model")

    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # Create Domain
    domain_tag = gmsh.model.occ.addRectangle(x_min, y_min, 0, x_max - x_min, y_max - y_min)

    # 2. FORCE NODES AT INLETS/OUTLETS (Critical for robustness)
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

    if feature_points:
        occ_res, _ = gmsh.model.occ.fragment([(2, domain_tag)], feature_points)
        domain_tag = occ_res[0][1]

    # 3. CREATE OBSTACLES
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
        if occ_res: domain_tag = occ_res[0][1]

    gmsh.model.occ.synchronize()

    # 4. MESH SETTINGS (Natural Look)
    # Calculate target edge length from user area
    target_h = np.sqrt(2 * geom_data['max_triangle_area'])

    gmsh.option.setNumber("Mesh.Algorithm", 5) # Delaunay (Natural look)
    gmsh.option.setNumber("Mesh.Smoothing", 0) # Disable smoothing for organic feel
    gmsh.option.setNumber("Mesh.CharacteristicLengthMin", target_h * 0.1)
    gmsh.option.setNumber("Mesh.CharacteristicLengthMax", target_h)

    gmsh.model.mesh.generate(2)

    # 5. EXTRACT DATA
    node_tags, node_coords, _ = gmsh.model.mesh.getNodes()
    sfepy_coords = np.array(node_coords).reshape(-1, 3)[:, :2]

    elem_types, elem_tags, elem_node_tags = gmsh.model.mesh.getElements(dim=2)
    tri_index = next((i for i, t in enumerate(elem_types) if t == 2), -1)

    if tri_index == -1:
        raise RuntimeError("Gmsh failed to generate 2D elements.")

    tri_nodes = np.array(elem_node_tags[tri_index], dtype=np.int32).reshape(-1, 3)

    tag_map = {tag: i for i, tag in enumerate(node_tags)}
    current_conn = np.zeros_like(tri_nodes)
    for r in range(tri_nodes.shape[0]):
        for c in range(3):
            current_conn[r, c] = tag_map[tri_nodes[r, c]]

    sfepy_conns = [current_conn]
    sfepy_mat_ids = [np.zeros(len(current_conn), dtype=np.int32)]

    gmsh.finalize()

    # 6. CREATE MESH & BOUNDARIES
    mesh = Mesh.from_data('ns_mesh', sfepy_coords, None, sfepy_conns, sfepy_mat_ids, ['2_3'])

    inlet_indices = set()
    outlet_indices = set()
    eps = 1e-4

    for i, (x, y) in enumerate(sfepy_coords):
        for item in all_features:
            c = sorted(item['coords'])
            s = item['side']
            is_inlet = (item in geom_data['inlets'])
            target = inlet_indices if is_inlet else outlet_indices

            hit = False
            if s == 'left' and abs(x - x_min) < eps:
                if c[0] - eps <= y <= c[1] + eps: hit = True
            elif s == 'right' and abs(x - x_max) < eps:
                if c[0] - eps <= y <= c[1] + eps: hit = True
            elif s == 'bottom' and abs(y - y_min) < eps:
                if c[0] - eps <= x <= c[1] + eps: hit = True
            elif s == 'top' and abs(y - y_max) < eps:
                if c[0] - eps <= x <= c[1] + eps: hit = True

            if hit: target.add(i)

    return mesh, {
        'inlet': np.array(list(inlet_indices), dtype=np.int32),
        'outlet': np.array(list(outlet_indices), dtype=np.int32)
    }