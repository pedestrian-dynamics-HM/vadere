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

area_threshold = 0.5
x_min, x_max = 0., 10.
y_min, y_max = 0., 8.
inlets = [{"id": 0, "side": "left", "coords": [1,3]},
          {"id": 1, "side": "top", "coords": [4,5]}]
inlet_velocity = 0.1 # in m/s
outlets = [{"id": 0, "side": "right", "coords": [5,8]},
          {"id": 1, "side": "bottom", "coords": [5,6]}]
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

    for entry in inlets:
        points.extend(get_side_coords(entry["side"], entry["coords"]))
    for entry in outlets:
        points.extend(get_side_coords(entry["side"], entry["coords"]))

    # points = np.array(points)
    info = triangle.MeshInfo()
    info.set_points(points)
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


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()
    config = vars(args)

    scenario_file_path = config['scenario']
    scenario_hash = config['hash']

    parameter_string = ""

    with open(scenario_file_path) as file:

        data = json.load(file)
        topography = data['scenario']['topography']

        bounding_box_width = topography['attributes']['boundingBoxWidth']

        x_min = topography['attributes']['bounds']['x'] + bounding_box_width
        y_min = topography['attributes']['bounds']['y'] + bounding_box_width
        x_max = topography['attributes']['bounds']['width'] - bounding_box_width
        y_max = topography['attributes']['bounds']['height'] - bounding_box_width
        attributes_model = data['scenario']['attributesModel']['org.vadere.state.attributes.models.airflow.AttributesAirFlowModel']

        grid_size = float(attributes_model['gridSize'])
        area_threshold = float(attributes_model['areaThreshold'])
        inlet_velocity = float(attributes_model['inletVelocity'])
        blocking_obstacles = attributes_model['blockingObstacles']

        parameter_string = parameter_string + str(attributes_model['gridSize']) + "-" + str(attributes_model['areaThreshold']) + "-" + str(attributes_model['inletVelocity']) + "-"

        inlets = []
        outlets = []
        for i, ins in enumerate(attributes_model['inlets']):
            parameter_string = parameter_string + ins['side'] + "[" + str(ins['start']) +","+ str(ins['end']) + "]"
            inlets.append({"id": i, "side": ins['side'], "coords": [float(ins['start']), float(ins['end'])]})
        parameter_string = parameter_string + "-"
        for i, outs in enumerate(attributes_model['outlets']):
            parameter_string = parameter_string + outs['side'] + "[" + str(outs['start'])+","+  str(outs['end']) + "]"
            outlets.append({"id": i, "side": outs['side'], "coords": [float(outs['start']), float(outs['end'])]})

        parameter_string = parameter_string + "-" + str(blocking_obstacles)

        obstacles = []
        for obstacle in topography['obstacles']:
            if obstacle['id'] in blocking_obstacles:
                if obstacle['shape']['type'] == 'RECTANGLE':
                    ob_x_min = obstacle['shape']['x']
                    ob_y_min = obstacle['shape']['y']
                    ob_x_max = ob_x_min + obstacle['shape']['width']
                    ob_y_max = ob_y_min + obstacle['shape']['height']
                    obstacles.append([(ob_x_min, ob_y_min), (ob_x_min, ob_y_max), (ob_x_max, ob_y_max), (ob_x_max, ob_y_min)])

                if obstacle['shape']['type'] == 'POLYGON':
                    obstacles.append([(point['x'], point['y']) for point in obstacle['shape']['points']])

    inlet_dict = {}
    for entry in inlets:
        inlet_dict.update({"inlet"+str(entry["id"]): get_boundary_lambda(entry["side"], entry["coords"])})
    outlet_dict = {}
    for entry in outlets:
        outlet_dict.update({"outlet"+str(entry["id"]): get_boundary_lambda(entry["side"], entry["coords"])})
    boundary_dict = inlet_dict | outlet_dict

    P, T = build_mesh()

    mesh = (MeshTri(P.T, T.T).with_boundaries(boundary_dict))

    element = {'u': ElementVector(ElementTriP2()),
               'p': ElementTriP1()}
    basis = {variable: Basis(mesh, e, intorder=3)
             for variable, e in element.items()}

    D = basis['u'].get_dofs([*boundary_dict])

    A = asm(vector_laplace, basis['u'])
    B = -asm(divergence, basis['u'], basis['p'])

    K = bmat([[A, B.T],
              [B, None]], 'csr')

    inlet_basis = basis['u'].zeros()
    inlet_basis[basis['u'].get_dofs([*inlet_dict])] = inlet_velocity
    outlet_basis = basis['p'].zeros()

    uvp = np.hstack((
        inlet_basis,
        outlet_basis,
    ))

    uvp = solve(*condense(K, x=uvp, D=D))
    uv, pressure = np.split(uvp, K.blocks)

    x_coords = mesh.p[0]
    y_coords = mesh.p[1]
    nr_points = mesh.p[0].shape[0]
    u = uv[basis['u'].nodal_dofs.flatten()[:nr_points]]
    v = uv[basis['u'].nodal_dofs.flatten()[nr_points:2*nr_points]]

    print(u.shape)
    print(v.shape)

    # Compute the velocity field manually using finite differences and interpolation
    triang = Triangulation(*mesh.p, mesh.t.T)

    #print(triang)
    #plt.triplot(triang, marker="o")
    #plt.show()


    interp_u = LinearTriInterpolator(triang, np.sqrt(u ** 2 + v ** 2))

    X, Y = np.meshgrid(np.arange(x_min + grid_size/2, x_max, grid_size), np.arange(y_min + grid_size/2, y_max, grid_size))
    # Compute gradient using finite differences
    h = 1e-2


    Vx = -(interp_u(X + h, Y) - interp_u(X - h, Y)) / (2 * h)
    Vy = -(interp_u(X, Y + h) - interp_u(X, Y - h)) / (2 * h)

    velocity = np.sqrt(Vx ** 2 + Vy ** 2)
    # print(Vx.max())
    # print(velocity.max())

    # Create square grid
    # plt.figure(figsize=(x_length,y_length))
    plt.figure()
    plt.gca().set_aspect('equal')
    plt.quiver(X, Y, Vx, Vy)
    plt.title('Velocity Field')
    plt.close()
    plt.show()

    np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx, header=f'{Vx.shape[0]}_{Vx.shape[1]}_{parameter_string}')
    np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy, header=f'{Vy.shape[0]}_{Vy.shape[1]}_{parameter_string}')
