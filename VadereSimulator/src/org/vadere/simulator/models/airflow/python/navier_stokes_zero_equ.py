import os
os.environ['OPENBLAS_NUM_THREADS'] = '1'
os.environ['MKL_NUM_THREADS'] = '1'
os.environ['NUMEXPR_NUM_THREADS'] = '1'
os.environ['OMP_NUM_THREADS'] = '1'

import sys
import time
import numpy as np
import argparse
import faulthandler
from scipy.spatial import cKDTree

from sfepy.base.base import Struct
from sfepy.discrete import (FieldVariable, Material, Integral, Function,
                            Equation, Equations, Problem)
from sfepy.discrete.fem import FEDomain, Field
from sfepy.terms import Term
from sfepy.discrete.conditions import Conditions, EssentialBC
from sfepy.solvers.ls import ScipyDirect
from sfepy.solvers.oseen import Oseen, StabilizationFunction

from build_mesh import build_mesh
from helpers import *

faulthandler.enable(file=sys.stderr, all_threads=True)

# --- HELPER FUNCTIONS ---
def compute_wall_distance(mesh, wall_indices):
    wall_coors = mesh.coors[wall_indices]
    tree = cKDTree(wall_coors)
    dists, _ = tree.query(mesh.coors, k=1)
    return dists

def turbulence_viscosity_fun(ts=None, coors=None, mode=None, **kwargs):
    # 1. Get Base Viscosity
    base_viscosity = kwargs.get('base_viscosity', 1e-3)

    # 2. Handle Initialization/Setup calls (SfePy calls this with coors=None)
    if mode != 'qp' or coors is None:
        return {'viscosity': np.array([[[base_viscosity]]])}

    # 3. Get Variables
    u_field_var = kwargs.get('u_var')
    dist_field_var = kwargs.get('dist_var')

    # 4. Get Wall Distance & Clamp
    d = dist_field_var.evaluate_at(coors)
    d = np.maximum(d, 0.0)

    # 5. Get Velocity Gradients from PREVIOUS iteration
    # returns shape (n_qp, n_ref, dim, dim) -> flattened to (n_total, dim, dim)
    grad_u = u_field_var.evaluate_at(coors, mode='grad')

    # 6. Calculate Strain Rate |S|
    # S = 0.5 * (grad_u + grad_u.T)
    S = 0.5 * (grad_u + np.swapaxes(grad_u, 1, 2))

    # Double contraction: S_ij * S_ij
    S_contract = np.einsum('ijk,ijk->i', S, S)

    # Safe Sqrt
    norm_S = np.sqrt(2.0 * S_contract)
    norm_S = norm_S.reshape(-1, 1, 1)

    # 7. Mixing Length Model
    kappa = 0.41
    l_m = kappa * d

    # Eddy Viscosity: nu_t = l_m^2 * |S|
    nu_t = (l_m**2) * norm_S

    # 8. SAFETY: Clamp and Clean
    # Prevent infinite viscosity spikes. Cap at 1000x base viscosity.
    nu_t = np.nan_to_num(nu_t, nan=0.0, posinf=0.0, neginf=0.0)
    nu_t = np.clip(nu_t, 0, 1000.0 * base_viscosity)

    # 9. Effective Viscosity
    nu_eff = base_viscosity + nu_t

    return {'viscosity': nu_eff}

