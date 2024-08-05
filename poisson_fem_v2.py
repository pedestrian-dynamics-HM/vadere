import numpy as np
import matplotlib.pyplot as plt
import meshpy.triangle as triangle
import scipy.linalg as la
import matplotlib.tri as tr
from mpl_toolkits.axes_grid1 import make_axes_locatable
import argparse
import json

# Parameters
x_min = 0.
x_max = 10.
y_min = 0.
y_max = 8.
area_threshold = 0.1
inlet_side = 'bottom'
outlet_side = 'left'
grid_size = 0.5
inlet_coords = (1, 5)  # Specify inlet width range for top/bottom or height range for left/right
outlet_coords = (2, 4)  # Specify outlet height range for left/right or width range for top/bottom
inlet_velocity = 1.0  # Specify inlet velocity
obstacles = [((4, 5), (5, 6)), ((1, 2), (2, 4))]

def g_N(x, y):
    if outlet_side == 'left' and np.isclose(x, x_min):
        if outlet_coords[0] <= y <= outlet_coords[1]:
            return 1e6
    elif outlet_side == 'right' and np.isclose(x, x_max):
        if outlet_coords[0] <= y <= outlet_coords[1]:
            return 1e6
    elif outlet_side == 'top' and np.isclose(y, y_max):
        if outlet_coords[0] <= x <= outlet_coords[1]:
            return 1e6
    elif outlet_side == 'bottom' and np.isclose(y, y_min):
        if outlet_coords[0] <= x <= outlet_coords[1]:
            return 1e6
    return 0.

def kappa(x, y):
    if inlet_side == 'left' and np.isclose(x, x_min):
        if inlet_coords[0] <= y <= inlet_coords[1]:
            return inlet_velocity
    elif inlet_side == 'right' and np.isclose(x, x_max):
        if inlet_coords[0] <= y <= inlet_coords[1]:
            return inlet_velocity
    elif inlet_side == 'top' and np.isclose(y, y_max):
        if inlet_coords[0] <= x <= inlet_coords[1]:
            return -inlet_velocity  # Negative because fluid is moving into the domain
    elif inlet_side == 'bottom' and np.isclose(y, y_min):
        if inlet_coords[0] <= x <= inlet_coords[1]:
            return inlet_velocity

    return 0.

def round_trip_connect(start, end):
    return [(i, i+1) for i in range(start, end)] + [(end, start)]

def needs_refinement(vertices, area):
    return area > area_threshold

def build_mesh():
    points = [(x_min, y_min), (x_min, y_max), (x_max, y_max), (x_max, y_min)]
    edges = round_trip_connect(0, len(points) - 1)

    for obs in obstacles:
        (x1, y1), (x2, y2) = obs
        rect_points = [(x1, y1), (x1, y2), (x2, y2), (x2, y1)]
        rect_start_idx = len(points)
        points.extend(rect_points)
        edges.extend(round_trip_connect(rect_start_idx, len(points) - 1))

    points = np.array(points)
    info = triangle.MeshInfo()
    info.set_points(points.tolist())
    info.set_holes([((o[0][0] + o[1][0]) / 2, (o[0][1] + o[1][1]) / 2) for o in obstacles])
    info.set_facets(edges)
    mesh = triangle.build(info, refinement_func=needs_refinement)
    P = np.array(mesh.points)
    T = np.array(mesh.elements)

    return P, T

def shoelace(x, y):
    return 0.5 * np.abs(np.dot(x, np.roll(y, 1)) - np.dot(y, np.roll(x, 1)))

def compute_hat_gradients(x, y):
    area = shoelace(x, y)
    b = [(y[1]-y[2]), (y[2]-y[0]), (y[0]-y[1])]/(2.*area)
    c = [(x[2]-x[1]), (x[0]-x[2]), (x[1]-x[0])]/(2.*area)
    return area, b, c

