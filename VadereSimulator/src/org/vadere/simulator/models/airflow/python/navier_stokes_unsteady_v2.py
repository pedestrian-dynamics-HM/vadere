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
from collections import deque

from sfepy.base.base import Struct
from sfepy.discrete import (FieldVariable, Material, Integral, Function,
                            Equation, Equations, Problem)
from sfepy.discrete.fem import FEDomain, Field
from sfepy.terms import Term
from sfepy.discrete.conditions import Conditions, EssentialBC
from sfepy.solvers.ls import ScipyDirect
from sfepy.solvers.ts_solvers import SimpleTimeSteppingSolver
from sfepy.solvers.nls import Newton

from build_mesh import build_mesh
from helpers import *

faulthandler.enable(file=sys.stderr, all_threads=True)

class SmagorinskyEddyViscosity:
    """Smagorinsky turbulence model for URANS"""
    def __init__(self, C_s=0.1, nu_laminar=1.5e-5):
        self.C_s = C_s
        self.nu_laminar = nu_laminar

    def compute_eddy_viscosity(self, u_vals, mesh, domain):
        """
        Compute turbulent viscosity using Smagorinsky model.
        nu_t = (C_s * delta)^2 * |S|
        where |S| = sqrt(2 * S_ij * S_ij) is the strain rate magnitude
        """
        # Get element sizes (characteristic length scale)
        volumes = mesh.cmesh.get_volumes(mesh.cmesh.tdim)
        delta = np.power(volumes, 1.0/mesh.cmesh.tdim)  # h = V^(1/dim)

        # Compute velocity gradients at element centers
        # This is a simplified approach - for production, use proper FEM gradient recovery
        n_el = mesh.n_el
        strain_rate_mag = np.zeros(n_el)

        # Get connectivity
        conn = mesh.cmesh.get_conn(mesh.cmesh.tdim, 0).indices
        conn = conn.reshape((n_el, -1))

        for i in range(n_el):
            nodes = conn[i]
            u_el = u_vals[nodes].reshape(-1, 2)

            # Simple finite difference approximation of strain rate
            if len(nodes) >= 3:
                # Compute velocity gradients (simplified)
                du_dx = np.std(u_el[:, 0])
                du_dy = np.std(u_el[:, 1])
                dv_dx = du_dy  # Approximate
                dv_dy = du_dx  # Approximate

                # Strain rate tensor magnitude: |S| = sqrt(2*S_ij*S_ij)
                S11 = du_dx
                S22 = dv_dy
                S12 = 0.5 * (du_dy + dv_dx)

                strain_rate_mag[i] = np.sqrt(2 * (S11**2 + S22**2 + 2*S12**2))

        # Compute eddy viscosity at elements
        nu_t = (self.C_s * delta)**2 * strain_rate_mag

        # Limit eddy viscosity (stability)
        nu_t = np.clip(nu_t, 0, 100 * self.nu_laminar)

        return nu_t, delta

def update_viscosity_field(m_fluid, u_vals, mesh, domain, smagorinsky, field_u):
    """Update the effective viscosity (laminar + turbulent)"""
    nu_t, delta = smagorinsky.compute_eddy_viscosity(u_vals, mesh, domain)

    # Map element values to nodes (simple averaging)
    conn = mesh.cmesh.get_conn(mesh.cmesh.tdim, 0).indices
    n_el = mesh.n_el
    conn = conn.reshape((n_el, -1))

    n_nod = mesh.n_nod
    nu_t_nodal = np.zeros(n_nod)
    counts = np.zeros(n_nod)

    for i in range(n_el):
        nodes = conn[i]
        for node in nodes:
            nu_t_nodal[node] += nu_t[i]
            counts[node] += 1

    nu_t_nodal = nu_t_nodal / np.maximum(counts, 1)

    # Update material with effective viscosity
    nu_eff = smagorinsky.nu_laminar + nu_t_nodal

    # Return as constant for now (proper implementation would use spatial field)
    nu_eff_mean = np.mean(nu_eff)

    return nu_eff_mean

def compute_adaptive_timestep(u_vals, mesh, nu, CFL=0.5, dt_max=1.0):
    """Compute adaptive timestep based on CFL condition"""
    # Get velocity magnitude
    u_mag = np.sqrt(u_vals[::2]**2 + u_vals[1::2]**2)
    u_max = np.max(u_mag)

    if u_max < 1e-10:
        return dt_max

    # Estimate minimum element size
    volumes = mesh.cmesh.get_volumes(mesh.cmesh.tdim)
    h_min = np.min(np.power(volumes, 1.0/mesh.cmesh.tdim))

    # CFL condition: dt < CFL * h / u_max
    dt_convective = CFL * h_min / u_max

    # Diffusion condition: dt < h^2 / (2*nu)
    dt_diffusive = h_min**2 / (4 * nu)

    dt = min(dt_convective, dt_diffusive, dt_max)

    return max(dt, 1e-4)  # Lower bound

