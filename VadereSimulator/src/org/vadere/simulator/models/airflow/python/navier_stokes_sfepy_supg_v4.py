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

# Assuming these local modules exist in your directory
from build_mesh_sfepy_v2 import build_mesh_and_indices
from vadere_helpers_v2 import *

faulthandler.enable(file=sys.stderr, all_threads=True)

# --- CONFIGURATION ---
target_nu = 1.5e-5
max_iters_final = 20

def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    # 1. SETUP & MESH GENERATION
    # --------------------------
    print("Extracting attributes...")
    geom_data = extract_attributes(args.scenario)

    # Note: For low viscosity, ensure area_threshold is small enough
    geom_data["area_threshold"] = 0.005

    print("Building mesh...")
    mesh, bdry_indices = build_mesh_and_indices(geom_data)

    print(f"Mesh: {mesh.n_nod} nodes, {mesh.n_el} elements.")
    domain = FEDomain('domain', mesh)

    # 2. REGIONS
    # --------------------------
    omega = domain.create_region('Omega', 'all')

    # Boundary Regions Helpers
    def get_inlet_idxs(coors, domain=None): return bdry_indices['inlet']
    def get_outlet_idxs(coors, domain=None): return bdry_indices['outlet']

    inlet_reg = domain.create_region('Inlet', 'vertices by get_inlet_idxs', 'facet',
                                     functions={'get_inlet_idxs': get_inlet_idxs})

    if len(bdry_indices['outlet']) > 0:
        outlet_reg = domain.create_region('Outlet', 'vertices by get_outlet_idxs', 'facet',
                                          functions={'get_outlet_idxs': get_outlet_idxs})
    else:
        outlet_reg = domain.create_region('Outlet', 'vertices by get_outlet_idxs', 'facet',
                                          functions={'get_outlet_idxs': lambda c, d: np.array([], dtype=np.int32)})

    # Calculate Wall Indices
    all_bdry_reg = domain.create_region('AllBoundary', 'vertices of surface', 'facet')
    all_bdry_indices = all_bdry_reg.vertices
    open_bdry_indices = np.union1d(bdry_indices['inlet'], bdry_indices['outlet'])
    wall_indices = np.setdiff1d(all_bdry_indices, open_bdry_indices)

    def get_wall_idxs(coors, domain=None): return wall_indices

    wall_reg = domain.create_region('Walls', 'vertices by get_wall_idxs', 'facet',
                                    functions={'get_wall_idxs': get_wall_idxs})

    # 3. FIELDS (Taylor-Hood P2/P1)
    # --------------------------
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    # 4. BOUNDARY CONDITIONS
    # --------------------------
    inlet_velocity = geom_data['inlet_velocity']
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']
    profile_eps = 1e-3

    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        for i, (x, y) in enumerate(coors[:, :2]):
            if abs(x - x_min) < profile_eps:   val[i, 0] = inlet_velocity
            elif abs(x - x_max) < profile_eps: val[i, 0] = -inlet_velocity
            elif abs(y - y_min) < profile_eps: val[i, 1] = inlet_velocity
            elif abs(y - y_max) < profile_eps: val[i, 1] = -inlet_velocity
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all' : inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all' : 0.0})
    bcs = Conditions([bc_inlet, bc_wall])

    # 5. PREPARE FOR SOLVER LOOP
    # --------------------------
    # Template Variables
    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')
    b = FieldVariable('b', 'parameter', field_u, primary_var_name='u')

    integral = Integral('i', order=3)

    name_map = {
        'u': 'u', 'p': 'p', 'b': 'b', 'v': 'v',
        'velocity': 'fu', 'pressure': 'fp',
        'viscosity': 'viscosity', 'fluid': 'fluid',
        'delta': 'delta', 'tau': 'tau', 'gamma': 'gamma'
    }

    # Initialize 'b' (Linearization)
    u_dof_coords = field_u.get_coor()
    n_dofs = u_dof_coords.shape[0]
    b_data_initial = np.zeros((n_dofs, 2))
    for i in range(n_dofs):
        x, y = u_dof_coords[i, 0], u_dof_coords[i, 1]
        if abs(x - x_max) < profile_eps:   b_data_initial[i, 0] = -inlet_velocity
        elif abs(y - y_min) < profile_eps: b_data_initial[i, 1] = inlet_velocity
        elif abs(x - x_min) < profile_eps: b_data_initial[i, 0] = inlet_velocity
        elif abs(y - y_max) < profile_eps: b_data_initial[i, 1] = -inlet_velocity

    current_u_data = b_data_initial.ravel()

    # 6. VISCOSITY CONTINUATION LOOP
    # --------------------------
    print("\n>>> Starting Robust Viscosity Continuation...")
    viscosity_ramp = [1e-2, 5e-3, 1e-3, 5e-4, 1e-4]#, 5e-5, target_nu]
    final_state = None

    for i, nu_val in enumerate(viscosity_ramp):
        print(f"\n--- Step {i+1}/{len(viscosity_ramp)}: Solving for viscosity = {nu_val} ---")

        # A. Materials
        m_fluid = Material('fluid', values={'viscosity': nu_val})
        stabil_func = Function('stabil_func', StabilizationFunction(name_map))
        m_stabil = Material('stabil', function=stabil_func)

        # B. Terms (Re-bind to new materials)
        t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
        t_conv = Term.new('dw_lin_convect(v, b, u)', integral, omega, v=v, b=b, u=u)
        t_press = Term.new('dw_stokes(v, p)', integral, omega, v=v, p=p)

        t_supg_c = Term.new('dw_st_supg_c(stabil.delta, v, b, u)', integral, omega, stabil=m_stabil, v=v, b=b, u=u)
        t_supg_p = Term.new('dw_st_supg_p(stabil.delta, v, b, p)', integral, omega, stabil=m_stabil, v=v, b=b, p=p)
        t_graddiv = Term.new('dw_st_grad_div(stabil.gamma, v, u)', integral, omega, stabil=m_stabil, v=v, u=u)

        t_div = Term.new('dw_stokes(u, q)', integral, omega, u=u, q=q)
        t_pspg_c = Term.new('dw_st_pspg_c(stabil.tau, q, b, u)', integral, omega, stabil=m_stabil, q=q, b=b, u=u)
        t_pspg_p = Term.new('dw_st_pspg_p(stabil.tau, q, p)', integral, omega, stabil=m_stabil, q=q, p=p)

        # C. Problem Definition
        eq_momentum = Equation('balance', t_diff + t_conv - t_press + t_graddiv + t_supg_c + t_supg_p)
        eq_continuity = Equation('incompressibility', t_div + t_pspg_c + t_pspg_p)

        equations = Equations([eq_momentum, eq_continuity])
        pb = Problem('stabilized_navier_stokes', equations=equations)
        pb.set_bcs(ebcs=bcs)

        # D. Set Linearization Data
        variables = pb.get_variables()
        variables['b'].set_data(np.array(current_u_data).ravel())

        # E. Solver Config
        is_last_step = (i == len(viscosity_ramp) - 1)
        tol_a = 1e-8 if is_last_step else 1e-6
        max_iter = max_iters_final if is_last_step else 15

        conf_oseen = Struct(
            name = 'oseen',
            kind = 'nls.oseen',
            stabil_mat = 'stabil',
            i_max = max_iter,
            eps_a = tol_a,
            eps_r = 1e-4,
            macheps = 1e-16,
            lin_red = 1e-2,
            check_navier_stokes_residual = False,
            problem = pb,
        )

        ls = ScipyDirect({})
        nls = Oseen(conf_oseen, lin_solver=ls, status={})
        nls.conf.problem = pb # Explicit link
        pb.set_solver(nls)

        # F. Solve
        try:
            print(f"   -> Solving...")
            state = pb.solve()
            final_state = state

            # --- FINAL CORRECT EXTRACTION METHOD ---
            # 1. Access the full solution vector (Velocity + Pressure mixed)
            full_vector = state.vec

            # 2. Ask the 'variables' container for the slice indices of 'u'
            #    (Note: We call get_indx('u') on the container, NOT the variable)
            try:
                # Standard SfePy API
                u_slice = variables.get_indx('u')
            except AttributeError:
                # Fallback for alternative SfePy versions
                u_slice = variables.adi.indx['u']

            # 3. Extract exactly the P2 velocity DOFs (19694 values)
            current_u_data = full_vector[u_slice]

            print(f">>> Converged for nu={nu_val}")

        except Exception as e:
            print(f"!!! Solver failed at nu={nu_val}")
            print(f"!!! Details: {e}")
            # If a step fails, we stop, but we keep the result from the previous step
            print("!!! Stopping continuation loop and saving last good result.")
            break

    if final_state is None:
        print("\n!!! FATAL: Solver failed on the very first viscosity step.")
        sys.exit(1)

    # 10. POST PROCESSING
    print("\n>>> Post-processing results...")
    out = final_state.create_output()
    u_vals = out['u'].data

    X, Y, Vx_grid, Vy_grid, vel_mag = postprocess_solution(
        u_vals,
        mesh,
        geom_data['grid_size'],
        x_min, x_max, y_min, y_max
    )

    ny, nx = X.shape
    parameter_string = get_parameter_string(geom_data)

    np.savetxt(args.scenario + '_' + args.hash + '_Vx.txt', Vx_grid,
               header=f'{ny}_{nx}_{parameter_string}')
    np.savetxt(args.scenario + '_' + args.hash + '_Vy.txt', Vy_grid,
               header=f'{ny}_{nx}_{parameter_string}')

    plot_results(mesh, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"])
    print(f"\nDone. Total time: {time.time() - start_time:.2f} s")

if __name__ == '__main__':
    main()