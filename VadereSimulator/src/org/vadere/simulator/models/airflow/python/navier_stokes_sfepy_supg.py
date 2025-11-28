import os
os.environ['OPENBLAS_NUM_THREADS'] = '1'
os.environ['MKL_NUM_THREADS'] = '1'
os.environ['NUMEXPR_NUM_THREADS'] = '1'

import sys
import time
import numpy as np
import argparse
import json
import faulthandler

from sfepy.base.base import Struct
from sfepy.discrete import (FieldVariable, Material, Integral, Function,
                            Equation, Equations, Problem)
from sfepy.discrete.fem import Mesh, FEDomain, Field
from sfepy.terms import Term
from sfepy.discrete.conditions import Conditions, EssentialBC
from sfepy.solvers.ls import ScipyDirect
from sfepy.solvers.nls import Newton

from build_mesh_sfepy import build_mesh
from vadere_helpers import extract_attributes
from plot_results import plot_results
from matplotlib.tri import Triangulation, LinearTriInterpolator
from skfem import MeshTri

faulthandler.enable(file=sys.stderr, all_threads=True)

# --- CONFIGURATION ---
target_nu = 5e-3  # Realistic air viscosity

# Stabilization parameter computation
class StabilizationFunction:
    """Computes stabilization parameters for SUPG/PSPG/Grad-Div"""

    def __init__(self, gamma=1.0):
        self.gamma = gamma  # Grad-div parameter

    def __call__(self, ts, coors, mode=None, **kwargs):
        """
        Returns stabilization parameters delta (SUPG), tau (PSPG), and gamma (grad-div)
        """
        if mode == 'qp':
            # Get problem context
            equations = kwargs.get('equations')
            if equations is None:
                # Default values if called outside equation context
                delta = 0.1
                tau = 0.1
            else:
                # Extract mesh size and velocity for parameter computation
                # In practice, these should be computed based on local element size
                # and velocity magnitude. Here we use simple estimates.
                h = 0.1  # Characteristic element size (should be computed)
                U = 1.0  # Characteristic velocity magnitude (should be computed)
                nu = target_nu

                # SUPG parameter (streamline diffusion)
                Pe = U * h / (2.0 * nu)  # Peclet number
                if Pe > 1.0:
                    delta = h / (2.0 * U) * (1.0 - 1.0/Pe)
                else:
                    delta = h * h / (4.0 * nu)

                # PSPG parameter (pressure stabilization)
                tau = delta

            # Return as material-like data structure
            out = {
                'delta': delta * np.ones((coors.shape[0], 1, 1)),
                'tau': tau * np.ones((coors.shape[0], 1, 1)),
                'gamma': self.gamma * np.ones((coors.shape[0], 1, 1)),
            }
            return out

        return None