def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    parser.add_argument('--dt', type=float, default=0.1, help='Initial timestep')
    parser.add_argument('--t-total', type=float, default=50.0, help='Total simulation time')
    parser.add_argument('--t-average-start', type=float, default=30.0, help='Start time-averaging')
    parser.add_argument('--adaptive-dt', action='store_true', help='Use adaptive timestepping')
    parser.add_argument('--C-s', type=float, default=0.1, help='Smagorinsky constant')
    args = parser.parse_args()

    # SETUP & MESH GENERATION
    geom_data = extract_attributes(args.scenario)
    mesh, bdry_indices = build_mesh(geom_data)

    print(f"Mesh: {mesh.n_nod} nodes, {mesh.n_el} elements.")
    domain = FEDomain('domain', mesh)

    # REGIONS
    omega = domain.create_region('Omega', 'all')
    all_bdry_reg = domain.create_region('AllBoundary', 'vertices of surface', 'facet')
    all_bdry_indices = all_bdry_reg.vertices

    def get_inlet_idxs(coors, domain=None): return bdry_indices['inlet']
    def get_outlet_idxs(coors, domain=None): return bdry_indices['outlet']

    inlet_reg = domain.create_region('Inlet', 'vertices by get_inlet_idxs', 'facet', functions={'get_inlet_idxs': get_inlet_idxs})
    outlet_reg = domain.create_region('Outlet', 'vertices by get_outlet_idxs', 'facet', functions={'get_outlet_idxs': get_outlet_idxs})

    open_bdry_indices = np.union1d(bdry_indices['inlet'], bdry_indices['outlet'])
    wall_indices = np.setdiff1d(all_bdry_indices, open_bdry_indices)

    def get_wall_idxs(coors, domain=None): return wall_indices
    wall_reg = domain.create_region('Walls', 'vertices by get_wall_idxs', 'facet', functions={'get_wall_idxs': get_wall_idxs})

    # FIELDS & VARIABLES
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')
    u_prev = FieldVariable('u_prev', 'parameter', field_u, primary_var_name='u')

    # BOUNDARY CONDITIONS
    inlet_velocity = geom_data['inlet_velocity']
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        np.random.seed(int(ts.time * 1000) % 2**31)
        for i, (x, y) in enumerate(coors[:, :2]):
            base_vel = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
            fluctuation = 0.05 * np.random.randn(2)
            val[i] = base_vel * (1 + fluctuation)
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all' : inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all' : 0.0})
    bcs = Conditions([bc_inlet, bc_wall])

    # MATERIALS
    nu_laminar = geom_data['viscosity']

    # --- FIX STARTS HERE ---
    # We use a mutable dictionary 'sim_state' to store dynamic parameters.
    # The 'get_fluid_props' function reads from this dictionary every time SfePy evaluates materials.
    sim_state = {'viscosity': nu_laminar}

    def get_fluid_props(ts, coors, mode=None, **kwargs):
        if mode == 'qp':
            n_qp, _ = coors.shape
            val = sim_state['viscosity']
            # SfePy expects material properties at all quadrature points (n_qp, 1, 1)
            return {'viscosity': np.tile(val, (n_qp, 1, 1)),
                    'density': np.tile(1.0, (n_qp, 1, 1))}

    # Initialize Material with the function, NOT static values
    m_fluid = Material('fluid', function=get_fluid_props)
    # --- FIX ENDS HERE ---

    smagorinsky = SmagorinskyEddyViscosity(C_s=args.C_s, nu_laminar=nu_laminar)

    integral = Integral('i', order=3)

    # EQUATIONS
    t_mass = Term.new('dw_volume_dot(fluid.density, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
    t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
    t_conv = Term.new('dw_lin_convect(v, u_prev, u)', integral, omega, v=v, u_prev=u_prev, u=u)
    t_press = Term.new('dw_stokes(v, p)', integral, omega, v=v, p=p)
    t_div = Term.new('dw_stokes(u, q)', integral, omega, u=u, q=q)
    t_graddiv = Term.new('dw_st_grad_div(fluid.viscosity, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)

    eq_momentum = Equation('balance', t_mass + t_diff + t_conv - t_press + t_graddiv)
    eq_continuity = Equation('incompressibility', t_div)
    equations = Equations([eq_momentum, eq_continuity])

    # PROBLEM & SOLVER
    pb = Problem('urans_navier_stokes', equations=equations)
    pb.set_bcs(ebcs=bcs)
    ls = ScipyDirect({})
    nls = Newton({'i_max': 5, 'eps_a': 1e-6, 'eps_r': 1e-4}, lin_solver=ls)
    pb.set_solver(nls)

    # TIMING
    dt = args.dt
    t_total = args.t_total
    t_average_start = args.t_average_start

    # INITIALIZATION
    variables = pb.get_variables()

    # 1. Prepare Full Quadratic Data (for Solver)
    u_dof_coords = field_u.get_coor()
    n_dofs = u_dof_coords.shape[0]
    u_data_dofs = np.zeros((n_dofs, 2))

    for i in range(n_dofs):
        x, y = u_dof_coords[i, 0], u_dof_coords[i, 1]
        u_data_dofs[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)

    u_vec_flat = u_data_dofs.ravel()

    variables['u'].set_data(u_vec_flat)
    variables['u_prev'].set_data(u_vec_flat.copy())
    variables['p'].set_data(np.zeros(field_p.n_nod))

    u_full_dofs = u_vec_flat.copy()

    # 2. Prepare Nodal Data (for Viscosity/Visualization)
    coors = mesh.coors
    n_nod = mesh.n_nod
    u_vec_nodal = np.zeros((n_nod, 2))

    for i in range(n_nod):
        x, y = coors[i, 0], coors[i, 1]
        u_vec_nodal[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)

    # TIME LOOP
    print("\n" + "="*60 + "\nSTARTING UNSTEADY SIMULATION\n" + "="*60)

    t = 0.0
    step = 0
    u_sum = None
    n_avg_samples = 0
    solve_times = deque(maxlen=10)
    solve_start = time.time()

    while t < t_total:
        step += 1

        if args.adaptive_dt and step > 1:
            dt = compute_adaptive_timestep(u_vec_nodal, mesh, nu_laminar, CFL=0.5, dt_max=0.5)

        step_start = time.time()

        if step % 5 == 0:
            # Calculate new viscosity based on current flow
            nu_eff = update_viscosity_field(m_fluid, u_vec_nodal, mesh, domain, smagorinsky, field_u)

            # --- FIX: Update the container ---
            # Instead of calling m_fluid.set_data(), we just update the variable
            # that m_fluid's function reads. SfePy handles the rest.
            sim_state['viscosity'] = nu_eff

        variables['u_prev'].set_data(u_full_dofs.copy())

        try:
            state = pb.solve()
            variables = state
            out = state.create_output()
            u_vec_nodal = out['u'].data

            full_solution_vector = state()
            u_indices = state['u'].indx
            u_full_dofs = full_solution_vector[u_indices]

        except Exception as e:
            print(f"\nSolver failed at t={t:.3f}, dt={dt:.4f}")
            print(f"Error: {e}")
            dt *= 0.5
            if dt < 1e-5: break
            continue

        t += dt

        if t >= t_average_start:
            if u_sum is None: u_sum = np.zeros_like(u_vec_nodal)
            u_sum += u_vec_nodal
            n_avg_samples += 1

        step_time = time.time() - step_start
        solve_times.append(step_time)

        if step % 10 == 0 or step == 1:
            avg_time = np.mean(solve_times)
            eta = avg_time * (t_total - t) / dt
            u_mag = np.sqrt(u_vec_nodal[:, 0]**2 + u_vec_nodal[:, 1]**2)
            print(f"Step {step:4d} | t={t:6.2f}/{t_total:.0f} | dt={dt:.4f} | |u|_max={np.max(u_mag):.3f} | solve={step_time:.3f}s | ETA={eta:.0f}s")

    # POST PROCESSING
    solve_time = time.time() - solve_start
    print(f"\nTotal solve time: {solve_time:.2f} s")

    if n_avg_samples > 0:
        u_avg = u_sum / n_avg_samples
        print(f"Using time-averaged solution over {n_avg_samples} samples")
    else:
        u_avg = u_vec_nodal
        print(f"Warning: No time-averaging performed")

    X, Y, Vx_grid, Vy_grid, vel_mag = postprocess_solution(u_avg, mesh, geom_data)

    cache_dir, scenario_name = get_cache_dir(args.scenario)
    param_str = get_parameter_string(geom_data) + f"_URANS_Cs{args.C_s}_tavg{n_avg_samples}"

    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vx.txt", Vx_grid, header=f'{X.shape[0]}_{X.shape[1]}_{param_str}')
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vy.txt", Vy_grid, header=f'{X.shape[0]}_{X.shape[1]}_{param_str}')

    plot_results(mesh, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"], f"{cache_dir / scenario_name}_{args.hash}_results.png")
    if os.path.exists("domain.vtk"): os.remove("domain.vtk")

if __name__ == '__main__':
    main()