def assemble_stiffness_matrix(P, T):
    nbr_points = len(P)
    nbr_triangles = len(T)
    A = np.zeros((nbr_points, nbr_points))
    for k in range(nbr_triangles):
        loc2glo = T[k, 0:3]
        x = P[loc2glo, 0]
        y = P[loc2glo, 1]
        area, b, c = compute_hat_gradients(x, y)
        local_stiffness_matrix = area * (np.outer(b, b) + np.outer(c, c))
        for i in range(3):
            for j in range(3):
                A[loc2glo[i], loc2glo[j]] += local_stiffness_matrix[i, j]
    return A

def boundary_edges(P, T):
    edges = np.vstack((T[:, [0, 1]], T[:, [0, 2]], T[:, [1, 2]]))
    edges.sort(axis=1)
    edges = edges[~np.all(np.isclose(edges, 0.0), axis=1)]  # Remove zero edges
    _, I, J = np.unique(edges, axis=0, return_index=True, return_inverse=True)
    vec = np.bincount(J)
    Q = np.where(vec == 1)[0]
    e = edges[I[Q]]
    return e

def assemble_boundary_matrix(P, T, kappa):
    nbr_points = len(P)
    E = boundary_edges(P, T)
    R = np.zeros((nbr_points, nbr_points))
    for e in range(len(E)):
        loc2glo = E[e, 0:2]
        x = P[loc2glo, 0]
        y = P[loc2glo, 1]
        length = np.sqrt((x[0] - x[1])**2 + (y[0] - y[1])**2)
        xc = np.mean(x)
        yc = np.mean(y)
        kc = kappa(xc, yc)
        local_boundary_matrix = kc / 6. * length * np.array([[2., 1.], [1., 2.]])
        for i in range(2):
            for j in range(2):
                R[loc2glo[i], loc2glo[j]] += local_boundary_matrix[i, j]
    return R

def assemble_boundary_vector(P, T, g_N):
    nbr_points = len(P)
    E = boundary_edges(P, T)
    r = np.zeros(nbr_points)
    for e in range(len(E)):
        loc2glo = E[e, 0:2]
        x = P[loc2glo, 0]
        y = P[loc2glo, 1]
        length = np.sqrt((x[0] - x[1])**2 + (y[0] - y[1])**2)
        xc = np.mean(x)
        yc = np.mean(y)
        c = g_N(xc, yc)
        local_boundary_vector = c / 2. * length * np.array([1., 1.])
        r[loc2glo] += local_boundary_vector
    return r

def setup_plot():
    ax = plt.gca()
    ax.set_xlim(x_min - 1., x_max + 1.)
    ax.set_ylim(y_min - 1., y_max + 1.)
    ax.set_aspect('equal')
    for obstacle in obstacles:
        rect = plt.Rectangle(obstacle[0],
                             obstacle[1][0] - obstacle[0][0],
                             obstacle[1][1] - obstacle[0][1],
                             fill=False)
        ax.add_artist(rect)
    return ax

def plot(P, T, phi):
    x = P[:, 0]
    y = P[:, 1]

    ax = setup_plot()
    triangulation = tr.Triangulation(x, y, T)
    interpolator = tr.CubicTriInterpolator(triangulation, phi)
    a = np.linspace(x_min, x_max, 200)
    b = np.linspace(y_min, y_max, 100)
    x_, y_ = np.meshgrid(a, b)
    u_x, u_y = interpolator.gradient(x_, y_)
    u = np.sqrt(u_x**2 + u_y**2)

    print(P)
    print(P.shape)
    print(phi)
    print(phi.shape)
    print(x_)
    print(y_)
    print(u_x)
    print(u_y)

    plt.contourf(x_, y_, u, alpha=0.7)
    plt.streamplot(x_, y_, u_x, u_y, color='k', linewidth=1)
    plt.title('Flow Visualization with Streamlines')
    plt.show()

