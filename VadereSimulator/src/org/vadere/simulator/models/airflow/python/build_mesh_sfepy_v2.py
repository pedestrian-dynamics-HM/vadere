import json
import numpy as np
import scipy.optimize as optim
import meshpy.triangle as triangle
from sfepy.discrete.fem import Mesh

def extract_attributes(scenario_file_path):
    """
    Parses the JSON scenario file and returns geometry parameters.
    """
    with open(scenario_file_path) as file:
        data = json.load(file)

    topography = data['scenario']['topography']
    attributes_model = data['scenario']['attributesModel']['org.vadere.state.attributes.models.airflow.AttributesAirFlowModel']

    # 1. Bounds Calculation
    topo_xmin = topography['attributes']['bounds']['x'] + topography['attributes']['boundingBoxWidth']
    topo_ymin = topography['attributes']['bounds']['y'] + topography['attributes']['boundingBoxWidth']
    topo_xmax = topography['attributes']['bounds']['x'] + topography['attributes']['bounds']['width']
    topo_ymax = topography['attributes']['bounds']['y'] + topography['attributes']['bounds']['height']

    airflow_xmin = attributes_model['bounds']['xmin']
    airflow_xmax = attributes_model['bounds']['xmax']
    airflow_ymin = attributes_model['bounds']['ymin']
    airflow_ymax = attributes_model['bounds']['ymax']

    x_min = max(topo_xmin, airflow_xmin)
    x_max = min(topo_xmax, airflow_xmax)
    y_min = max(topo_ymin, airflow_ymin)
    y_max = min(topo_ymax, airflow_ymax)

    # 2. Parameters
    grid_size = float(attributes_model['gridSize'])
    area_threshold = float(attributes_model['areaThreshold'])
    inlet_velocity = float(attributes_model['inletVelocity'])
    blocking_obstacles = attributes_model['blockingObstacles']

    # 3. Inlets / Outlets
    inlets = []
    for i, ins in enumerate(attributes_model['inlets']):
        inlets.append({
            "id": i,
            "side": ins['side'],
            "coords": [float(ins['start']), float(ins['start'] + ins['width'])]
        })

    outlets = []
    for i, outs in enumerate(attributes_model['outlets']):
        outlets.append({
            "id": i,
            "side": outs['side'],
            "coords": [float(outs['start']), float(outs['start'] + outs['width'])]
        })

    # 4. Obstacles
    obstacles = []
    for obstacle in topography['obstacles']:
        if obstacle['id'] in blocking_obstacles:
            if obstacle['shape']['type'] == 'RECTANGLE':
                ob_x_min = obstacle['shape']['x']
                ob_y_min = obstacle['shape']['y']
                ob_x_max = ob_x_min + obstacle['shape']['width']
                ob_y_max = ob_y_min + obstacle['shape']['height']
                # Store as list of points
                obstacles.append([(ob_x_min, ob_y_min), (ob_x_min, ob_y_max),
                                  (ob_x_max, ob_y_max), (ob_x_max, ob_y_min)])

            elif obstacle['shape']['type'] == 'POLYGON':
                obstacles.append([(point['x'], point['y']) for point in obstacle['shape']['points']])

    return {
        'grid_size': grid_size,
        'area_threshold': area_threshold,
        'x_min': x_min, 'x_max': x_max,
        'y_min': y_min, 'y_max': y_max,
        'inlet_velocity': inlet_velocity,
        'inlets': inlets,
        'outlets': outlets,
        'obstacles': obstacles
    }

def find_hole_point(obstacle):
    """Finds a point inside the polygon to mark as a hole (void)."""
    obs = list(obstacle)
    arr = np.array(obs)
    # Fast centroid fallback usually works for convex/simple shapes
    centroid = np.mean(arr, axis=0)
    return centroid[0], centroid[1]

