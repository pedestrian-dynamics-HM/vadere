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
target_nu = 1.5e-3

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
    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')

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
    m_fluid = Material('fluid', values={'val': [[target_nu]]})
    m_penalty = Material('m_penalty', values={'val': [[1.0]]})
    integral = Integral('i', order=4)

    # ==========================================
    # PHASE 1: STOKES INITIALIZATION
    # ==========================================
    print("\n>>> PHASE 1: Stokes Initialization...")

    # Define Terms specifically for Stokes
    t_div_s = Term.new('dw_div_grad(fluid.val, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
    t_press_s = Term.new('dw_stokes(v, p)', integral, omega, v=v, p=p)
    t_cont_s = Term.new('dw_stokes(u, q)', integral, omega, u=u, q=q)
    t_pen_s = Term.new('dw_volume_dot(m_penalty.val, q, p)', integral, omega, m_penalty=m_penalty, q=q, p=p)

    eq_mom_s = Equation('balance', t_div_s - t_press_s)
    eq_cont_s = Equation('continuity', t_cont_s + 1e-9 * t_pen_s)
    stokes_eqs = Equations([eq_mom_s, eq_cont_s])

    pb_stokes = Problem('stokes', equations=stokes_eqs)
    pb_stokes.set_bcs(ebcs=bcs)

    ls = ScipyDirect({})
    nls_stokes = Newton({'i_max': 1, 'eps_a': 1e-10}, lin_solver=ls)
    pb_stokes.set_solver(nls_stokes)

    # Solve Stokes
    pb_stokes.time_update()
    state_stokes = pb_stokes.solve()
    print(">>> Stokes solution found.")

    # ==========================================
    # PHASE 2: NAVIER-STOKES RAMPING
    # ==========================================
    print(f"\n>>> PHASE 2: Navier-Stokes Ramping...")

    # Define NEW Terms for Navier-Stokes (Creates fresh internal state)
    t_div_ns = Term.new('dw_div_grad(fluid.val, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
    t_conv_ns = Term.new('dw_convect(v, u)', integral, omega, v=v, u=u)
    t_press_ns = Term.new('dw_stokes(v, p)', integral, omega, v=v, p=p)
    t_cont_ns = Term.new('dw_stokes(u, q)', integral, omega, u=u, q=q)
    t_pen_ns = Term.new('dw_volume_dot(m_penalty.val, q, p)', integral, omega, m_penalty=m_penalty, q=q, p=p)

    eq_mom_ns = Equation('balance', t_div_ns + t_conv_ns - t_press_ns)
    eq_cont_ns = Equation('continuity', t_cont_ns + 1e-9 * t_pen_ns)
    ns_eqs = Equations([eq_mom_ns, eq_cont_ns])

    # Create NEW Problem
    pb_ns = Problem('navier_stokes', equations=ns_eqs)
    pb_ns.set_bcs(ebcs=bcs)

    nls_status = {}
    nls_ns = Newton({'i_max': 20, 'eps_a': 1e-3, 'eps_r': 1e-3}, lin_solver=ls, status=nls_status)
    pb_ns.set_solver(nls_ns)

    # 1. Initialize Structure
    pb_ns.time_update()

    # 2. FIX: Force memory allocation for variables
    # This ensures variables_ns.vec is not None
    _ = pb_ns.create_state()

    # 3. Transfer Stokes solution
    variables_ns = pb_ns.get_variables()
    variables_ns.set_state(state_stokes.vec)

    # Access material for manual updating from the NEW equations
    mat_fluid = ns_eqs[0].terms['dw_div_grad'].get_materials(join=True)[0]

    steps = 20
    nus = np.logspace(np.log10(0.1), np.log10(target_nu), steps)

    state_final = state_stokes

    for i, nu_val in enumerate(nus):
        print(f"\n--- Step {i+1}/{steps}: Solving for nu = {nu_val:.2e} ---")

        # Update Viscosity
        mat_fluid.datas['special']['val'] = np.array([[[[nu_val]]]])
        mat_fluid.reset()

        # Use previous solution as guess
        variables_ns.set_state(state_final.vec)

        try:
            state_final = pb_ns.solve()
            print(f"  > Success. Residual: {nls_status.get('err', 'N/A')}")
        except Exception as e:
            print(f"  > Failed at {nu_val}. Using previous result.")
            break

    # 8. POST PROCESSING
    out = state_final.create_output()
    try: u_vals = out['u'].data
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

    np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx_raw, header=f'{Vx_raw.shape[0]}_1_{parameter_string}')
    np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy_raw, header=f'{Vy_raw.shape[0]}_1_{parameter_string}')

    sk_p = mesh.coors[:, :2].T
    sk_t = mesh.get_conn(mesh.descs[0]).T
    display_mesh = MeshTri(sk_p, sk_t)

    plot_results(display_mesh, grid_x, grid_y, Vx_grid, Vy_grid, vel_mag, obstacles)
    print(f"\nDone. Total time: {time.time() - start_time:.2f} s")

if __name__ == '__main__':
    main()