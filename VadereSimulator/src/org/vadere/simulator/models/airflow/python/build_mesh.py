import meshpy.triangle as triangle
from skfem import *
import numpy as np
import scipy.optimize as optim


def round_trip_connect(start, end):
    """Connects points in a loop."""
    return [(i, i + 1) for i in range(start, end)] + [(end, start)]

def get_side_coords(side, coords, x_min, x_max, y_min, y_max):
    """Gets coordinates for boundary segments."""
    if side == 'left': return [(x_min, coords[0]), (x_min, coords[1])]
    elif side == 'right': return [(x_max, coords[0]), (x_max, coords[1])]
    elif side == 'top': return [(coords[0], y_max), (coords[1], y_max)]
    elif side == 'bottom': return [(coords[0], y_min), (coords[1], y_min)]
    else: raise ValueError("Unknown side: choose from 'left', 'right', 'bottom', 'top'")

def get_boundary_lambda(side, coords, x_min, x_max, y_min, y_max):
    """Returns lambda function to identify boundary facets."""
    domain_size = min(x_max - x_min, y_max - y_min)
    tol = domain_size * 1e-6
    if side == 'left': return lambda x: np.isclose(x[0], x_min) & (x[1] >= coords[0 ] -tol) & (x[1] <= coords[1 ] +tol)
    elif side == 'right': return lambda x: np.isclose(x[0], x_max) & (x[1] >= coords[0 ] -tol) & \
                    (x[1] <= coords[1] + tol)  
    elif side == 'bottom':
        return lambda x: np.isclose(x[1], y_min) & (x[0] >= coords[0] - tol) & (x[0] <= coords[1] + tol)
    elif side == 'top':
        return lambda x: np.isclose(x[1], y_max) & (x[0] >= coords[0] - tol) & (x[0] <= coords[1] + tol)
    else:
        raise ValueError("Unknown side")


def is_on_obstacle_boundary(x, obstacles_arg):
    """
    Checks if points x lie on the boundary of any polygon in obstacles_arg.
    Each polygon in obstacles_arg is a list of (x,y) vertex tuples.
    x: A (2, num_points) array of point coordinates to check.
    obstacles_arg: A list of polygons. Each polygon is a list of (x, y) vertices.
    """
    on_any_polygon_segment = np.zeros(x.shape[1], dtype=bool)
    tol = 1e-9
    t_tol = 1e-7

    for polygon_vertices in obstacles_arg:
        num_poly_verts = len(polygon_vertices)
        if num_poly_verts < 2:
            continue

        for i in range(num_poly_verts):
            p_start = np.array(polygon_vertices[i])
            p_end = np.array(polygon_vertices[(i + 1) % num_poly_verts])
            seg_vec_dx = p_end[0] - p_start[0]
            seg_vec_dy = p_end[1] - p_start[1]

            seg_length_squared = seg_vec_dx**2 + seg_vec_dy**2

            if seg_length_squared < tol**2:
                dist_sq_to_p_start = (x[0] - p_start[0])**2 + (x[1] - p_start[1])**2
                is_near_point_segment = dist_sq_to_p_start < tol**2
                on_any_polygon_segment |= is_near_point_segment
                continue

            pt_vec_dx = x[0] - p_start[0]
            pt_vec_dy = x[1] - p_start[1]
            dot_product = pt_vec_dx * seg_vec_dx + pt_vec_dy * seg_vec_dy
            t = dot_product / seg_length_squared
            projects_onto_segment = (t >= -t_tol) & (t <= 1 + t_tol)
            dist_sq_numerator = (pt_vec_dx * seg_vec_dy - pt_vec_dy * seg_vec_dx)**2
            dist_sq_to_infinite_line = dist_sq_numerator / seg_length_squared
            is_on_this_segment = projects_onto_segment & (dist_sq_to_infinite_line < tol**2)

            on_any_polygon_segment |= is_on_this_segment

    return on_any_polygon_segment


def find_midpoint_of_polygon(obstacle):
    obstacle.append(obstacle[0])
    c = [0, 0, -1]
    a_x, a_y, a_r, b = [], [], [], []
    for i in range(len(obstacle) - 1):
        n = (obstacle[i][1] - obstacle[i+1][1], obstacle[i+1][0] - obstacle[i][0])
        n = n / np.linalg.norm(n)
        n = -n
        a_x.append(-n[0])
        a_y.append(-n[1])
        a_r.append(1)
        b.append(-n[0] * obstacle[i][0] - n[1] * obstacle[i][1])
    result = optim.linprog(c=c, A_ub=np.stack((a_x, a_y, a_r), axis=1), b_ub=b)
    return result.x[0], result.x[1]


def build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max):
    """Builds the mesh using meshpy.triangle."""
    points = [(x_min, y_min), (x_min, y_max), (x_max, y_max), (x_max, y_min)]
    edges = round_trip_connect(0, len(points) - 1)
    holes = []
    for obs in obstacles:
        obs_start_idx = len(points)
        points.extend(obs)
        edges.extend(round_trip_connect(obs_start_idx, len(points) - 1))
        holes.append(find_midpoint_of_polygon(obs))

    for entry in inlets: points.extend(get_side_coords(entry["side"], entry["coords"],
                                                       x_min, x_max, y_min, y_max))
    for entry in outlets: points.extend(get_side_coords(entry["side"], entry["coords"],
                                                        x_min, x_max, y_min, y_max))

    info = triangle.MeshInfo()
    info.set_points(points)
    info.set_holes(holes)
    info.set_facets(edges)

    mesh = triangle.build(info, refinement_func=lambda vertices, area: area > area_threshold)
    P = np.array(mesh.points)
    T = np.array(mesh.elements)
    mesh = MeshTri(P.T, T.T)

    inlet_dict = {}
    for entry in inlets: inlet_dict.update(
        {"inlet" + str(entry["id"]): get_boundary_lambda(entry["side"], entry["coords"],
                                                         x_min, x_max, y_min, y_max)})
    outlet_dict = {}
    for entry in outlets: outlet_dict.update(
        {"outlet" + str(entry["id"]): get_boundary_lambda(entry["side"], entry["coords"],
                                                          x_min, x_max, y_min, y_max)})
    obstacle_dict = {"obstacle": lambda x: is_on_obstacle_boundary(x, obstacles)}
    boundary_dict = {**inlet_dict, **outlet_dict, **obstacle_dict}
    mesh = mesh.with_boundaries(boundary_dict)

    return mesh, inlet_dict, outlet_dict, boundary_dict
