import numpy as np
import meshpy.triangle as triangle
from sfepy.discrete.fem import Mesh


class GeometryBuilder:
    """
    A helper class to manage nodes and edges strictly.
    It prevents duplicate nodes by snapping coordinates to a grid
    and prevents zero-length edges.
    """
    def __init__(self, precision=6):
        self.nodes = []
        self.node_map = {}
        self.edges = set()
        self.precision = precision

    def add_node(self, x, y):
        # round coordinates to handle floating point noise
        key = (round(x, self.precision), round(y, self.precision))

        if key in self.node_map:
            return self.node_map[key]

        idx = len(self.nodes)
        self.nodes.append((x, y))
        self.node_map[key] = idx
        return idx

    def add_segment(self, x1, y1, x2, y2):
        idx1 = self.add_node(x1, y1)
        idx2 = self.add_node(x2, y2)
        if idx1 == idx2:
            return # ignore zero-length segments
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


def build_mesh(geom_data):
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    builder = GeometryBuilder(precision=5)

    def make_side_points(p_start, p_end, fixed_val, axis, side_name):
        pts = [p_start, p_end]

        features = geom_data['inlets'] + geom_data['outlets']
        for feat in features:
            if feat['side'] == side_name:
                c1, c2 = feat['coords']
                if axis == 0: pts.extend([(c1, fixed_val), (c2, fixed_val)])
                else:         pts.extend([(fixed_val, c1), (fixed_val, c2)])

        pts.sort(key=lambda p: p[axis])
        if p_start[axis] > p_end[axis]:
            pts.reverse()
        return pts

    b_pts = make_side_points((x_min, y_min), (x_max, y_min), y_min, 0, 'bottom')
    r_pts = make_side_points((x_max, y_min), (x_max, y_max), x_max, 1, 'right')
    t_pts = make_side_points((x_max, y_max), (x_min, y_max), y_max, 0, 'top')
    l_pts = make_side_points((x_min, y_max), (x_min, y_min), x_min, 1, 'left')

    for i in range(len(b_pts)-1): builder.add_segment(*b_pts[i], *b_pts[i+1])
    for i in range(len(r_pts)-1): builder.add_segment(*r_pts[i], *r_pts[i+1])
    for i in range(len(t_pts)-1): builder.add_segment(*t_pts[i], *t_pts[i+1])
    for i in range(len(l_pts)-1): builder.add_segment(*l_pts[i], *l_pts[i+1])

    holes = []
    for obs_points in geom_data['obstacles']:
        builder.add_loop(obs_points, is_closed=True)
        holes.append(find_hole_point(obs_points))

    info = builder.get_mesh_info(holes)
    mesh_data = triangle.build(info, refinement_func=lambda v, area: area > geom_data['area_threshold'])

    # convert to sfepy
    sfepy_coords = np.array(mesh_data.points)
    sfepy_conns = [np.array(mesh_data.elements)]
    sfepy_mat_ids = [np.zeros(len(mesh_data.elements), dtype=np.int32)]

    mesh = Mesh.from_data('ns_mesh', sfepy_coords, None, sfepy_conns, sfepy_mat_ids, ['2_3'])

    # identify boundary indices
    inlet_indices = set()
    outlet_indices = set()
    eps = 1e-4
    for i, (x, y) in enumerate(sfepy_coords):
        for item in geom_data['inlets']:
            c = item['coords']
            s = item['side']
            if s == 'left' and abs(x - x_min) < eps and c[0]-eps <= y <= c[1]+eps: inlet_indices.add(i)
            elif s == 'right' and abs(x - x_max) < eps and c[0]-eps <= y <= c[1]+eps: inlet_indices.add(i)
            elif s == 'bottom' and abs(y - y_min) < eps and c[0]-eps <= x <= c[1]+eps: inlet_indices.add(i)
            elif s == 'top' and abs(y - y_max) < eps and c[0]-eps <= x <= c[1]+eps: inlet_indices.add(i)

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