import json
import numpy as np
from matplotlib.tri import Triangulation, LinearTriInterpolator
from matplotlib import pyplot as plt


def extract_attributes(scenario_file_path):
    """
    Parses the JSON scenario file and returns geometry parameters
    """
    with open(scenario_file_path) as file:
        data = json.load(file)

    topography = data['scenario']['topography']
    attributes_model = data['scenario']['attributesModel']['org.vadere.state.attributes.models.airflow.AttributesAirFlowModel']

    topo_xmin = topography['attributes']['bounds']['x'] + topography['attributes']['boundingBoxWidth']
    topo_ymin = topography['attributes']['bounds']['y'] + topography['attributes']['boundingBoxWidth']
    topo_xmax = topography['attributes']['bounds']['x'] + topography['attributes']['bounds']['width'] - topography['attributes']['boundingBoxWidth']
    topo_ymax = topography['attributes']['bounds']['y'] + topography['attributes']['bounds']['height'] - topography['attributes']['boundingBoxWidth']

    airflow_xmin = attributes_model['bounds']['xmin']
    airflow_xmax = attributes_model['bounds']['xmax']
    airflow_ymin = attributes_model['bounds']['ymin']
    airflow_ymax = attributes_model['bounds']['ymax']

    x_min = max(topo_xmin, airflow_xmin)
    x_max = min(topo_xmax, airflow_xmax)
    y_min = max(topo_ymin, airflow_ymin)
    y_max = min(topo_ymax, airflow_ymax)

    rect_grid_cell_size = float(attributes_model['rectangularGridCellSize'])
    max_triangle_area = float(attributes_model['maxTriangleArea'])
    inlet_velocity = float(attributes_model['inletVelocity'])
    viscosity = float(attributes_model['viscosity'])
    blocking_obstacles = attributes_model['blockingObstacles']

    inlets = []
    for i, ins in enumerate(attributes_model['inlets']):
        inlets.append({
            "id": i,
            "side": ins['side'],
            "coords": [float(ins['start']), float(ins['start'] + ins['width'])]
        })
    if not inlets:
        raise ValueError(f"No inlets defined. The airflow model requires at least one inlet.")

    outlets = []
    for i, outs in enumerate(attributes_model['outlets']):
        outlets.append({
            "id": i,
            "side": outs['side'],
            "coords": [float(outs['start']), float(outs['start'] + outs['width'])]
        })
    if not outlets:
        raise ValueError(f"No outlets defined. The airflow model requires at least one outlet.")

    obstacles = []
    for obstacle in topography['obstacles']:
        if obstacle['id'] in blocking_obstacles:
            if obstacle['shape']['type'] == 'RECTANGLE':
                ob_x_min = obstacle['shape']['x']
                ob_y_min = obstacle['shape']['y']
                ob_x_max = ob_x_min + obstacle['shape']['width']
                ob_y_max = ob_y_min + obstacle['shape']['height']
                obstacles.append([(ob_x_min, ob_y_min), (ob_x_min, ob_y_max),
                                  (ob_x_max, ob_y_max), (ob_x_max, ob_y_min)])

            elif obstacle['shape']['type'] == 'POLYGON':
                obstacles.append([(point['x'], point['y']) for point in obstacle['shape']['points']])

    return {
        'rect_grid_cell_size': rect_grid_cell_size,
        'max_triangle_area': max_triangle_area,
        'x_min': x_min, 'x_max': x_max,
        'y_min': y_min, 'y_max': y_max,
        'inlet_velocity': inlet_velocity,
        'viscosity': viscosity,
        'inlets': inlets,
        'outlets': outlets,
        'obstacles': obstacles
    }


def get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, velocity, eps=1e-3):
    if abs(x - x_min) < eps:   return [velocity, 0.0]
    elif abs(x - x_max) < eps: return [-velocity, 0.0]
    elif abs(y - y_min) < eps: return [0.0, velocity]
    elif abs(y - y_max) < eps: return [0.0, -velocity]
    return [0.0, 0.0]


def get_parameter_string(geom_data):
    parameter_string = f"{geom_data['rect_grid_cell_size']}-{geom_data['max_triangle_area']}-{geom_data['inlet_velocity']}-"

    for item in geom_data['inlets']:
        parameter_string += f"{item['side']}[{item['coords'][0]},{item['coords'][1]}]"
    parameter_string += "-"

    for item in geom_data['outlets']:
        parameter_string += f"{item['side']}[{item['coords'][0]},{item['coords'][1]}]"

    parameter_string += f"-xmin[{geom_data['x_min']}]-xmax[{geom_data['x_max']}]-ymin[{geom_data['y_min']}]-ymax[{geom_data['y_max']}]"

    return parameter_string


