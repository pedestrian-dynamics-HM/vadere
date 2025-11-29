import json
import numpy as np
import meshpy.triangle as triangle
from sfepy.discrete.fem import Mesh

# ==========================================
# 1. ROBUST GEOMETRY BUILDER CLASS
# ==========================================
class GeometryBuilder:
    """
    A helper class to manage nodes and edges strictly.
    It prevents duplicate nodes by snapping coordinates to a grid
    and prevents zero-length edges.
    """
    def __init__(self, precision=6):
        self.nodes = []          # List of (x, y)
        self.node_map = {}       # Map "rounded_coord" -> index
        self.edges = set()       # Set of tuples (min_id, max_id) to avoid duplicates
        self.precision = precision

    def add_node(self, x, y):
        # Round coordinates to handle floating point noise
        key = (round(x, self.precision), round(y, self.precision))

        if key in self.node_map:
            return self.node_map[key]

        idx = len(self.nodes)
        self.nodes.append((x, y)) # Store original float (or rounded, doesn't matter much if consistent)
        self.node_map[key] = idx
        return idx

    def add_segment(self, x1, y1, x2, y2):
        idx1 = self.add_node(x1, y1)
        idx2 = self.add_node(x2, y2)

        if idx1 == idx2:
            return # Ignore zero-length segments

        # Store edges as sorted tuples to treat (a,b) same as (b,a)
        edge = tuple(sorted((idx1, idx2)))
        self.edges.add(edge)

    def add_loop(self, points, is_closed=True):
        """Adds a sequence of points as segments."""
        if len(points) < 2: return

        prev_idx = self.add_node(points[0][0], points[0][1])
        first_idx = prev_idx

        for i in range(1, len(points)):
            curr_idx = self.add_node(points[i][0], points[i][1])
            if curr_idx != prev_idx:
                self.edges.add(tuple(sorted((prev_idx, curr_idx))))
                prev_idx = curr_idx

        if is_closed and prev_idx != first_idx:
            self.edges.add(tuple(sorted((prev_idx, first_idx))))

    def get_mesh_info(self, holes):
        info = triangle.MeshInfo()
        info.set_points(self.nodes)
        info.set_facets(list(self.edges))
        info.set_holes(holes)
        return info


def find_hole_point(poly_points):
    """Finds a robust point inside a polygon."""
    arr = np.array(poly_points)
    return np.mean(arr[:, 0]), np.mean(arr[:, 1])

# ==========================================
# 3. MESH GENERATION
# ==========================================
def build_mesh_and_indices(geom_data):
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # Initialize the robust builder
    builder = GeometryBuilder(precision=5) # 5 decimals is plenty for valid geometry

    # --- A. PREPARE BOUNDARY LISTS ---
    # We collect points for each side to ensure Inlets/Outlets are inserted correctly

    # Helper to create sorted points for a side
    def make_side_points(p_start, p_end, fixed_val, axis, side_name):
        # axis: 0 for vary X (bottom/top), 1 for vary Y (left/right)
        pts = [p_start, p_end]

        # Add features (Inlets/Outlets)
        features = geom_data['inlets'] + geom_data['outlets']
        for feat in features:
            if feat['side'] == side_name:
                c1, c2 = feat['coords']
                if axis == 0: pts.extend([(c1, fixed_val), (c2, fixed_val)])
                else:         pts.extend([(fixed_val, c1), (fixed_val, c2)])

        # Sort
        pts.sort(key=lambda p: p[axis])
        # If p_start > p_end (e.g. top or left), reverse
        if p_start[axis] > p_end[axis]:
            pts.reverse()
        return pts

    # Generate points for the 4 sides
    # Bottom (Left -> Right)
    b_pts = make_side_points((x_min, y_min), (x_max, y_min), y_min, 0, 'bottom')
    # Right (Bottom -> Top)
    r_pts = make_side_points((x_max, y_min), (x_max, y_max), x_max, 1, 'right')
    # Top (Right -> Left)
    t_pts = make_side_points((x_max, y_max), (x_min, y_max), y_max, 0, 'top')
    # Left (Top -> Bottom)
    l_pts = make_side_points((x_min, y_max), (x_min, y_min), x_min, 1, 'left')

    # Add these loops to the builder
    # Note: We use is_closed=False and manually chain them to avoid double edges at corners
    for i in range(len(b_pts)-1): builder.add_segment(*b_pts[i], *b_pts[i+1])
    for i in range(len(r_pts)-1): builder.add_segment(*r_pts[i], *r_pts[i+1])
    for i in range(len(t_pts)-1): builder.add_segment(*t_pts[i], *t_pts[i+1])
    for i in range(len(l_pts)-1): builder.add_segment(*l_pts[i], *l_pts[i+1])

    # --- B. ADD OBSTACLES ---
    holes = []
    for obs_points in geom_data['obstacles']:
        # obs_points is a list of (x,y)
        builder.add_loop(obs_points, is_closed=True)
        holes.append(find_hole_point(obs_points))

    # --- C. RUN TRIANGLE ---
    info = builder.get_mesh_info(holes)

    # flags: 'p' (PSLG), 'q' (quality), 'a' (area constraint)
    # The error usually happens here if data is bad
    mesh_data = triangle.build(info, refinement_func=lambda v, area: area > geom_data['area_threshold'])

    # --- D. CONVERT TO SFEPY ---
    sfepy_coords = np.array(mesh_data.points)
    sfepy_conns = [np.array(mesh_data.elements)]
    sfepy_mat_ids = [np.zeros(len(mesh_data.elements), dtype=np.int32)]

    mesh = Mesh.from_data('ns_mesh', sfepy_coords, None, sfepy_conns, sfepy_mat_ids, ['2_3'])

    # --- E. IDENTIFY BOUNDARY INDICES ---
    # We do this AFTER mesh generation on the final nodes
    inlet_indices = set()
    outlet_indices = set()

    eps = 1e-4
    for i, (x, y) in enumerate(sfepy_coords):
        # Check Inlets
        for item in geom_data['inlets']:
            c = item['coords']
            s = item['side']
            if s == 'left' and abs(x - x_min) < eps and c[0]-eps <= y <= c[1]+eps: inlet_indices.add(i)
            elif s == 'right' and abs(x - x_max) < eps and c[0]-eps <= y <= c[1]+eps: inlet_indices.add(i)
            elif s == 'bottom' and abs(y - y_min) < eps and c[0]-eps <= x <= c[1]+eps: inlet_indices.add(i)
            elif s == 'top' and abs(y - y_max) < eps and c[0]-eps <= x <= c[1]+eps: inlet_indices.add(i)

        # Check Outlets
        for item in geom_data['outlets']:
            c = item['coords']
            s = item['side']
            if s == 'left' and abs(x - x_min) < eps and c[0]-eps <= y <= c[1]+eps: outlet_indices.add(i)
            elif s == 'right' and abs(x - x_max) < eps and c[0]-eps <= y <= c[1]+eps: outlet_indices.add(i)
            elif s == 'bottom' and abs(y - y_min) < eps and c[0]-eps <= x <= c[1]+eps: outlet_indices.add(i)
            elif s == 'top' and abs(y - y_max) < eps and c[0]-eps <= x <= c[1]+eps: outlet_indices.add(i)

    return mesh, {
        'inlet': np.array(list(inlet_indices), dtype=np.int32),
        'outlet': np.array(list(outlet_indices), dtype=np.int32)
    }