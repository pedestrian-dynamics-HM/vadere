import meshpy.triangle as triangle
from skfem import *
import numpy as np
import scipy.optimize as optim


def round_trip_connect(start, end):
    """Connects points in a loop."""
    return [(i, i + 1) for i in range(start, end)] + [(end, start)]

def get_boundary_lambda(side, coords, x_min, x_max, y_min, y_max):
    """Returns lambda function to identify boundary facets."""
    tol = 1e-5
    if side == 'left':
        return lambda x: np.isclose(x[0], x_min, atol=tol) & (x[1] >= coords[0]-tol) & (x[1] <= coords[1]+tol)
    elif side == 'right':
        return lambda x: np.isclose(x[0], x_max, atol=tol) & (x[1] >= coords[0]-tol) & (x[1] <= coords[1]+tol)
    elif side == 'bottom':
        return lambda x: np.isclose(x[1], y_min, atol=tol) & (x[0] >= coords[0]-tol) & (x[0] <= coords[1]+tol)
    elif side == 'top':
        return lambda x: np.isclose(x[1], y_max, atol=tol) & (x[0] >= coords[0]-tol) & (x[0] <= coords[1]+tol)
    else:
        raise ValueError("Unknown side")

def is_on_obstacle_boundary(x, obstacles_arg):
    """
    Checks if points x lie on the boundary of any polygon.
    Optimized slightly for vectorization.
    """
    on_any_polygon_segment = np.zeros(x.shape[1], dtype=bool)
    tol = 1e-9
    t_tol = 1e-7

    for polygon_vertices in obstacles_arg:
        verts = np.array(polygon_vertices)
        num_poly_verts = len(verts)
        if num_poly_verts < 2:
            continue

        p_start = verts
        p_end = np.roll(verts, -1, axis=0) # Shifted array to get next point

        # Vectorized segment calculations
        seg_vec = p_end - p_start
        seg_length_squared = np.sum(seg_vec**2, axis=1)

        # Check point distance to segments
        # We iterate segments manually to compare against the batch of points 'x'
        for i in range(num_poly_verts):
            p1 = p_start[i]
            v = seg_vec[i]
            len_sq = seg_length_squared[i]

            if len_sq < tol**2:
                dist_sq = (x[0] - p1[0])**2 + (x[1] - p1[1])**2
                on_any_polygon_segment |= (dist_sq < tol**2)
                continue

            pt_vec_dx = x[0] - p1[0]
            pt_vec_dy = x[1] - p1[1]

            dot_prod = pt_vec_dx * v[0] + pt_vec_dy * v[1]
            t = dot_prod / len_sq

            # Check projection
            projects = (t >= -t_tol) & (t <= 1 + t_tol)

            # Perpendicular distance
            cross_prod = pt_vec_dx * v[1] - pt_vec_dy * v[0]
            dist_sq_line = (cross_prod**2) / len_sq

            is_on_segment = projects & (dist_sq_line < tol**2)
            on_any_polygon_segment |= is_on_segment

    return on_any_polygon_segment

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
    Robust mesh builder.
    Fixes segfaults by merging collinear boundary points and removing duplicates.
    """

    # 1. Organize points by side
    # We define the 4 corners
    corners = {
        'bl': (x_min, y_min), 'tl': (x_min, y_max),
        'tr': (x_max, y_max), 'br': (x_max, y_min)
    }

    # Lists to hold points for each side.
    # We treat the boundary as a continuous loop: Bottom -> Right -> Top -> Left (Counter-Clockwise)
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
            # Based on side, we know which coordinate is fixed
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

    # 2. Sort and Deduplicate points for each side to form valid segments
    final_boundary_points = []

    def process_side(side_name, sort_key, reverse=False):
        pts = side_points[side_name]
        # Sort
        pts.sort(key=sort_key, reverse=reverse)

        # Remove duplicates (using a small tolerance is safer than set())
        unique_pts = []
        if not pts: return

        unique_pts.append(pts[0])
        for i in range(1, len(pts)):
            curr = np.array(pts[i])
            prev = np.array(unique_pts[-1])
            if np.linalg.norm(curr - prev) > 1e-9: # Deduplication tolerance
                unique_pts.append(pts[i])

        # We don't add the last point if it's the start of the next side
        # (The logic below handles the closed loop explicitly)
        return unique_pts

    # Bottom: varies by x, sort ascending
    b_pts = process_side('bottom', lambda p: p[0])
    # Right: varies by y, sort ascending
    r_pts = process_side('right',  lambda p: p[1])
    # Top: varies by x, sort descending (to keep CCW winding)
    t_pts = process_side('top',    lambda p: p[0], reverse=True)
    # Left: varies by y, sort descending (to keep CCW winding)
    l_pts = process_side('left',   lambda p: p[1], reverse=True)

    # Chain them together.
    # Note: The end of bottom is the start of right, etc.
    # We pop the last element of each segment to avoid double counting corners
    # IF the corners are perfectly aligned.

    # A safer way for MeshPy is simply to dump all points and ensure unique connectivity
    full_loop = []

    # Helper to merge lists while checking corner duplicates
    def merge_lists(source, dest):
        for p in source:
            if not dest:
                dest.append(p)
            else:
                if np.linalg.norm(np.array(p) - np.array(dest[-1])) > 1e-9:
                    dest.append(p)

        # Check wrap-around (last point vs first point)
        if len(dest) > 1 and np.linalg.norm(np.array(dest[-1]) - np.array(dest[0])) < 1e-9:
            dest.pop() # Remove last point if it equals first

    merge_lists(b_pts, full_loop)
    merge_lists(r_pts, full_loop)
    merge_lists(t_pts, full_loop)
    merge_lists(l_pts, full_loop)

    points = list(full_loop)

    # Generate segments for the outer boundary
    # The points are now ordered CCW.
    edges = []
    for i in range(len(points)):
        edges.append((i, (i + 1) % len(points)))

    # 3. Add Obstacles
    holes = []
    for obs in obstacles:
        start_idx = len(points)

        # Ensure obstacle doesn't have duplicate points
        clean_obs = []
        if len(obs) > 0:
            clean_obs.append(obs[0])
            for i in range(1, len(obs)):
                if np.linalg.norm(np.array(obs[i]) - np.array(clean_obs[-1])) > 1e-9:
                    clean_obs.append(obs[i])
            # Check closing
            if np.linalg.norm(np.array(clean_obs[-1]) - np.array(clean_obs[0])) < 1e-9:
                clean_obs.pop()

        points.extend(clean_obs)
        # Connect obstacle vertices
        obs_len = len(clean_obs)
        edges.extend([(start_idx + i, start_idx + (i + 1) % obs_len) for i in range(obs_len)])

        holes.append(find_midpoint_of_polygon(clean_obs))

    # 4. Build Mesh
    info = triangle.MeshInfo()
    info.set_points(points)
    info.set_holes(holes)
    info.set_facets(edges)

    mesh = triangle.build(info, refinement_func=lambda vertices, area: area > area_threshold)

    # 5. Convert to skfem
    P = np.array(mesh.points)
    T = np.array(mesh.elements)
    mesh = MeshTri(P.T, T.T)

    # 6. Create Boundaries
    inlet_dict = {}
    for entry in inlets:
        inlet_dict["inlet" + str(entry["id"])] = get_boundary_lambda(
            entry["side"], entry["coords"], x_min, x_max, y_min, y_max)

    outlet_dict = {}
    for entry in outlets:
        outlet_dict["outlet" + str(entry["id"])] = get_boundary_lambda(
            entry["side"], entry["coords"], x_min, x_max, y_min, y_max)

    obstacle_dict = {"obstacle": lambda x: is_on_obstacle_boundary(x, obstacles)}

    boundary_dict = {**inlet_dict, **outlet_dict, **obstacle_dict}
    mesh = mesh.with_boundaries(boundary_dict)

    return mesh, inlet_dict, outlet_dict, boundary_dict