def build_mesh_and_indices(geom_data):
    """
    Builds the mesh using MeshPy and identifies boundary node indices.

    Returns:
        mesh (sfepy.discrete.fem.Mesh): The SfePy mesh object.
        boundaries (dict): Dictionary containing numpy arrays of node indices
                           keys: 'inlet', 'outlet', 'walls'
    """
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']
    area_threshold = geom_data['area_threshold']

    # --- 1. Construct Boundary Segments ---
    # To keep this robust, we define the full box loop, inserting inlet/outlet points

    points = []

    # Helper to generate points along a side, inserting features
    def get_side_points(p_start, p_end, fixed_dim, dim_index, side_name, features):
        # Base points
        pts = [p_start, p_end]

        # Add feature points
        relevant = [f for f in features if f['side'] == side_name]
        for feat in relevant:
            c = feat['coords']
            if dim_index == 0: # Horizontal side (vary x)
                pts.append((c[0], fixed_dim))
                pts.append((c[1], fixed_dim))
            else: # Vertical side (vary y)
                pts.append((fixed_dim, c[0]))
                pts.append((fixed_dim, c[1]))

        # Sort points based on direction
        pts = np.array(pts)
        if dim_index == 0:
            # Sort by x
            idx = np.argsort(pts[:,0])
            if p_start[0] > p_end[0]: idx = idx[::-1] # Reverse if going right-to-left
        else:
            # Sort by y
            idx = np.argsort(pts[:,1])
            if p_start[1] > p_end[1]: idx = idx[::-1] # Reverse if going top-to-bottom

        return pts[idx].tolist()

    # Build the outer loop Counter-Clockwise
    # Bottom (Left to Right)
    loop = get_side_points((x_min, y_min), (x_max, y_min), y_min, 0, 'bottom', geom_data['inlets'] + geom_data['outlets'])
    # Right (Bottom to Top)
    loop += get_side_points((x_max, y_min), (x_max, y_max), x_max, 1, 'right', geom_data['inlets'] + geom_data['outlets'])[1:]
    # Top (Right to Left)
    loop += get_side_points((x_max, y_max), (x_min, y_max), y_max, 0, 'top', geom_data['inlets'] + geom_data['outlets'])[1:]
    # Left (Top to Bottom)
    loop += get_side_points((x_min, y_max), (x_min, y_min), x_min, 1, 'left', geom_data['inlets'] + geom_data['outlets'])[1:]

    # Remove duplicate closure point if exists
    if np.linalg.norm(np.array(loop[0]) - np.array(loop[-1])) < 1e-9:
        loop.pop()

    points = loop
    edges = [(i, (i + 1) % len(points)) for i in range(len(points))]

    # Add Obstacles
    holes = []
    for obs in geom_data['obstacles']:
        start_idx = len(points)
        # Clean duplicates
        clean_obs = []
        for p in obs:
            if not clean_obs or np.linalg.norm(np.array(p) - np.array(clean_obs[-1])) > 1e-9:
                clean_obs.append(p)
        if np.linalg.norm(np.array(clean_obs[0]) - np.array(clean_obs[-1])) < 1e-9:
            clean_obs.pop() # Open the loop for logic below

        points.extend(clean_obs)
        n_obs = len(clean_obs)
        edges.extend([(start_idx + i, start_idx + (i + 1) % n_obs) for i in range(n_obs)])
        holes.append(find_hole_point(clean_obs))

    # --- 2. Generate Mesh ---
    info = triangle.MeshInfo()
    info.set_points(points)
    info.set_holes(holes)
    info.set_facets(edges)

    # Generate
    mesh_data = triangle.build(info, refinement_func=lambda vertices, area: area > area_threshold)

    sfepy_coords = np.array(mesh_data.points)
    sfepy_conns = [np.array(mesh_data.elements)]
    sfepy_mat_ids = [np.zeros(len(mesh_data.elements), dtype=np.int32)]
    sfepy_descs = ['2_3'] # 2D Triangles

    # --- 3. Identify Boundary Nodes (The "Right Format") ---
    # instead of doing this in main, we do it here where we have the logic
    # We create sets of indices

    eps = 1e-4
    inlet_indices = set()
    outlet_indices = set()
    wall_indices = set()

    # Check every node against definitions
    # This is O(N) but safer than geometric searching in main
    for i, (x, y) in enumerate(sfepy_coords):
        is_boundary = False

        # Check Inlets
        for item in geom_data['inlets']:
            c = item['coords']
            s = item['side']
            hit = False
            if s == 'left' and abs(x - x_min) < eps and c[0]-eps <= y <= c[1]+eps: hit = True
            elif s == 'right' and abs(x - x_max) < eps and c[0]-eps <= y <= c[1]+eps: hit = True
            elif s == 'bottom' and abs(y - y_min) < eps and c[0]-eps <= x <= c[1]+eps: hit = True
            elif s == 'top' and abs(y - y_max) < eps and c[0]-eps <= x <= c[1]+eps: hit = True

            if hit:
                inlet_indices.add(i)
                is_boundary = True

        # Check Outlets
        for item in geom_data['outlets']:
            c = item['coords']
            s = item['side']
            hit = False
            if s == 'left' and abs(x - x_min) < eps and c[0]-eps <= y <= c[1]+eps: hit = True
            elif s == 'right' and abs(x - x_max) < eps and c[0]-eps <= y <= c[1]+eps: hit = True
            elif s == 'bottom' and abs(y - y_min) < eps and c[0]-eps <= x <= c[1]+eps: hit = True
            elif s == 'top' and abs(y - y_max) < eps and c[0]-eps <= x <= c[1]+eps: hit = True

            if hit:
                outlet_indices.add(i)
                is_boundary = True

        # Check Outer Walls (excluding identified inlets/outlets)
        if not is_boundary:
            # Check Outer Box
            on_box = (abs(x - x_min) < eps or abs(x - x_max) < eps or
                      abs(y - y_min) < eps or abs(y - y_max) < eps)

            # Check Obstacles (internal walls)
            # A node is on an obstacle if it matches the points provided or lies on segment
            # For simplicity in FEM, "Walls" usually means everything that is not Inlet/Outlet
            # But strictly, we check if it's on a boundary segment.
            # Meshpy ensures boundary nodes are preserved.
            # A simple approach: If it's on a facet but not inlet/outlet, it's a wall.
            # However, meshpy doesn't easily give "is on boundary" without markers.
            # Fallback: Assume if it is on the outer box, it's a wall.
            if on_box:
                wall_indices.add(i)

    # Note: Internal obstacle boundaries are tricky without Markers.
    # To handle obstacles correctly, we should rely on SfePy's "vertices of surface"
    # minus inlets/outlets.
    # So we return explicit inlets/outlets, and let SfePy deduce the rest.

    mesh = Mesh.from_data('navier_stokes_mesh', sfepy_coords, None, sfepy_conns, sfepy_mat_ids, sfepy_descs)

    boundaries = {
        'inlet': np.array(list(inlet_indices), dtype=np.int32),
        'outlet': np.array(list(outlet_indices), dtype=np.int32)
        # We will calculate 'wall' in main as (All_Boundary - Inlet - Outlet) to catch obstacles automatically
    }

    return mesh, boundaries