# --- MAIN ---
def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    # SETUP & MESH GENERATION
    geom_data = extract_attributes(args.scenario)
    base_viscosity = geom_data.get("viscosity", 1e-3)

    mesh, bdry_indices = build_mesh(geom_data)

    print(f"Mesh: {mesh.n_nod} nodes, {mesh.n_el} elements.")
    mesh_time = time.time()

    domain = FEDomain('domain', mesh)

    # REGIONS
    omega = domain.create_region('Omega', 'all')
    all_bdry_reg = domain.create_region('AllBoundary', 'vertices of surface', 'facet')
    all_bdry_indices = all_bdry_reg.vertices

    def get_inlet_idxs(coors, domain=None): return bdry_indices['inlet']
    def get_outlet_idxs(coors, domain=None): return bdry_indices['outlet']

    inlet_reg = domain.create_region('Inlet', 'vertices by get_inlet_idxs', 'facet',
                                     functions={'get_inlet_idxs': get_inlet_idxs})
    outlet_reg = domain.create_region('Outlet', 'vertices by get_outlet_idxs', 'facet',
                                      functions={'get_outlet_idxs': get_outlet_idxs})

    open_bdry_indices = np.union1d(bdry_indices['inlet'], bdry_indices['outlet'])
    wall_indices = np.setdiff1d(all_bdry_indices, open_bdry_indices)

    def get_wall_idxs(coors, domain=None): return wall_indices
    wall_reg = domain.create_region('Walls', 'vertices by get_wall_idxs', 'facet',
                                    functions={'get_wall_idxs': get_wall_idxs})

    # WALL DISTANCE CALC
    print("Computing wall distances...")
    wall_dists = compute_wall_distance(mesh, wall_indices)

    # FIELDS & VARIABLES
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)
    field_d = Field.from_args('fd', np.float64, 'scalar', omega, approx_order=1)

    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')
    b = FieldVariable('b', 'parameter', field_u, primary_var_name='u')

    # Auxiliary Variables
    dist_var = FieldVariable('dist_var', 'parameter', field_d, primary_var_name='(set-to-None)')
    dist_var.set_data(wall_dists)

    u_prev = FieldVariable('u_prev', 'parameter', field_u, primary_var_name='(set-to-None)')
    # Initialize u_prev to zero
    u_prev.set_data(np.zeros(field_u.n_nod * field_u.n_components))

    # BOUNDARY CONDITIONS
    inlet_velocity = geom_data['inlet_velocity']
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        for i, (x, y) in enumerate(coors[:, :2]):
            val[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all' : inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all' : 0.0})
    bcs = Conditions([bc_inlet, bc_wall])

    # MATERIALS
    # Pass 'base_viscosity' here so the function can access it
    visc_fun_obj = Function('visc_fun', turbulence_viscosity_fun,
                            extra_args={'u_var': u_prev,
                                        'dist_var': dist_var,
                                        'base_viscosity': base_viscosity})

    m_fluid = Material('fluid', function=visc_fun_obj)

    name_map = {
        'u': 'u', 'p': 'p', 'b': 'b', 'v': 'v',
        'velocity': 'fu', 'pressure': 'fp',
        'viscosity': 'viscosity', 'fluid': 'fluid',
        'delta': 'delta', 'tau': 'tau', 'gamma': 'gamma'
    }
    stabil_func = Function('stabil_func', StabilizationFunction(name_map))
    m_stabil = Material('stabil', function=stabil_func)

    integral = Integral('i', order=3)

    # EQUATIONS
    # Note: Fluid viscosity is now spatially varying (from m_fluid)
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

    pb = Problem('stabilized_navier_stokes', equations=equations)
    pb.set_bcs(ebcs=bcs)

    ls = ScipyDirect({})

    conf_oseen = Struct(
        name = 'oseen',
        kind = 'nls.oseen',
        stabil_mat = 'stabil',
        i_max = 5,
        eps_a = 1e-6,
        eps_r = 1e-4,
        macheps = 1e-16,
        lin_red = 1e-2,
        check_navier_stokes_residual = False,
        problem = pb,
    )

    nls = Oseen(conf_oseen, lin_solver=ls, status={})
    nls.conf.problem = pb
    pb.set_solver(nls)

    # INITIALIZATION
    variables = pb.get_variables()
    u_dof_coords = field_u.get_coor()
    n_dofs = u_dof_coords.shape[0]

    # Initialize b (convective velocity) for the Oseen solver
    b_data = np.zeros((n_dofs, 2))
    for i in range(n_dofs):
        x, y = u_dof_coords[i, 0], u_dof_coords[i, 1]
        b_data[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
    variables['b'].set_data(b_data.ravel())

    # --- FIX IS HERE ---
    # Initialize u_prev to ZERO.
    # This ensures the first iteration is effectively "Laminar" (gradients=0 -> nu_t=0).
    # This prevents the solver from crashing on the first step due to artificial gradients.
    zero_u = np.zeros(n_dofs * 2, dtype=np.float64)
    u_prev.set_data(zero_u)

    # --- OUTER TURBULENCE LOOP ---
    print("\nStarting Turbulence Outer Loop...")
    turbulence_max_iters = 10


    for outer_it in range(turbulence_max_iters):
        print(f"--- Outer Iteration {outer_it + 1} / {turbulence_max_iters} ---")

        # Solve
        state = pb.solve()

        # Check Convergence
        new_u_data = state['u'].data
        prev_u_data = u_prev.data

        # Avoid division by zero if field is empty (unlikely)
        norm_new = np.linalg.norm(new_u_data)
        if norm_new > 1e-12:
            diff_norm = np.linalg.norm(new_u_data - prev_u_data) / norm_new
        else:
            diff_norm = 0.0

        print(f"Velocity Update Norm: {diff_norm:.6e}")

        u_prev.set_data(new_u_data)

        if diff_norm < 1e-4:
            print("Turbulence loop converged.")
            break

    # POST PROCESSING
    out = state.create_output()
    u_vals = out['u'].data

    X, Y, Vx_grid, Vy_grid, vel_mag = postprocess_solution(
        u_vals, mesh, geom_data
    )
    ny, nx = X.shape
    parameter_string = get_parameter_string(geom_data)

    cache_dir, scenario_name = get_cache_dir(args.scenario)
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vx.txt", Vx_grid,
               header=f'{ny}_{nx}_{parameter_string}')
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vy.txt", Vy_grid,
               header=f'{ny}_{nx}_{parameter_string}')

    print(f"\nOseen solver time: {time.time() - mesh_time:.2f} s")
    print(f"\nTotal time: {time.time() - start_time:.2f} s")
    plot_results(mesh, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"],
                 f"{cache_dir / scenario_name}_{args.hash}_results.png")

    if os.path.exists("domain.vtk"):
            os.remove("domain.vtk")

if __name__ == '__main__':
    main()