import meshpy.triangle as triangle
import numpy as np
import scipy.optimize as optim
from sfepy.discrete.fem import Mesh

def find_midpoint_of_polygon(obstacle):
    """
    Finds a point inside the polygon to mark as a hole.
    Uses a centroid fallback if linear programming fails or for simple shapes.
    """
    obs = list(obstacle)

    # Quick fallback: simple centroid often works and is faster/safer
    arr = np.array(obs)
    centroid = np.mean(arr, axis=0)

    try:
        obs.append(obs[0]) # Close the loop
        c = [0, 0, -1]
        a_x, a_y, a_r, b = [], [], [], []
        for i in range(len(obs) - 1):
            n = (obs[i][1] - obs[i+1][1], obs[i+1][0] - obs[i][0])
            norm = np.linalg.norm(n)
            if norm == 0: continue
            n = n / norm
            n = -n
            a_x.append(-n[0])
            a_y.append(-n[1])
            a_r.append(1)
            b.append(-n[0] * obs[i][0] - n[1] * obs[i][1])

        result = optim.linprog(c=c, A_ub=np.stack((a_x, a_y, a_r), axis=1), b_ub=b, bounds=(None, None))
        if result.success:
            return result.x[0], result.x[1]
    except Exception:
        pass

    return centroid[0], centroid[1]

def build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max):
    """
    Robust mesh builder using meshpy (Triangle).
    Returns a SfePy Mesh object.
    """

    # 1. Organize points by side
    corners = {
        'bl': (x_min, y_min), 'tl': (x_min, y_max),
        'tr': (x_max, y_max), 'br': (x_max, y_min)
    }

    # Lists to hold points for each side (Counter-Clockwise)
    side_points = {
        'bottom': [corners['bl'], corners['br']],
        'right':  [corners['br'], corners['tr']],
        'top':    [corners['tr'], corners['tl']],
        'left':   [corners['tl'], corners['bl']]
    }

    # Helper to add inlet/outlet points to the correct side list
    def add_features(features):
        for feat in features:
            s = feat['side']
            c = feat['coords']
            if s == 'left':
                p1, p2 = (x_min, c[0]), (x_min, c[1])
            elif s == 'right':
                p1, p2 = (x_max, c[0]), (x_max, c[1])
            elif s == 'bottom':
                p1, p2 = (c[0], y_min), (c[1], y_min)
            elif s == 'top':
                p1, p2 = (c[0], y_max), (c[1], y_max)
            else: continue
            side_points[s].extend([p1, p2])

    add_features(inlets)
    add_features(outlets)

    # 2. Sort and Deduplicate points for each side
    final_boundary_points = []

    def process_side(side_name, sort_key, reverse=False):
        pts = side_points[side_name]
        pts.sort(key=sort_key, reverse=reverse)

        unique_pts = []
        if not pts: return []

        unique_pts.append(pts[0])
        for i in range(1, len(pts)):
            curr = np.array(pts[i])
            prev = np.array(unique_pts[-1])
            if np.linalg.norm(curr - prev) > 1e-9:
                unique_pts.append(pts[i])
        return unique_pts

    b_pts = process_side('bottom', lambda p: p[0])
    r_pts = process_side('right',  lambda p: p[1])
    t_pts = process_side('top',    lambda p: p[0], reverse=True)
    l_pts = process_side('left',   lambda p: p[1], reverse=True)

    # Merge lists into one loop
    full_loop = []
    def merge_lists(source, dest):
        for p in source:
            if not dest:
                dest.append(p)
            else:
                if np.linalg.norm(np.array(p) - np.array(dest[-1])) > 1e-9:
                    dest.append(p)
        # Check wrap-around
        if len(dest) > 1 and np.linalg.norm(np.array(dest[-1]) - np.array(dest[0])) < 1e-9:
            dest.pop()

    merge_lists(b_pts, full_loop)
    merge_lists(r_pts, full_loop)
    merge_lists(t_pts, full_loop)
    merge_lists(l_pts, full_loop)

    points = list(full_loop)

    # Generate segments for the outer boundary
    edges = []
    for i in range(len(points)):
        edges.append((i, (i + 1) % len(points)))

    # 3. Add Obstacles
    holes = []
    for obs in obstacles:
        start_idx = len(points)

        # Clean obstacle points
        clean_obs = []
        if len(obs) > 0:
            clean_obs.append(obs[0])
            for i in range(1, len(obs)):
                if np.linalg.norm(np.array(obs[i]) - np.array(clean_obs[-1])) > 1e-9:
                    clean_obs.append(obs[i])
            if np.linalg.norm(np.array(clean_obs[-1]) - np.array(clean_obs[0])) < 1e-9:
                clean_obs.pop()

        points.extend(clean_obs)
        obs_len = len(clean_obs)
        edges.extend([(start_idx + i, start_idx + (i + 1) % obs_len) for i in range(obs_len)])

        holes.append(find_midpoint_of_polygon(clean_obs))

    # 4. Run MeshPy (Triangle)
    info = triangle.MeshInfo()
    info.set_points(points)
    info.set_holes(holes)
    info.set_facets(edges)

    mesh_data = triangle.build(info, refinement_func=lambda vertices, area: area > area_threshold)

    # 5. Convert to SfePy Mesh
    # SfePy requires: coords, conns (connectivity), mat_ids, descs

    # Coords: (N_nodes, 3) usually, but 2D is (N_nodes, 2)
    sfepy_coords = np.array(mesh_data.points)

    # Connectivity: List of arrays. We have one group of elements (triangles)
    sfepy_conns = [np.array(mesh_data.elements)]

    # Material IDs: All 0 (fluid)
    sfepy_mat_ids = [np.zeros(len(mesh_data.elements), dtype=np.int32)]

    # Descriptors: '2_3' means 2D Triangle
    sfepy_descs = ['2_3']

    # Create the SfePy Mesh Object
    mesh = Mesh.from_data('navier_stokes_mesh',
                          sfepy_coords,
                          None, # nod_grps
                          sfepy_conns,
                          sfepy_mat_ids,
                          sfepy_descs)

    # Return empty dicts for the others to maintain compatibility with your main script's unpacking
    return mesh, {}, {}, {}