import numpy as np
from matplotlib.tri import LinearTriInterpolator, Triangulation


def define_inlet_basis(basis, mesh, inlets, inlet_velocity):
    inlet_basis = basis['u'].zeros()
    for entry in inlets:
        inlet_facets = mesh.boundaries["inlet" + str(entry["id"])]
        inlet_dofs = basis['u'].get_dofs(facets=inlet_facets)
        inlet_indices_x_global = inlet_dofs.all()[::2]
        inlet_indices_y_global = inlet_dofs.all()[1::2]
        side = entry["side"]
        if side == 'left':
            inlet_basis[inlet_indices_x_global] = inlet_velocity
        elif side == 'right':
            inlet_basis[inlet_indices_x_global] = -inlet_velocity
        elif side == 'bottom':
            inlet_basis[inlet_indices_y_global] = inlet_velocity
        elif side == 'top':
            inlet_basis[inlet_indices_y_global] = -inlet_velocity
    return inlet_basis


def define_dofs(basis, mesh, inlet_dict, outlet_dict):
    inlet_facets = np.concatenate([mesh.boundaries[key] for key in inlet_dict])
    outlet_facets = np.concatenate([mesh.boundaries[key] for key in outlet_dict])
    obstacle_facets = mesh.boundaries['obstacle']
    all_boundary_facets = mesh.boundary_facets()
    non_wall_facets = np.unique(np.concatenate((inlet_facets, outlet_facets, obstacle_facets)))
    wall_facet_indices = np.setdiff1d(all_boundary_facets, non_wall_facets, assume_unique=True)

    inlet_dofs = basis['u'].get_dofs(facets=inlet_facets)
    wall_dofs = basis['u'].get_dofs(facets=wall_facet_indices)
    obstacle_dofs = basis['u'].get_dofs(facets=obstacle_facets)

    D = np.unique(np.concatenate((
        inlet_dofs.all(),
        wall_dofs.all(),
        obstacle_dofs.all()
    )))
    return D


def postprocess_solution(basis, mesh, uv, grid_size, x_min, x_max, y_min, y_max):
    u_nodes = uv[basis['u'].nodal_dofs[0, :]]
    v_nodes = uv[basis['u'].nodal_dofs[1, :]]
    triang = Triangulation(*mesh.p, mesh.t.T)
    interp_u_comp = LinearTriInterpolator(triang, u_nodes)
    interp_v_comp = LinearTriInterpolator(triang, v_nodes)
    X, Y = np.meshgrid(np.arange(x_min, x_max + grid_size, grid_size),
                       np.arange(y_min, y_max + grid_size, grid_size))
    Vx = interp_u_comp(X, Y)
    Vy = interp_v_comp(X, Y)
    mask = np.isnan(Vx) | np.isnan(Vy)
    Vx = np.nan_to_num(Vx)
    Vy = np.nan_to_num(Vy)
    velocity_magnitude = np.ma.masked_array(np.sqrt(Vx ** 2 + Vy ** 2), mask=mask)
    print(f"Max U component on grid: {np.max(Vx):.4f}")
    print(f"Max V component on grid: {np.max(Vy):.4f}")
    print(f"Max velocity magnitude on grid: {np.max(velocity_magnitude):.4f}")

    return X, Y, Vx, Vy, velocity_magnitude
