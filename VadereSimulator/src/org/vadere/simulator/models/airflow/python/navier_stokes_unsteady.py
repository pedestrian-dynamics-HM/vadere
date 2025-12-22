import os
import sys
import time
import numpy as np
import argparse
import faulthandler

from sfepy.base.base import Struct
from sfepy.discrete import (FieldVariable, Material, Integral, Function,
                            Equation, Equations, Problem)
from sfepy.discrete.fem import FEDomain, Field
from sfepy.terms import Term
from sfepy.discrete.conditions import Conditions, EssentialBC
from sfepy.solvers import Solver

from build_mesh import build_mesh
from helpers import *

faulthandler.enable(file=sys.stderr, all_threads=True)

# --- CONFIGURATION ---
t0 = 0.0
t1 = 1.0
dt = 0.05
n_steps = int(round((t1 - t0) / dt))
avg_last_n_steps = int(n_steps * 0.4)

# --- HELPER CLASS FOR AVERAGING ---
class VelocityAverager:
    def __init__(self, start_step, variables, region):
        self.start_step = start_step
        self.region = region
        self.count = 0

        # Robust Initialization:
        # Get the actual data slice for 'u' in the domain to determine shape.
        sample_u = variables['u'].get_state_in_region(self.region)
        self.u_acc = np.zeros_like(sample_u.ravel())

    def __call__(self, pb, ts, variables):
        if ts.step >= self.start_step:
            # 1. Extract specific DOFs for 'u' in the region
            u_current = variables['u'].get_state_in_region(self.region)

            # 2. Flatten to ensure it matches the accumulator shape
            u_current = u_current.ravel()

            # 3. Accumulate
            self.u_acc += u_current
            self.count += 1

            if ts.step % 10 == 0:
                print(f"Step {ts.step}: Averaging... (Samples: {self.count})")

