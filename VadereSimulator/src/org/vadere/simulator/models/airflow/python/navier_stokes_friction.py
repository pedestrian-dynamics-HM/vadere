"""
Quasi-2D Navier-Stokes Solver with Rayleigh Friction (Depth-Averaged Formulation)

This script solves the steady incompressible Navier-Stokes equations in 2D with an
added linear drag term to represent vertical shear stress from floor/ceiling friction.

The governing equations are:
    (u·∇)u = -∇p/ρ + ν∇²u - αu    (momentum with Rayleigh friction)
    ∇·u = 0                        (continuity)

The friction coefficient α is derived from depth-averaging the 3D equations assuming
a Poiseuille-like vertical velocity profile:
    α = 12ν/H²

where H is the room height and ν is the kinematic viscosity.

References:
    - Dolzhansky et al. (1992), J. Fluid Mech. 241, 705-722
    - Clercx & van Heijst (2009), Appl. Mech. Rev. 62, 020802
    - Boffetta & Ecke (2012), Annu. Rev. Fluid Mech. 44, 427-451

Author: Modified for quasi-2D indoor airflow modeling
"""

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


def compute_rayleigh_friction_coefficient(room_height, kinematic_viscosity,
                                          turbulence_factor=1.0, verbose=True):
    """
    Compute the Rayleigh friction coefficient α for depth-averaged quasi-2D model.

    Derivation (Poiseuille flow between parallel plates):
    ----------------------------------------------------
    For fully-developed laminar flow between floor and ceiling:

    1. Vertical velocity profile: u(z) = u_max * (1 - (2z/H)²)
    2. Wall shear stress: τ_wall = μ * (∂u/∂z)|_wall = 4μu_max/H
    3. Depth-averaged velocity: ū = (2/3) * u_max
    4. Friction force (both walls): F = -2τ_wall/H = -12μū/H² = -αρū

    Therefore: α = 12ν/H²

    Parameters
    ----------
    room_height : float
        Height of the room H [m]
    kinematic_viscosity : float
        Kinematic viscosity of air ν [m²/s]
    turbulence_factor : float, optional
        Multiplicative factor to increase α for turbulent conditions.
        - 1.0 for laminar (default)
        - 10-100 for mildly turbulent indoor flows
        - Higher for strongly turbulent conditions
    verbose : bool
        Print diagnostic information

    Returns
    -------
    alpha : float
        Rayleigh friction coefficient [1/s]

    Notes
    -----
    For turbulent flows, the effective friction is higher. The laminar estimate
    provides a lower bound. In practice, calibration against experimental data
    or 3D simulations may be needed.

    Typical values for indoor airflow (H=2.5m, air at 20°C):
    - Laminar estimate: α ≈ 2.9e-5 s⁻¹
    - Turbulent estimate: α ≈ 1e-3 to 1e-2 s⁻¹
    """
    # Laminar (Poiseuille) derivation
    alpha_laminar = 12.0 * kinematic_viscosity / (room_height ** 2)

    # Apply turbulence correction
    alpha = alpha_laminar * turbulence_factor

    if verbose:
        print("\n" + "="*60)
        print("RAYLEIGH FRICTION COEFFICIENT CALCULATION")
        print("="*60)
        print(f"Room height H:           {room_height:.2f} m")
        print(f"Kinematic viscosity ν:   {kinematic_viscosity:.2e} m²/s")
        print(f"Turbulence factor:       {turbulence_factor:.1f}")
        print("-"*60)
        print(f"Formula: α = 12ν/H² × turbulence_factor")
        print(f"α_laminar = 12 × {kinematic_viscosity:.2e} / {room_height:.2f}²")
        print(f"         = {alpha_laminar:.4e} s⁻¹")
        print(f"α_final   = {alpha:.4e} s⁻¹")
        print("-"*60)

        # Physical interpretation
        decay_time = 1.0 / alpha if alpha > 0 else float('inf')
        print(f"Momentum decay timescale: τ = 1/α = {decay_time:.1f} s")

        # Estimate decay length for typical indoor velocity
        U_typical = 0.5  # m/s, typical indoor air velocity
        decay_length = U_typical / alpha if alpha > 0 else float('inf')
        print(f"Decay length (U={U_typical} m/s): L = U/α ≈ {decay_length:.1f} m")
        print("="*60 + "\n")

    return alpha


