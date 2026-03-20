import json
import numpy as np
from matplotlib.tri import Triangulation, LinearTriInterpolator
from matplotlib import pyplot as plt
from pathlib import Path
import matplotlib.ticker as ticker
import matplotlib.colors as mcolors
from matplotlib.colors import PowerNorm

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
    max_triangle_edge_len = float(attributes_model['maxTriangleEdgeLen'])
    inlet_velocity = float(attributes_model['inletVelocity'])
    reynolds_nr = float(attributes_model['reynoldsNumber'])
    viscosity = convert_reynolds_to_viscosity(reynolds_nr, inlet_velocity, x_min, x_max, y_min, y_max)
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
        'max_triangle_edge_len': max_triangle_edge_len,
        'x_min': x_min, 'x_max': x_max,
        'y_min': y_min, 'y_max': y_max,
        'inlet_velocity': inlet_velocity,
        'viscosity': viscosity,
        'inlets': inlets,
        'outlets': outlets,
        'obstacles': obstacles
    }

def convert_reynolds_to_viscosity(reynolds_nr, inlet_velocity, x_min, x_max, y_min, y_max):
    char_length = ((x_max - x_min) + (y_max - y_min)) / 2
    viscosity = (inlet_velocity * char_length) / reynolds_nr
    return viscosity

def get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, velocity, eps=1e-3):
    if abs(x - x_min) < eps:   return [velocity, 0.0]
    elif abs(x - x_max) < eps: return [-velocity, 0.0]
    elif abs(y - y_min) < eps: return [0.0, velocity]
    elif abs(y - y_max) < eps: return [0.0, -velocity]
    return [0.0, 0.0]


def get_parameter_string(geom_data):
    parameter_string = f"{geom_data['rect_grid_cell_size']}-{geom_data['max_triangle_edge_len']}-{geom_data['inlet_velocity']}-"

    for item in geom_data['inlets']:
        parameter_string += f"{item['side']}[{item['coords'][0]},{item['coords'][1]}]"
    parameter_string += "-"

    for item in geom_data['outlets']:
        parameter_string += f"{item['side']}[{item['coords'][0]},{item['coords'][1]}]"

    parameter_string += f"-xmin[{geom_data['x_min']}]-xmax[{geom_data['x_max']}]-ymin[{geom_data['y_min']}]-ymax[{geom_data['y_max']}]"

    return parameter_string


def postprocess_solution(u_vals, mesh, geom_data):
    """
    Converts FEM results on triangular grid to a rectangular grid for Vadere.
    """
    res = geom_data.get('rect_grid_cell_size', 0.1)
    xmin, xmax = geom_data['x_min'], geom_data['x_max']
    ymin, ymax = geom_data['y_min'], geom_data['y_max']

    nx = int(np.round((xmax - xmin) / res)) + 1
    ny = int(np.round((ymax - ymin) / res)) + 1

    x_rng = np.linspace(xmin, xmax, nx)
    y_rng = np.linspace(ymin, ymax, ny)
    X, Y = np.meshgrid(x_rng, y_rng)

    conn = mesh.get_conn('2_3')
    triang = Triangulation(mesh.coors[:, 0], mesh.coors[:, 1], conn)

    interp_u = LinearTriInterpolator(triang, u_vals[:, 0])
    interp_v = LinearTriInterpolator(triang, u_vals[:, 1])
    Vx = interp_u(X, Y).filled(0.0)
    Vy = interp_v(X, Y).filled(0.0)

    velocity_magnitude = np.hypot(Vx, Vy)
    print(f"Grid: {nx}x{ny} points (res={res}m)")
    print(f"Max velocity magnitude [m/s]: {np.max(velocity_magnitude):.4f} m/s")
    print(f"Average velocity magnitude [m/s]: {np.mean(velocity_magnitude):.4f}")

    return X, Y, Vx, Vy, velocity_magnitude


def get_cache_dir(scenario_path):
    scenario_path = Path(scenario_path)
    parent_dir = scenario_path.parent
    scenario_name = scenario_path.stem
    cache_dir = parent_dir / "cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    return cache_dir, scenario_name


