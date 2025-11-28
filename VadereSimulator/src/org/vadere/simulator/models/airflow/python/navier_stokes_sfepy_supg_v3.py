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
from sfepy.solvers.oseen import Oseen, StabilizationFunction

from build_mesh_sfepy import build_mesh
from vadere_helpers import extract_attributes
from plot_results import plot_results
from matplotlib.tri import Triangulation, LinearTriInterpolator
from skfem import MeshTri

faulthandler.enable(file=sys.stderr, all_threads=True)

# --- CONFIGURATION ---
# We will ramp down to this target
FINAL_TARGET_NU = 1e-2

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

        # Keep your fine threshold here (0.2 is fine if we use ramping)
        area_threshold = 0.025

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

    # 3. FIELDS & VARIABLES
    omega = domain.create_region('Omega', 'all')
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=1)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')
    b = FieldVariable('b', 'parameter', field_u, primary_var_name='u')

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

    # 5. PREPARE INITIAL GUESS (Calculated once)
    # We calculate the inlet profile once to use as the starting guess
    # for the very first iteration.
    print(">>> Preparing initial guess...")
    b_data_initial = np.zeros((mesh.n_nod, 2))
    for i in range(mesh.n_nod):
        x, y = coords[i, 0], coords[i, 1]
        if abs(x - x_max) < profile_eps:   b_data_initial[i, 0] = -inlet_velocity
        elif abs(y - y_min) < profile_eps: b_data_initial[i, 1] = inlet_velocity
        elif abs(x - x_min) < profile_eps: b_data_initial[i, 0] = inlet_velocity
        elif abs(y - y_max) < profile_eps: b_data_initial[i, 1] = -inlet_velocity

    # ====================================================
    # 6. VISCOSITY CONTINUATION LOOP
    # ====================================================
    viscosity_steps = [FINAL_TARGET_NU * 20, FINAL_TARGET_NU * 5, FINAL_TARGET_NU]

    # Variable to store the solution from the previous step
    previous_solution = None

    print("\n>>> Starting Viscosity Continuation...")

    for i, nu_step in enumerate(viscosity_steps):
        print(f"\n==================================================")
        print(f">>> Step {i+1}/3: Solving for viscosity = {nu_step:.5f}")
        print(f"==================================================")

        # --- A. DEFINE MATERIALS ---
        m_fluid = Material('fluid', values={'viscosity': nu_step})

        name_map = {
            'u': 'u', 'p': 'p', 'b': 'b', 'v': 'v',
            'velocity': 'fu', 'pressure': 'fp',
            'viscosity': 'viscosity', 'fluid': 'fluid',
            'delta': 'delta', 'tau': 'tau', 'gamma': 'gamma'
        }
        stabil_func = Function('stabil_func', StabilizationFunction(name_map))
        m_stabil = Material('stabil', function=stabil_func)

        # --- B. DEFINE TERMS & EQUATIONS ---
        integral = Integral('i', order=3)

        t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega,
                          fluid=m_fluid, v=v, u=u)
        t_conv = Term.new('dw_lin_convect(v, b, u)', integral, omega,
                          v=v, b=b, u=u)
        t_press = Term.new('dw_stokes(v, p)', integral, omega,
                           v=v, p=p)
        t_supg_c = Term.new('dw_st_supg_c(stabil.delta, v, b, u)', integral, omega,
                            stabil=m_stabil, v=v, b=b, u=u)
        t_supg_p = Term.new('dw_st_supg_p(stabil.delta, v, b, p)', integral, omega,
                            stabil=m_stabil, v=v, b=b, p=p)
        t_graddiv = Term.new('dw_st_grad_div(stabil.gamma, v, u)', integral, omega,
                             stabil=m_stabil, v=v, u=u)
        t_div = Term.new('dw_stokes(u, q)', integral, omega,
                         u=u, q=q)
        t_pspg_c = Term.new('dw_st_pspg_c(stabil.tau, q, b, u)', integral, omega,
                            stabil=m_stabil, q=q, b=b, u=u)
        t_pspg_p = Term.new('dw_st_pspg_p(stabil.tau, q, p)', integral, omega,
                            stabil=m_stabil, q=q, p=p)

        eq_momentum = Equation('balance', t_diff + t_conv - t_press + t_graddiv + t_supg_c + t_supg_p)
        eq_continuity = Equation('incompressibility', t_div + t_pspg_c + t_pspg_p)
        equations = Equations([eq_momentum, eq_continuity])

        # --- C. DEFINE PROBLEM ---
        pb = Problem('stabilized_navier_stokes', equations=equations)
        pb.set_bcs(ebcs=bcs)

        # --- FIX: ALLOCATE MEMORY ---
        pb.time_update()
        # CRITICAL LINE: This forces SfePy to create the arrays for u and p
        _ = pb.create_state()
        # ----------------------------

        # --- D. SET INITIAL GUESS ---
        variables = pb.get_variables()

        if previous_solution is None:
            # First run: Use the manual inlet profile for 'b'
            variables['b'].set_data(b_data_initial.ravel())
        else:
            # Subsequent runs: Use the results from the previous viscosity step
            print("    (Initializing with solution from previous step)")

            # 1. Force the calculated state (u, p) into the allocated memory
            variables.set_state(previous_solution, force=True)

            # 2. Update the advection parameter (b) to match (u)
            u_current = variables['u']().ravel()
            variables['b'].set_data(u_current)

        # --- E. SOLVE ---
        ls = ScipyDirect({})
        conf_oseen = Struct(
            name = 'oseen',
            kind = 'nls.oseen',
            stabil_mat = 'stabil',
            i_max = 50,
            eps_a = 1e-6,
            eps_r = 1.0,
            macheps = 1e-16,
            lin_red = 1e-2,
            check_navier_stokes_residual = False,
            problem = pb
        )

        nls = Oseen(conf_oseen, lin_solver=ls, status={})
        nls.conf.problem = pb
        pb.set_solver(nls)

        state = pb.solve()

        # Save state for next iteration
        previous_solution = state()

    print(">>> Continuation Converged.")

    # 7. POST PROCESSING
    print("\n>>> Post-processing results...")

    out = state.create_output()
    try:    u_vals = out['u'].data
    except: u_vals = out.u.data

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