def main():
    start_time = time.time()

    parser = argparse.ArgumentParser(
        description='Quasi-2D Navier-Stokes solver with Rayleigh friction'
    )
    parser.add_argument('scenario', help='Scenario configuration file/name')
    parser.add_argument('hash', help='Unique identifier for output files')
    parser.add_argument('--room-height', type=float, default=2.5,
                        help='Room height in meters (default: 2.5)')
    parser.add_argument('--turbulence-factor', type=float, default=1.0,
                        help='Turbulence enhancement factor for α (default: 1.0)')
    parser.add_argument('--alpha-override', type=float, default=None,
                        help='Directly specify α value, overriding calculation')
    args = parser.parse_args()

    # =========================================================================
    # SETUP & MESH GENERATION
    # =========================================================================
    geom_data = extract_attributes(args.scenario)
    mesh, bdry_indices = build_mesh(geom_data)

    print(f"Mesh: {mesh.n_nod} nodes, {mesh.n_el} elements.")
    mesh_time = time.time()
    print(f"\nMesh creation time: {mesh_time - start_time:.2f} s")
    domain = FEDomain('domain', mesh)

    # =========================================================================
    # RAYLEIGH FRICTION COEFFICIENT
    # =========================================================================
    # Standard air properties at 20°C, 1 atm
    # Note: We use kinematic viscosity here, not dynamic viscosity
    kinematic_viscosity = geom_data['viscosity']  # Should be ~1.5e-5 m²/s for air

    if args.alpha_override is not None:
        alpha_value = args.alpha_override
        print(f"\nUsing user-specified α = {alpha_value:.4e} s⁻¹")
    else:
        alpha_value = compute_rayleigh_friction_coefficient(
            room_height=args.room_height,
            kinematic_viscosity=kinematic_viscosity,
            turbulence_factor=args.turbulence_factor,
            verbose=True
        )

    # =========================================================================
    # REGIONS
    # =========================================================================
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

    # =========================================================================
    # FIELDS & VARIABLES
    # =========================================================================
    # Taylor-Hood elements: P2 velocity, P1 pressure (stable pairing)
    field_u = Field.from_args('fu', np.float64, 'vector', omega, approx_order=2)
    field_p = Field.from_args('fp', np.float64, 'scalar', omega, approx_order=1)

    u = FieldVariable('u', 'unknown', field_u)
    v = FieldVariable('v', 'test', field_u, primary_var_name='u')
    p = FieldVariable('p', 'unknown', field_p)
    q = FieldVariable('q', 'test', field_p, primary_var_name='p')
    b = FieldVariable('b', 'parameter', field_u, primary_var_name='u')

    # =========================================================================
    # BOUNDARY CONDITIONS
    # =========================================================================
    inlet_velocity = geom_data['inlet_velocity']
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    def inlet_profile_func(ts, coors, **kwargs):
        val = np.zeros((coors.shape[0], 2))
        for i, (x, y) in enumerate(coors[:, :2]):
            val[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
        return val

    inlet_fun = Function('inlet_vel', inlet_profile_func)
    bc_inlet = EssentialBC('InletBC', inlet_reg, {'u.all': inlet_fun})
    bc_wall = EssentialBC('WallBC', wall_reg, {'u.all': 0.0})
    bcs = Conditions([bc_inlet, bc_wall])

    # =========================================================================
    # MATERIALS
    # =========================================================================
    # Fluid properties (viscosity for diffusion term)
    m_fluid = Material('fluid', values={'viscosity': geom_data['viscosity']})

    # Rayleigh friction material
    # The dw_dot term computes: ∫ α (v · u) dΩ
    # This adds the weak form of the drag term -αu to the momentum equation
    m_drag = Material('drag', values={'alpha': alpha_value})

    # Stabilization parameters (SUPG/PSPG/grad-div)
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

    # =========================================================================
    # WEAK FORM TERMS
    # =========================================================================
    # The weak form of the quasi-2D Navier-Stokes with Rayleigh friction:
    #
    # Momentum: ∫[ν∇v:∇u + v·(b·∇)u + αv·u - p(∇·v)] dΩ + stabilization = 0
    # Continuity: ∫[q(∇·u)] dΩ + stabilization = 0

    # --- Momentum equation terms ---
    # Viscous diffusion: ∫ ν ∇v : ∇u dΩ
    t_diff = Term.new('dw_div_grad(fluid.viscosity, v, u)', integral, omega,
                      fluid=m_fluid, v=v, u=u)

    # Convection (linearized): ∫ v · (b·∇)u dΩ
    t_conv = Term.new('dw_lin_convect(v, b, u)', integral, omega,
                      v=v, b=b, u=u)

    # Pressure gradient: -∫ p (∇·v) dΩ
    t_press = Term.new('dw_stokes(v, p)', integral, omega,
                       v=v, p=p)

    # RAYLEIGH FRICTION (depth-averaged drag): ∫ α v·u dΩ
    # This is the key term for quasi-2D regularization!
    # dw_dot computes: ∫ material_coefficient * (v · u) dΩ
    t_drag = Term.new('dw_dot(drag.alpha, v, u)', integral, omega,
                      drag=m_drag, v=v, u=u)

    # --- Stabilization terms (SUPG/grad-div) ---
    t_supg_c = Term.new('dw_st_supg_c(stabil.delta, v, b, u)', integral, omega,
                        stabil=m_stabil, v=v, b=b, u=u)
    t_supg_p = Term.new('dw_st_supg_p(stabil.delta, v, b, p)', integral, omega,
                        stabil=m_stabil, v=v, b=b, p=p)
    t_graddiv = Term.new('dw_st_grad_div(stabil.gamma, v, u)', integral, omega,
                         stabil=m_stabil, v=v, u=u)

    # --- Continuity equation terms ---
    t_div = Term.new('dw_stokes(u, q)', integral, omega,
                     u=u, q=q)

    # --- Stabilization terms (PSPG) ---
    t_pspg_c = Term.new('dw_st_pspg_c(stabil.tau, q, b, u)', integral, omega,
                        stabil=m_stabil, q=q, b=b, u=u)
    t_pspg_p = Term.new('dw_st_pspg_p(stabil.tau, q, p)', integral, omega,
                        stabil=m_stabil, q=q, p=p)

    # =========================================================================
    # ASSEMBLE EQUATIONS
    # =========================================================================
    # Momentum balance WITH Rayleigh friction term
    # Note: t_drag is ADDED (not subtracted) because the weak form of -αu gives +α∫v·u
    eq_momentum = Equation('balance',
                           t_diff + t_conv + t_drag - t_press + t_graddiv + t_supg_c + t_supg_p)

    # Mass conservation (incompressibility)
    eq_continuity = Equation('incompressibility', t_div + t_pspg_c + t_pspg_p)

    equations = Equations([eq_momentum, eq_continuity])

    print("\n" + "="*60)
    print("EQUATION SUMMARY")
    print("="*60)
    print("Momentum: diffusion + convection + DRAG - pressure + stabilization")
    print("Continuity: divergence + PSPG stabilization")
    print(f"Rayleigh friction coefficient α = {alpha_value:.4e} s⁻¹")
    print("="*60 + "\n")

    # =========================================================================
    # PROBLEM & SOLVER SETUP
    # =========================================================================
    pb = Problem('quasi2d_navier_stokes', equations=equations)
    pb.set_bcs(ebcs=bcs)

    # Direct solver (robust for moderate problem sizes)
    ls = ScipyDirect({})

    # Alternative: Iterative solver for large problems
    # ls = ScipyIterative({
    #     'method': 'gmres',
    #     'i_max': 15000,
    #     'eps_a': 1e-9,
    #     'eps_r': 1e-9,
    # })

    # Oseen iteration (Picard linearization of convection term)
    conf_oseen = Struct(
        name='oseen',
        kind='nls.oseen',
        stabil_mat='stabil',
        i_max=max_iters,
        eps_a=1e-6,
        eps_r=1e-4,
        macheps=1e-16,
        lin_red=1e-2,
        check_navier_stokes_residual=False,
        problem=pb,
    )

    nls = Oseen(conf_oseen, lin_solver=ls, status={})
    nls.conf.problem = pb
    pb.set_solver(nls)

    # =========================================================================
    # INITIALIZATION
    # =========================================================================
    variables = pb.get_variables()
    u_dof_coords = field_u.get_coor()
    n_dofs = u_dof_coords.shape[0]

    # Initialize convective velocity b with inlet profile
    b_data = np.zeros((n_dofs, 2))
    for i in range(n_dofs):
        x, y = u_dof_coords[i, 0], u_dof_coords[i, 1]
        b_data[i] = get_initial_velocity_at_point(x, y, x_min, x_max, y_min, y_max, inlet_velocity)
    variables['b'].set_data(b_data.ravel())

    # =========================================================================
    # SOLVE
    # =========================================================================
    print("Starting Oseen iteration...")
    state = pb.solve()

    # =========================================================================
    # POST-PROCESSING
    # =========================================================================
    out = state.create_output()
    u_vals = out['u'].data

    X, Y, Vx_grid, Vy_grid, vel_mag = postprocess_solution(
        u_vals, mesh, geom_data
    )
    ny, nx = X.shape
    parameter_string = get_parameter_string(geom_data)

    cache_dir, scenario_name = get_cache_dir(args.scenario)

    # Save velocity fields
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vx.txt", Vx_grid,
               header=f'{ny}_{nx}_{parameter_string}')
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vy.txt", Vy_grid,
               header=f'{ny}_{nx}_{parameter_string}')

    # Timing summary
    solve_time = time.time()
    print(f"\nOseen solver time: {solve_time - mesh_time:.2f} s")
    print(f"Total time: {time.time() - start_time:.2f} s")

    # Generate visualization
    plot_results(mesh, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"],
                 f"{cache_dir / scenario_name}_{args.hash}_results.png")

    # Cleanup
    if os.path.exists("domain.vtk"):
        os.remove("domain.vtk")


if __name__ == '__main__':
    main()