def plot_results(mesh, X, Y, Vx, Vy, vel_mag, obstacles, path):
    """
    Plots mesh, streamlines, and vectors as three separate figures
    """
    # Assuming necessary imports (plt, np, mcolors, PowerNorm, ticker) are above

    base = plt.cm.Blues
    trunc_blues = mcolors.LinearSegmentedColormap.from_list(
        'trunc_blues',
        base(np.linspace(0.3, 1.0, 256))
    )

    plt.rcParams.update({
        "font.size": 16,
        "axes.titlesize": 18,
        "axes.labelsize": 16,
        "xtick.labelsize": 16,
        "ytick.labelsize": 16,
    })

    x_min, x_max = np.min(X), np.max(X)
    y_min, y_max = np.min(Y), np.max(Y)
    domain_width = x_max - x_min
    domain_height = y_max - y_min
    aspect = domain_width / domain_height

    # Set dimensions for a single plot
    plot_height = 7
    plot_width = plot_height * aspect

    def draw_obstacles(ax):
        for obs in obstacles:
            obs_arr = np.array(obs)
            ax.fill(obs_arr[:, 0], obs_arr[:, 1], color='grey', alpha=1.0, zorder=10)

    def format_axes(ax):
        """Applies common formatting to individual axes."""
        ax.set_xlabel("x (m)")
        ax.set_aspect('equal', adjustable='box')
        ax.set_xlim(x_min, x_max)
        ax.set_ylim(y_min, y_max)

    # Figure 1: Mesh
    fig_mesh, ax_mesh = plt.subplots(figsize=(plot_width, plot_height), constrained_layout=True)

    coords = mesh.coors
    conn = mesh.get_conn(mesh.descs[0])
    ax_mesh.triplot(coords[:, 0], coords[:, 1], conn,
                    color='k', linewidth=0.8, alpha=0.9)
    draw_obstacles(ax_mesh)
    #ax_mesh.set_title("Mesh (Triangulation)")
    ax_mesh.set_ylabel("y (m)")
    format_axes(ax_mesh)

    fig_mesh.savefig(path + "_mesh.svg")
    plt.show()
    plt.close(fig_mesh)

    # Figure 2: Streamlines
    fig_stream, ax_stream = plt.subplots(figsize=(plot_width, plot_height), constrained_layout=True)

    levels = np.linspace(0, np.max(vel_mag), 100)
    cf1 = ax_stream.contourf(X, Y, vel_mag, levels=levels, cmap=trunc_blues, norm=PowerNorm(gamma=0.6))
    ax_stream.streamplot(X, Y, Vx, Vy, color='white', linewidth=2.0,
                         density=1.0, arrowsize=2.0, arrowstyle='->')
    draw_obstacles(ax_stream)

    cb1 = fig_stream.colorbar(cf1, ax=ax_stream, location='bottom', fraction=0.05, pad=0.02)
    cb1.set_label('Velocity (m/s)')
    cb1.locator = ticker.MaxNLocator(nbins=6)
    cb1.update_ticks()

    ax_stream.set_ylabel("y (m)")
    format_axes(ax_stream)

    fig_stream.savefig(path + "_streamlines.svg")
    fig_stream.savefig(path + "_streamlines.png")
    plt.show()
    plt.close(fig_stream)

    # Figure 3: Rectangular grid airflow
    fig_quiver, ax_quiver = plt.subplots(figsize=(plot_width, plot_height), constrained_layout=True)

    cf2 = ax_quiver.contourf(X, Y, vel_mag, levels=levels, cmap=trunc_blues, norm=PowerNorm(gamma=0.6))
    ax_quiver.quiver(X, Y, Vx, Vy, color='white', scale=2, width=0.005, alpha=1.0)
    draw_obstacles(ax_quiver)

    cb2 = fig_quiver.colorbar(cf2, ax=ax_quiver, location='bottom', fraction=0.05, pad=0.02)
    cb2.set_label('Velocity (m/s)')
    cb2.locator = ticker.MaxNLocator(nbins=4)
    cb2.update_ticks()

    ax_quiver.set_ylabel("y (m)")
    format_axes(ax_quiver)

    fig_quiver.savefig(path + "_vectors.svg")
    plt.show()
    plt.close(fig_quiver)