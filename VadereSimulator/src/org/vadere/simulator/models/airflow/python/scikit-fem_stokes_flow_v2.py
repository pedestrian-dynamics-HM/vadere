from skfem import *
from skfem.models.poisson import vector_laplace, laplace, mass
from skfem.io import from_meshio
from skfem.visuals.matplotlib import plot, show, draw
from skfem.models.general import divergence, rot
import meshpy.triangle as triangle
from matplotlib import pyplot as plt
from matplotlib.tri import LinearTriInterpolator, Triangulation
from skfem.helpers import dot, grad
import numpy as np
import numpy as np
from functools import partial
import argparse
import json
import scipy.optimize as optim


area_threshold = 0.1
x_min, x_max = 0., 10.
y_min, y_max = 0., 8.
inlet_side = 'left'  # 'left', 'right', 'top', 'bottom'
inlet_coords = (1, 5)
inlet_velocity = 0.1  # in m/s
outlet_side = 'right'  # 'left', 'right', 'top', 'bottom'
outlet_coords = (5, 8)
obstacles = [((4, 5), (5, 6)), ((1, 2), (2, 4))]
grid_size = 0.5

# Geometry construction utility functions
def round_trip_connect(start, end):
    return [(i, i + 1) for i in range(start, end)] + [(end, start)]

def needs_refinement(vertices, area):
    return area > area_threshold

def get_side_coords(side, coords):
    if side == 'left':
        return [(x_min, coords[0]), (x_min, coords[1])]
    elif side == 'right':
        return [(x_max, coords[0]), (x_max, coords[1])]
    elif side == 'top':
        return [(coords[0], y_max), (coords[1], y_max)]
    elif side == 'bottom':
        return [(coords[0], y_min), (coords[1], y_min)]
    else:
        raise ValueError("Unknown side: choose from 'left', 'right', 'bottom', 'top'")

# Building the mesh
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


def build_mesh():
    points = [(x_min, y_min), (x_min, y_max), (x_max, y_max), (x_max, y_min)]
    edges = round_trip_connect(0, len(points) - 1)
    holes = []

    for obs in obstacles:
        obs_start_idx = len(points)
        points.extend(obs)
        edges.extend(round_trip_connect(obs_start_idx, len(points) - 1))
        holes.append(find_midpoint_of_polygon(obs))

    points = np.array(points)
    info = triangle.MeshInfo()
    info.set_points(points.tolist())
    info.set_holes(holes)
    info.set_facets(edges)
    mesh = triangle.build(info, refinement_func=needs_refinement)
    P = np.array(mesh.points)
    T = np.array(mesh.elements)

    return P, T

def get_boundary_lambda(side, coords):
    if side == 'left':
        return lambda x: np.isclose(x[0], x_min) & (x[1] >= coords[0]) & (x[1] <= coords[1])
    elif side == 'right':
        return lambda x: np.isclose(x[0], x_max) & (x[1] >= coords[0]) & (x[1] <= coords[1])
    elif side == 'bottom':
        return lambda x: np.isclose(x[1], y_min) & (x[0] >= coords[0]) & (x[0] <= coords[1])
    elif side == 'top':
        return lambda x: np.isclose(x[1], y_max) & (x[0] >= coords[0]) & (x[0] <= coords[1])
    else:
        raise ValueError("Unknown side: choose from 'left', 'right', 'bottom', 'top'")

def main():
    P, T = build_mesh()
    mesh = (MeshTri(P.T, T.T)
        .with_boundaries({
            'inlet': get_boundary_lambda(inlet_side, inlet_coords),
            'outlet': get_boundary_lambda(outlet_side, outlet_coords)
    }))

    element = {'u': ElementVector(ElementTriP2()),
               'p': ElementTriP1()}
    basis = {variable: Basis(mesh, e, intorder=3)
             for variable, e in element.items()}

    D = basis['u'].get_dofs(['inlet', 'outlet'])

    A = asm(vector_laplace, basis['u'])
    B = -asm(divergence, basis['u'], basis['p'])

    K = bmat([[A, B.T],
              [B, None]], 'csr')

    inlet_basis = basis['u'].zeros()
    inlet_basis[basis['u'].get_dofs("inlet")] = inlet_velocity
    outlet_basis = basis['p'].zeros()
    #outlet_basis[basis['p'].get_dofs("outlet")] = 0
    uvp = np.hstack((
        inlet_basis,
        outlet_basis,
    ))

    uvp = solve(*condense(K, x=uvp, D=D))
    uv, pressure = np.split(uvp, K.blocks)

    #plot(mesh, pressure)
    #plt.show()

    x_coords = mesh.p[0]
    y_coords = mesh.p[1]
    nr_points = mesh.p[0].shape[0]
    u = uv[basis['u'].nodal_dofs.flatten()[:nr_points]]
    v = uv[basis['u'].nodal_dofs.flatten()[nr_points:2*nr_points]]

    # Compute the velocity field manually using finite differences and interpolation
    triang = Triangulation(*mesh.p, mesh.t.T)
    interp_u = LinearTriInterpolator(triang, np.sqrt(u ** 2 + v ** 2))

    X, Y = np.meshgrid(np.arange(x_min, x_max, grid_size), np.arange(y_min, y_max, grid_size))
    # Compute gradient using finite differences
    h = 1e-2
    Vx = -(interp_u(X + h, Y) - interp_u(X - h, Y)) / (2 * h)
    Vy = -(interp_u(X, Y + h) - interp_u(X, Y - h)) / (2 * h)
    velocity = np.sqrt(Vx ** 2 + Vy ** 2)
    print(Vx.max())
    print(velocity.max())

    # Plot the streamlines using ax.streamplot
    #fig, ax = plt.subplots()
    #draw(mesh, ax=ax)
    #ax.streamplot(X, Y, Vx, Vy, color=np.sqrt(Vx**2 + Vy**2), cmap='viridis')
    #ax.set_title('Airflow Streamlines')
    #plt.show()

    # Create square grid
    plt.figure()
    plt.quiver(X, Y, Vx, Vy)
    plt.title('Velocity Field')
    plt.show()
    return Vx, Vy

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    parser.add_argument('grid_size')
    parser.add_argument('area_threshold')
    parser.add_argument('inlet_side')
    parser.add_argument('inlet_start')
    parser.add_argument('inlet_end')
    parser.add_argument('inlet_velocity')
    parser.add_argument('outlet_side')
    parser.add_argument('outlet_start')
    parser.add_argument('outlet_end')
    args = parser.parse_args()
    config = vars(args)

    grid_size = float(config['grid_size'])
    area_threshold = float(config['area_threshold'])
    inlet_side = config['inlet_side']
    inlet_coords = (float(config['inlet_start']), float(config['inlet_end']))
    inlet_velocity = float(config['inlet_velocity'])
    outlet_side = config['outlet_side']
    outlet_coords = (float(config['outlet_start']), float(config['outlet_end']))

    scenario_file_path = config['scenario']
    scenario_hash = config['hash']

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
                obstacles.append([(ob_x_min, ob_y_min), (ob_x_min, ob_y_max), (ob_x_max, ob_y_max), (ob_x_max, ob_y_min)])

            if obstacle['shape']['type'] == 'POLYGON':
                obstacles.append([(point['x'], point['y']) for point in obstacle['shape']['points']])

        Vx, Vy = main()

        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx, header=f'{Vx.shape[0]} {Vx.shape[1]}')
        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy, header=f'{Vy.shape[0]} {Vy.shape[1]}')