def main():
    start_time = time.time()
    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    # 1. SETUP & MESH
    geom_data = extract_attributes(args.scenario)
    geom_data['density'] = 1.225

    mesh, bdry_indices = build_mesh(geom_data)
    domain = FEDomain('domain', mesh)

    omega = domain.create_region('Omega', 'all')
    all_bdry_reg = domain.create_region('AllBoundary', 'vertices of surface', 'facet')
    all_bdry_indices = all_bdry_reg.vertices

    def get_inlet_idxs(coors, domain=None): return bdry_indices['inlet']
    def get_outlet_idxs(coors, domain=None): return bdry_indices['outlet']
    def get_wall_idxs(coors, domain=None):
        open_bdry = np.union1d(bdry_indices['inlet'], bdry_indices['outlet'])
        return np.setdiff1d(all_bdry_indices, open_bdry)

    inlet_reg = domain.create_region('Inlet', 'vertices by get_inlet_idxs', 'facet',
                                     functions={'get_inlet_idxs': get_inlet_idxs})
    wall_reg = domain.create_region('Walls', 'vertices by get_wall_idxs', 'facet',
                                    functions={'get_wall_idxs': get_wall_idxs})

    # 2. FIELDS & VARIABLES
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')

    # 3. BOUNDARY CONDITIONS
    inlet_velocity = geom_data['inlet_velocity']
    x_min, x_max, y_min, y_max = geom_data['x_min'], geom_data['x_max'], geom_data['y_min'], geom_data['y_max']

    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        for i, (x, y) in enumerate(coors[:, :2]):
            val[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all' : inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all' : 0.0})
    bcs = Conditions([bc_inlet, bc_wall])

    # 4. MATERIALS & EQUATIONS
    m_fluid = Material('fluid', values={'viscosity': geom_data['viscosity'], 'density': geom_data['density']})
    integral = Integral('i', order=3)

    t_inertial = Term.new('dw_volume_dot(fluid.density, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
    t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega, fluid=m_fluid, v=v, u=u)
    t_conv = Term.new('dw_convect(v, u, u)', integral, omega, v=v, u=u)
    t_press = Term.new('dw_stokes(v, p)', integral, omega, v=v, p=p)
    t_div = Term.new('dw_stokes(u, q)', integral, omega, u=u, q=q)

    eq_momentum = Equation('balance', t_inertial + t_conv + t_diff - t_press)
    eq_continuity = Equation('incompressibility', t_div)
    equations = Equations([eq_momentum, eq_continuity])

    # 5. SOLVERS
    # OPTION A: Iterative Solver (Faster for larger meshes)
    # Uses GMRES with ILU preconditioning.
    #ls_conf = {
    #    'name': 'ls',
    #    'kind': 'ls.scipy_iterative',
    #    'method': 'gmres',
    #    'i_max': 500,
    #    'eps_a': 1e-6,
    #    'eps_r': 1e-6,
    #}

    # OPTION B: Direct Solver (Keep this if your mesh is very small < 2000 nodes)
    ls_conf = {'name': 'ls', 'kind': 'ls.scipy_direct'}

    # NONLINEAR SOLVER (NEWTON)
    nls_conf = {
        'name': 'newton',
        'kind': 'nls.newton',

        # SPEED OPTIMIZATION 1: Relax tolerances
        # We don't need 1e-8 precision for every time step.
        # 1e-3 is usually enough to capture the flow physics.
        'eps_a': 1e-4,
        'eps_r': 1e-3,

        # SPEED OPTIMIZATION 2: Jacobian Reuse (lin_red)
        # If the residual reduces by factor 0.1 (lin_red), REUSE the matrix
        # instead of rebuilding it. This saves massive amounts of CPU time.
        'lin_red': 0.1,

        'i_max': 15,    # Safety limit
    }

    # TIME STEPPING SOLVER
    ts_conf = {
        'name': 'ts',
        'kind': 'ts.simple',
        't0': t0, 't1': t1, 'dt': dt, 'n_step': n_steps,
        'verbose': 1
    }

    pb = Problem('unsteady_ns', equations=equations)
    pb.set_bcs(ebcs=bcs)

    # Init Solvers
    ls = Solver.any_from_conf(Struct(**ls_conf))
    nls = Solver.any_from_conf(Struct(**nls_conf), lin_solver=ls)

    # Bind Evaluator (Critical for manual solving)
    evaluator = pb.get_evaluator()
    nls.fun = evaluator.eval_residual
    nls.fun_grad = evaluator.eval_tangent_matrix

    ts = Solver.any_from_conf(Struct(**ts_conf), nls=nls, problem=pb)
    pb.set_solver(ts)

    # 6. INITIALIZATION
    variables = pb.get_variables()
    variables['u'].set_data(np.zeros(variables['u'].n_dof))
    variables['p'].set_data(np.zeros(variables['p'].n_dof))

    # 7. SOLVE WITH AVERAGING
    print(f"Starting time-stepping: {n_steps} steps, dt={dt}")

    averager = VelocityAverager(
        start_step=(n_steps - avg_last_n_steps),
        variables=variables,
        region=omega
    )

    # Store the solution state
    state = pb.solve(step_hook=averager)

    # 8. POST PROCESSING
    if averager.count > 0:
        print(f"Averaged over last {averager.count} steps.")

        # Calculate Mean DOFs
        u_mean_dofs = averager.u_acc / averager.count

        # --- FIX: INJECT MEAN DATA INTO STATE OBJECT ---
        # Instead of setting variables directly (which is locked),
        # we modify the State object's vector.

        # 1. Get indices of 'u' in the global state vector
        idxs_u = variables.get_indx('u')

        # 2. Overwrite the U part of the state vector with our mean
        # state.vec contains the full global solution
        state.vec[idxs_u] = u_mean_dofs

        # 3. Create output from the modified state
        # This automatically handles the conversion from DOFs to Nodal Values
        out = state.create_output()
        u_nodal_vals = out['u'].data

    else:
        print("Warning: No steps averaged. Using last step.")
        out = state.create_output()
        u_nodal_vals = out['u'].data

    # Continue with plotting using the (now averaged) nodal values
    X, Y, Vx_grid, Vy_grid, vel_mag = postprocess_solution(
        u_nodal_vals, mesh, geom_data
    )

    parameter_string = get_parameter_string(geom_data)
    cache_dir, scenario_name = get_cache_dir(args.scenario)

    ny, nx = X.shape
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vx_mean.txt", Vx_grid,
               header=f'{ny}_{nx}_{parameter_string}')
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vy_mean.txt", Vy_grid,
               header=f'{ny}_{nx}_{parameter_string}')

    print(f"\nTotal time: {time.time() - start_time:.2f} s")
    plot_results(mesh, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"],
                 f"{cache_dir / scenario_name}_{args.hash}_mean_results.png")

if __name__ == '__main__':
    main()