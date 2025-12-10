import numpy as np
import gmsh
from sfepy.discrete.fem import Mesh

def build_mesh(geom_data):
    # Initialize Gmsh
    gmsh.initialize()
    gmsh.model.add("airflow_model")

    # 1. SETUP GEOMETRY (Using OpenCASCADE Kernel)
    # --------------------------------------------
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # Create the main domain rectangle
    # addRectangle(x, y, z, dx, dy)
    domain_tag = gmsh.model.occ.addRectangle(x_min, y_min, 0, x_max - x_min, y_max - y_min)

    obstacle_tags = []

    # Create obstacles
    for obs_pts in geom_data['obstacles']:
        # Check if obstacle is a rectangle (optimization)
        # (Assuming axis-aligned for simplicity, but Polygon works too)
        # Let's use generic Polygon for robustness

        # Gmsh requires points -> lines -> wire -> surface
        # But addPolygon is simpler if available, otherwise we use addLineLoop

        # 1. Add Points
        p_tags = []
        for (px, py) in obs_pts:
            # addPoint(x, y, z, mesh_size)
            p_tags.append(gmsh.model.occ.addPoint(px, py, 0))

        # 2. Add Lines
        l_tags = []
        for i in range(len(p_tags)):
            p1 = p_tags[i]
            p2 = p_tags[(i + 1) % len(p_tags)]
            l_tags.append(gmsh.model.occ.addLine(p1, p2))

        # 3. Add Wire (Loop) and Surface
        wire_tag = gmsh.model.occ.addCurveLoop(l_tags)
        surf_tag = gmsh.model.occ.addPlaneSurface([wire_tag])
        obstacle_tags.append((2, surf_tag)) # (Dimension, Tag)

    # 2. BOOLEAN DIFFERENCE (The Magic Step)
    # --------------------------------------
    # Domain - Obstacles
    # This automatically handles "touching walls" and "overlapping" perfectly.
    if obstacle_tags:
        # cut(object, tool)
        gmsh.model.occ.cut([(2, domain_tag)], obstacle_tags)

    # Sync CAD to Mesh model
    gmsh.model.occ.synchronize()

    # 3. MESH SETTINGS
    # ----------------
    # Set global element size based on your 'max_triangle_area'
    # Approx side length h ~ sqrt(2 * area) for equilateral triangle
    target_h = np.sqrt(2 * geom_data['max_triangle_area'])

    # Or rely on Gmsh's adaptive sizing, but let's clamp it
    gmsh.option.setNumber("Mesh.CharacteristicLengthMin", target_h * 0.5)
    gmsh.option.setNumber("Mesh.CharacteristicLengthMax", target_h)

    # Generate 2D Mesh
    gmsh.model.mesh.generate(2)

    # 4. EXTRACT DATA FOR SFEPY
    # -------------------------
    # Get all nodes
    node_tags, node_coords, _ = gmsh.model.mesh.getNodes()

    # Reshape coords (x, y, z) -> (N, 3)
    sfepy_coords = np.array(node_coords).reshape(-1, 3)[:, :2] # Keep only X, Y

    # Get all 2D triangle elements
    # elementTypes: 2 is 3-node triangle
    elem_types, elem_tags, elem_node_tags = gmsh.model.mesh.getElements(dim=2)

    if not elem_types:
        raise RuntimeError("Gmsh failed to generate any 2D elements.")

    # Find the index for triangles (type 2)
    tri_index = -1
    for i, t in enumerate(elem_types):
        if t == 2:
            tri_index = i
            break

    if tri_index == -1:
        raise RuntimeError("No triangle elements found.")

    # Extract connectivity
    # elem_node_tags is a flat list [n1, n2, n3, n1, n2, n3...]
    tri_nodes = np.array(elem_node_tags[tri_index], dtype=np.int32).reshape(-1, 3)

    # Gmsh node tags are 1-based and might not be contiguous.
    # We need to map them to 0-based indices matching sfepy_coords row order.

    # Create mapping: Gmsh_Tag -> Array_Index
    tag_map = {tag: i for i, tag in enumerate(node_tags)}

    # Remap connectivity
    sfepy_conns = []
    current_conn = np.zeros_like(tri_nodes)
    for r in range(tri_nodes.shape[0]):
        for c in range(3):
            current_conn[r, c] = tag_map[tri_nodes[r, c]]

    sfepy_conns.append(current_conn)
    sfepy_mat_ids = [np.zeros(len(current_conn), dtype=np.int32)]

    # Cleanup Gmsh
    gmsh.finalize()

    # 5. CREATE SFEPY MESH
    # --------------------
    mesh = Mesh.from_data('ns_mesh', sfepy_coords, None, sfepy_conns, sfepy_mat_ids, ['2_3'])

    # 6. IDENTIFY BOUNDARIES (Spatial Search)
    # ---------------------------------------
    # (This logic works fine on the generated nodes)
    inlet_indices = set()
    outlet_indices = set()
    eps = 1e-4

    for i, (x, y) in enumerate(sfepy_coords):
        for item in geom_data['inlets'] + geom_data['outlets']:
            c, s = item['coords'], item['side']
            is_inlet = (item in geom_data['inlets'])
            target = inlet_indices if is_inlet else outlet_indices

            hit = False
            if s == 'left' and abs(x - x_min) < eps and c[0]-eps <= y <= c[1]+eps: hit=True
            elif s == 'right' and abs(x - x_max) < eps and c[0]-eps <= y <= c[1]+eps: hit=True
            elif s == 'bottom' and abs(y - y_min) < eps and c[0]-eps <= x <= c[1]+eps: hit=True
            elif s == 'top' and abs(y - y_max) < eps and c[0]-eps <= x <= c[1]+eps: hit=True

            if hit: target.add(i)

    return mesh, {
        'inlet': np.array(list(inlet_indices), dtype=np.int32),
        'outlet': np.array(list(outlet_indices), dtype=np.int32)
    }