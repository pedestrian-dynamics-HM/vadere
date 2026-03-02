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

from sfepy.base.base import Struct
from sfepy.discrete import (FieldVariable, Material, Integral, Function,
                            Equation, Equations, Problem)
from sfepy.discrete.fem import FEDomain, Field
from sfepy.terms import Term
from sfepy.discrete.conditions import Conditions, EssentialBC
from sfepy.solvers.ls import ScipyDirect, ScipyIterative, PETScKrylovSolver
from sfepy.solvers.oseen import Oseen, StabilizationFunction

from build_mesh import build_mesh
from helpers import *

faulthandler.enable(file=sys.stderr, all_threads=True)

max_iters = 20

def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    # SETUP & MESH GENERATION
    geom_data = extract_attributes(args.scenario)

    mesh, bdry_indices = build_mesh(geom_data)

    print(f"Mesh: {mesh.n_nod} nodes, {mesh.n_el} elements.")
    mesh_time = time.time()
    print(f"\nMesh creation time: {mesh_time - start_time:.2f} s")
    domain = FEDomain('domain', mesh)

    # REGIONS
    omega = domain.create_region('Omega', 'all')
    all_bdry_reg = domain.create_region('AllBoundary', 'vertices of surface', 'facet')
    all_bdry_indices = all_bdry_reg.vertices

    def get_inlet_idxs(coors, domain=None):
        return bdry_indices['inlet']

    def get_outlet_idxs(coors, domain=None):
        return bdry_indices['outlet']

    inlet_reg = domain.create_region('Inlet',
                                     'vertices by get_inlet_idxs',
                                     'facet',
                                     functions={'get_inlet_idxs': get_inlet_idxs})
    outlet_reg = domain.create_region('Outlet',
                                      'vertices by get_outlet_idxs',
                                      'facet',
                                      functions={'get_outlet_idxs': get_outlet_idxs})
    open_bdry_indices = np.union1d(bdry_indices['inlet'], bdry_indices['outlet'])
    wall_indices = np.setdiff1d(all_bdry_indices, open_bdry_indices)

    def get_wall_idxs(coors, domain=None):
        return wall_indices

    wall_reg = domain.create_region('Walls',
                                    'vertices by get_wall_idxs',
                                    'facet',
                                    functions={'get_wall_idxs': get_wall_idxs})

    # FIELDS & VARIABLES
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')
    b = FieldVariable('b', 'parameter', field_u, primary_var_name='u')

    # BOUNDARY CONDITIONS
    inlet_velocity = geom_data['inlet_velocity']
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']
    profile_eps = 1e-3

    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        for i, (x, y) in enumerate(coors[:, :2]):
            val[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all' : inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all' : 0.0})
    bcs = Conditions([bc_inlet, bc_wall])

    # MATERIALS & STABILIZATION
    m_fluid = Material('fluid', values={'viscosity': geom_data['viscosity']})

    name_map = {
        'u': 'u',
        'p': 'p',
        'b': 'b',
        'v': 'v',
        'velocity': 'fu',
        'pressure': 'fp',
        'viscosity': 'viscosity',
        'fluid': 'fluid',
        'delta': 'delta',
        'tau': 'tau',
        'gamma': 'gamma'
    }

    stabil_func = Function('stabil_func', StabilizationFunction(name_map))
    m_stabil = Material('stabil', function=stabil_func)

    integral = Integral('i', order=3)

    # EQUATIONS
    # momentum
    t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega,
                      fluid=m_fluid, v=v, u=u)
    t_conv = Term.new('dw_lin_convect(v, b, u)', integral, omega,
                      v=v, b=b, u=u)
    t_press = Term.new('dw_stokes(v, p)', integral, omega,
                       v=v, p=p)

    # stabilization
    t_supg_c = Term.new('dw_st_supg_c(stabil.delta, v, b, u)', integral, omega,
                        stabil=m_stabil, v=v, b=b, u=u)
    t_supg_p = Term.new('dw_st_supg_p(stabil.delta, v, b, p)', integral, omega,
                        stabil=m_stabil, v=v, b=b, p=p)
    t_graddiv = Term.new('dw_st_grad_div(stabil.gamma, v, u)', integral, omega,
                         stabil=m_stabil, v=v, u=u)

    # continuity
    t_div = Term.new('dw_stokes(u, q)', integral, omega,
                     u=u, q=q)

    # stabilization
    t_pspg_c = Term.new('dw_st_pspg_c(stabil.tau, q, b, u)', integral, omega,
                        stabil=m_stabil, q=q, b=b, u=u)
    t_pspg_p = Term.new('dw_st_pspg_p(stabil.tau, q, p)', integral, omega,
                        stabil=m_stabil, q=q, p=p)

    eq_momentum = Equation('balance', t_diff + t_conv - t_press + t_graddiv + t_supg_c + t_supg_p)
    eq_continuity = Equation('incompressibility', t_div + t_pspg_c + t_pspg_p)
    equations = Equations([eq_momentum, eq_continuity])

    # PROBLEM & SOLVER
    pb = Problem('stabilized_navier_stokes', equations=equations)
    pb.set_bcs(ebcs=bcs)
    ls = ScipyDirect({})

    conf_oseen = Struct(
        name = 'oseen',
        kind = 'nls.oseen',
        stabil_mat = 'stabil',
        i_max = max_iters,
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
    b_data = np.zeros((n_dofs, 2))
    for i in range(n_dofs):
        x, y = u_dof_coords[i, 0], u_dof_coords[i, 1]
        b_data[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
    variables['b'].set_data(b_data.ravel())

    # SOLVE
    state = pb.solve()

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
                 f"{cache_dir / scenario_name}_{args.hash}")

    if os.path.exists("domain.vtk"):
            os.remove("domain.vtk")

if __name__ == '__main__':
    main()