def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()
    config = vars(args)

    scenario_file_path = config['scenario']
    scenario_hash = config['hash']
    parameter_string = ""

    # 1. BUILD MESH
    with open(scenario_file_path) as file:
        data = json.load(file)
        topography = data['scenario']['topography']
        attributes_model = data['scenario']['attributesModel']['org.vadere.state.attributes.models.airflow.AttributesAirFlowModel']
        area_threshold = 0.2
        grid_size, _, x_min, x_max, y_min, y_max, inlet_velocity, inlets, outlets, obstacles, parameter_string = extract_attributes(topography, attributes_model, parameter_string)
        mesh, _, _, _ = build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max)

    print(f"Mesh: {mesh.n_nod} nodes, {mesh.n_el} elements.")
    domain = FEDomain('domain', mesh)
    coords = mesh.coors

    # 2. REGIONS
    epsilon = 1e-3

    def create_region_from_json(name, bdry_list, domain, x_bounds, y_bounds):
        mx_min, mx_max = x_bounds
        my_min, my_max = y_bounds
        def get_indices(coors, domain=None):
            indices = []
            for i, (x, y) in enumerate(coors[:, :2]):
                in_region = False
                for item in bdry_list:
                    side = item['side']
                    c = item['coords']
                    start, end = c[0], c[1]
                    if side == 'left':
                        if (x < mx_min + epsilon) and (y > start - epsilon) and (y < end + epsilon): in_region = True
                    elif side == 'right':
                        if (x > mx_max - epsilon) and (y > start - epsilon) and (y < end + epsilon): in_region = True
                    elif side == 'bottom':
                        if (y < my_min + epsilon) and (x > start - epsilon) and (x < end + epsilon): in_region = True
                    elif side == 'top':
                        if (y > my_max - epsilon) and (x > start - epsilon) and (x < end + epsilon): in_region = True
                    if in_region: break
                if in_region: indices.append(i)
            return np.array(indices, dtype=np.int32)
        fun_name = f'get_{name}_indices'
        return domain.create_region(name, f'vertices by {fun_name}', 'facet', functions={fun_name: get_indices})

    min_x, max_x = np.min(coords[:, 0]), np.max(coords[:, 0])
    min_y, max_y = np.min(coords[:, 1]), np.max(coords[:, 1])

    inlet_reg = create_region_from_json('Inlet', inlets, domain, (min_x, max_x), (min_y, max_y))
    if outlets:
        outlet_reg = create_region_from_json('Outlet', outlets, domain, (min_x, max_x), (min_y, max_y))
    else:
        outlet_reg = domain.create_region('Outlet', f'vertices in (x > {max_x - 0.01})', 'facet')

    all_bdry_reg = domain.create_region('AllBoundary', 'vertices of surface', 'facet')

    inlet_nodes = inlet_reg.vertices
    outlet_nodes = outlet_reg.vertices
    all_bdry_nodes = all_bdry_reg.vertices
    open_boundary_nodes = np.union1d(inlet_nodes, outlet_nodes)
    wall_node_indices = np.setdiff1d(all_bdry_nodes, open_boundary_nodes)

    def get_wall_indices(coors, domain=None): return wall_node_indices
    wall_reg = domain.create_region('Walls', 'vertices by get_wall_indices', 'facet', functions={'get_wall_indices': get_wall_indices})

    print(f"Regions: Inlet={len(inlet_reg.vertices)}, Outlet={len(outlet_reg.vertices)}, Walls={len(wall_reg.vertices)}")

    # 3. FIELDS & VARIABLES
    omega = domain.create_region('Omega', 'all')
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    # State variables
    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')

    # Parameter field for Oseen linearization (advection velocity from previous iteration)
    b = FieldVariable('b', 'parameter', field_u, primary_var_name='(set-to-none)')

    # 4. BOUNDARY CONDITIONS
    profile_eps = 1e-2
    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        for i, (x, y) in enumerate(coors[:, :2]):
            if abs(x - x_max) < profile_eps:   val[i, 0] = -inlet_velocity
            elif abs(y - y_min) < profile_eps: val[i, 1] = inlet_velocity
            elif abs(x - x_min) < profile_eps: val[i, 0] = inlet_velocity
            elif abs(y - y_max) < profile_eps: val[i, 1] = -inlet_velocity
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all' : inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all' : 0.0})
    bc_outlet_p = EssentialBC('OutletPressure', outlet_reg, {'p.0' : 0.0})

    bcs = Conditions([bc_inlet, bc_wall, bc_outlet_p])

    # 5. MATERIALS
    m_fluid = Material('fluid', values={'viscosity': target_nu})

    # Stabilization material (computed via function)
    stabil_func = Function('stabil_func', StabilizationFunction(gamma=1.0))
    m_stabil = Material('stabil', function=stabil_func)

    integral = Integral('i', order=4)

    # ==========================================
    # STABILIZED NAVIER-STOKES WITH OSEEN ITERATION
    # ==========================================
    print("\n>>> Solving Stabilized Navier-Stokes with SUPG/PSPG/Grad-Div...")

    # Define stabilized Navier-Stokes terms
    # CRITICAL: Argument order is (Material, Virtual, Parameter, State) or (Material, Virtual, State)

    # Momentum equation
    t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega,
                      fluid=m_fluid, v=v, u=u)
    t_conv = Term.new('dw_lin_convect(v, b, u)', integral, omega,
                      v=v, b=b, u=u)
    t_press = Term.new('dw_stokes(v, p)', integral, omega,
                       v=v, p=p)

    # Stabilization terms for momentum
    t_supg_c = Term.new('dw_st_supg_c(stabil.delta, v, b, u)', integral, omega,
                        stabil=m_stabil, v=v, b=b, u=u)
    t_supg_p = Term.new('dw_st_supg_p(stabil.delta, v, b, p)', integral, omega,
                        stabil=m_stabil, v=v, b=b, p=p)
    t_graddiv = Term.new('dw_st_grad_div(stabil.gamma, v, u)', integral, omega,
                         stabil=m_stabil, v=v, u=u)

    # Continuity equation
    t_div = Term.new('dw_stokes(u, q)', integral, omega,
                     u=u, q=q)

    # Stabilization terms for continuity
    t_pspg_c = Term.new('dw_st_pspg_c(stabil.tau, q, b, u)', integral, omega,
                        stabil=m_stabil, q=q, b=b, u=u)
    t_pspg_p = Term.new('dw_st_pspg_p(stabil.tau, q, p)', integral, omega,
                        stabil=m_stabil, q=q, p=p)

    # Assemble equations
    eq_momentum = Equation('balance',
                          t_diff + t_conv - t_press + t_graddiv + t_supg_c + t_supg_p)
    eq_continuity = Equation('incompressibility',
                            t_div + t_pspg_c + t_pspg_p)

    equations = Equations([eq_momentum, eq_continuity])

    # Create problem
    pb = Problem('stabilized_navier_stokes', equations=equations)
    pb.set_bcs(ebcs=bcs)

    # Linear solver
    ls = ScipyDirect({})

    # Newton solver with i_max=1 (makes it a linear Oseen solver per iteration)
    nls = Newton({'i_max': 1, 'eps_a': 1e-10}, lin_solver=ls)
    pb.set_solver(nls)

    # Initialize problem
    pb.time_update()

    # Get variables
    variables = pb.get_variables()

    # Initialize state with zero velocity and pressure
    state = pb.create_state()

    # Initialize parameter field b with inlet velocity (initial guess)
    u_dof_coords = field_u.get_coor()
    n_dofs = u_dof_coords.shape[0]

    b_data = np.zeros((n_dofs, 2))

    for i in range(n_dofs):
        # Use u_dof_coords instead of coords
        x, y = u_dof_coords[i, 0], u_dof_coords[i, 1]

        if abs(x - x_max) < profile_eps:   b_data[i, 0] = -inlet_velocity
        elif abs(y - y_min) < profile_eps: b_data[i, 1] = inlet_velocity
        elif abs(x - x_min) < profile_eps: b_data[i, 0] = inlet_velocity
        elif abs(y - y_max) < profile_eps: b_data[i, 1] = -inlet_velocity

    variables['b'].set_data(b_data.ravel())

    # Oseen (Picard) iteration loop
    max_iter = 50
    tol = 1e-4

    print("\n--- Starting Oseen Iterations ---")
    for iteration in range(max_iter):
        print(f"\nIteration {iteration + 1}/{max_iter}")

        # Solve linearized problem (Oseen step)
        state = pb.solve()

        # --- CORRECT DATA EXTRACTION ---
        # 1. Access variable 'u' directly
        # 2. Call it () to get the data
        # 3. Ravel to 1D
        u_vals = state['u']().ravel()

        # Get previous velocity from parameter field b
        u_prev = variables['b']().ravel()

        # Compute change in velocity
        du = np.linalg.norm(u_vals - u_prev) / (np.linalg.norm(u_vals) + 1e-16)

        print(f"  Relative velocity change: {du:.6e}")

        # Update parameter field b with new velocity
        variables['b'].set_data(u_vals)

        # Check convergence
        if du < tol:
            print(f"\n>>> Converged after {iteration + 1} iterations!")
            break
    else:
        print(f"\n>>> Maximum iterations ({max_iter}) reached.")

    # 6. POST PROCESSING
    print("\n>>> Post-processing results...")

    # --- CORRECT BLOCK FOR SECTION 6 ---
    # We use create_output() here. It automatically maps the
    # Quadratic elements (2090 data points) back to the
    # Linear Mesh nodes (280 data points) required for the plotting below.
    out = state.create_output()

    try:
        u_vals = out['u'].data
    except:
        # Fallback for older SfePy versions
        u_vals = out.u.data
    # -----------------------------------

    Vx_raw = u_vals[:, 0]
    Vy_raw = u_vals[:, 1]

    nx, ny = 200, 200
    x_lin = np.linspace(x_min, x_max, nx)
    y_lin = np.linspace(y_min, y_max, ny)
    grid_x, grid_y = np.meshgrid(x_lin, y_lin)

    elems = mesh.get_conn(mesh.descs[0])
    triang = Triangulation(mesh.coors[:,0], mesh.coors[:,1], elems)
    interpolator_Vx = LinearTriInterpolator(triang, Vx_raw)
    interpolator_Vy = LinearTriInterpolator(triang, Vy_raw)

    Vx_grid = interpolator_Vx(grid_x, grid_y).filled(0.0)
    Vy_grid = interpolator_Vy(grid_x, grid_y).filled(0.0)
    vel_mag = np.sqrt(Vx_grid**2 + Vy_grid**2)

    np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx_raw,
               header=f'{Vx_raw.shape[0]}_1_{parameter_string}')
    np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy_raw,
               header=f'{Vy_raw.shape[0]}_1_{parameter_string}')

    sk_p = mesh.coors[:, :2].T
    sk_t = mesh.get_conn(mesh.descs[0]).T
    display_mesh = MeshTri(sk_p, sk_t)

    plot_results(display_mesh, grid_x, grid_y, Vx_grid, Vy_grid, vel_mag, obstacles)
    print(f"\nDone. Total time: {time.time() - start_time:.2f} s")

if __name__ == '__main__':
    main()