def postprocess_solution(u_vals, mesh, rect_grid_cell_size, x_min, x_max, y_min, y_max):
    """
    Converts FEM results on triangular grid to a rectangular grid for Vadere.
    """
    Vx_raw = u_vals[:, 0]
    Vy_raw = u_vals[:, 1]

    elems = mesh.get_conn(mesh.descs[0])
    triang = Triangulation(mesh.coors[:, 0], mesh.coors[:, 1], elems)
    interp_u_comp = LinearTriInterpolator(triang, Vx_raw)
    interp_v_comp = LinearTriInterpolator(triang, Vy_raw)

    eps = 1e-9
    x_rng = np.arange(x_min, x_max + rect_grid_cell_size/100.0, rect_grid_cell_size)
    y_rng = np.arange(y_min, y_max + rect_grid_cell_size/100.0, rect_grid_cell_size)
    X, Y = np.meshgrid(x_rng, y_rng)
    Vx = interp_u_comp(X, Y).filled(0.0)
    Vy = interp_v_comp(X, Y).filled(0.0)
    mask = (Vx == 0.0) & (Vy == 0.0)
    velocity_magnitude = np.sqrt(Vx ** 2 + Vy ** 2)
    masked_mag = np.ma.masked_array(velocity_magnitude, mask=mask)

    print(f"Max U component on grid: {np.max(Vx):.4f}")
    print(f"Max V component on grid: {np.max(Vy):.4f}")
    print(f"Max velocity magnitude on grid: {np.max(velocity_magnitude):.4f}")
    if masked_mag.count() > 0:
        print(f"Average velocity magnitude (fluid only): {np.mean(masked_mag):.4f}")

    return X, Y, Vx, Vy, velocity_magnitude


def plot_results(mesh, X, Y, Vx, Vy, vel_mag, obstacles):
    """
    Plots mesh, streamlines, and vectors side-by-side
    """
    x_min, x_max = np.min(X), np.max(X)
    y_min, y_max = np.min(Y), np.max(Y)
    domain_width = x_max - x_min
    domain_height = y_max - y_min
    aspect = domain_width / domain_height
    plot_height = 15
    plot_width = plot_height * aspect

    fig_width = plot_width * 3
    fig, axes = plt.subplots(1, 3,
                             figsize=(fig_width, plot_height),
                             sharex=True,
                             sharey=True,
                             constrained_layout=True)
    ax_mesh, ax_stream, ax_quiver = axes

    def draw_obstacles(ax):
        for obs in obstacles:
            obs_arr = np.array(obs)
            ax.fill(obs_arr[:, 0], obs_arr[:, 1], color='grey', alpha=1.0, zorder=10)

    # plot 1: mesh
    coords = mesh.coors
    conn = mesh.get_conn(mesh.descs[0])
    ax_mesh.triplot(coords[:, 0], coords[:, 1], conn,
                    color='k', linewidth=0.5, alpha=0.6)
    draw_obstacles(ax_mesh)
    ax_mesh.set_title("Mesh (Triangulation)")
    ax_mesh.set_ylabel("y (m)")

    # plot 2: streamlines
    levels = np.linspace(0, np.max(vel_mag), 50)
    cf1 = ax_stream.contourf(X, Y, vel_mag, levels=levels, cmap='viridis')
    ax_stream.streamplot(X, Y, Vx, Vy, color='white', linewidth=0.5,
                         density=1.5, arrowsize=1, arrowstyle='->')
    draw_obstacles(ax_stream)
    ax_stream.set_title("Streamlines")
    cb1 = fig.colorbar(cf1, ax=ax_stream, location='bottom', fraction=0.05, pad=0.05)
    cb1.set_label('Velocity (m/s)')

    # plot 3: rectangular grid airflow
    cf2 = ax_quiver.contourf(X, Y, vel_mag, levels=levels, cmap='viridis')
    ax_quiver.quiver(X, Y, Vx, Vy, color='white', scale=10, width=0.003, alpha=0.8)
    draw_obstacles(ax_quiver)
    ax_quiver.set_title("Velocity Vectors")
    cb2 = fig.colorbar(cf2, ax=ax_quiver, location='bottom', fraction=0.05, pad=0.05)
    cb2.set_label('Velocity (m/s)')

    for ax in axes:
        ax.set_xlabel("x (m)")
        ax.set_aspect('equal', adjustable='box')
        ax.set_xlim(x_min, x_max)
        ax.set_ylim(y_min, y_max)

    plt.show()