def get_velocity_at_point(x_point, y_point, P, T, phi):
    triangulation = tr.Triangulation(P[:, 0], P[:, 1], np.array(T))
    interpolator = tr.LinearTriInterpolator(triangulation, phi)
    grad_phi_x_interp, grad_phi_y_interp = interpolator.gradient(x_point, y_point)
    velocity = np.array([grad_phi_x_interp, grad_phi_y_interp])
    speed = np.linalg.norm(velocity)
    return velocity, speed


def check_obstacle_coverage(x_min, x_max, y_min, y_max, obstacles):
    covered_area = 0.0
    cell_area = (x_max - x_min) * (y_max - y_min)
    for obs in obstacles:
        (x1, y1), (x2, y2) = obs
        overlap_x = max(0, min(x2, x_max) - max(x1, x_min))
        overlap_y = max(0, min(y2, y_max) - max(y1, y_min))
        covered_area += overlap_x * overlap_y

    return covered_area / cell_area


def create_square_grid_and_calculate_velocity(P, T, phi, obstacles, grid_size):
    x_vals = np.arange(x_min, x_max, grid_size)
    y_vals = np.arange(y_min, y_max, grid_size)
    velocities = np.zeros((len(x_vals), len(y_vals), 2))

    for i, x in enumerate(x_vals):
        for j, y in enumerate(y_vals):
            x_center = x + grid_size / 2
            y_center = y + grid_size / 2

            coverage = check_obstacle_coverage(x, x + grid_size, y, y + grid_size, obstacles)
            if coverage > 0.5:
                velocities[i, j, :] = 0
            else:
                velocities[i, j, :], _ = get_velocity_at_point(x_center, y_center, P, T, phi)

    return x_vals, y_vals, velocities


def main():
    # Execution
    print('Building mesh...', end=' ', flush=True)
    P, T = build_mesh()
    print('Done.')
    print('Assembling linear system...', end=' ', flush=True)
    A = assemble_stiffness_matrix(P, T)
    R = assemble_boundary_matrix(P, T, kappa)
    r = assemble_boundary_vector(P, T, g_N)
    print('Done.')
    print('Solving the linear system...', end=' ', flush=True)
    phi = la.solve(A + R, r)
    print('Done.')

    x_vals, y_vals, velocities = create_square_grid_and_calculate_velocity(P, T, phi, obstacles, grid_size)

    # Visualization (optional)
    plt.figure()
    X, Y = np.meshgrid(x_vals, y_vals)
    U = velocities[:, :, 0] # velocities x direction
    V = velocities[:, :, 1] # velocities y direction
    plt.quiver(X, Y, U.T, V.T)
    plt.title('Velocity Field')
    plt.show()

    return U, V


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('grid_size')
    args = parser.parse_args()
    config = vars(args)

    grid_size = float(config['grid_size'])

    scenario_file_path = config['scenario']

    with open(scenario_file_path) as file:

        data = json.load(file)

        topography = data['scenario']['topography']
        x_min = topography['attributes']['bounds']['x']
        y_min = topography['attributes']['bounds']['y']
        x_max = x_min + topography['attributes']['bounds']['width']
        y_max = x_min + topography['attributes']['bounds']['height']

        obstacles = []
        for obstacle in topography['obstacles']:
            if obstacle['shape']['type'] == 'RECTANGLE':
                ob_x_min = obstacle['shape']['x']
                ob_y_min = obstacle['shape']['y']
                ob_x_max = ob_x_min + obstacle['shape']['width']
                ob_y_max = ob_y_min + obstacle['shape']['height']
                obstacles.append(((ob_x_min, ob_y_min), (ob_x_max, ob_y_max)))

        U, V = main()

        np.savetxt(scenario_file_path + '_U.txt', U, header=f'{U.shape[0]} {U.shape[1]}')
        np.savetxt(scenario_file_path + '_V.txt', V, header=f'{V.shape[0]} {V.shape[1]}')



