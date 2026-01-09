"""
LBM Airflow Simulation with Smagorinsky Turbulence Model
=========================================================
Pure NumPy implementation for 2D steady-state airflow in pedestrian simulations.

Key Features:
- D2Q9 lattice with BGK collision operator
- Smagorinsky Subgrid-Scale (SGS) turbulence model for realistic mixing
- Zou-He velocity boundary conditions for inlets
- Extrapolation/convective outflow for outlets
- Halfway bounce-back for walls and obstacles
- Vectorized NumPy operations for performance

Author: Claude (Anthropic)
Purpose: Airflow precomputation for aerosol transport in pedestrian dynamics
"""

import os
os.environ['OPENBLAS_NUM_THREADS'] = '1'
os.environ['MKL_NUM_THREADS'] = '1'
os.environ['NUMEXPR_NUM_THREADS'] = '1'
os.environ['OMP_NUM_THREADS'] = '1'

import time
import argparse
import sys
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.path import Path as MplPath
from pathlib import Path
from scipy.interpolate import RegularGridInterpolator

from helpers import extract_attributes, get_cache_dir, get_parameter_string

# =============================================================================
# D2Q9 LATTICE CONSTANTS
# =============================================================================
D2Q9_CX = np.array([0, 1, 0, -1, 0, 1, -1, -1, 1], dtype=np.float64)
D2Q9_CY = np.array([0, 0, 1, 0, -1, 1, 1, -1, -1], dtype=np.float64)
D2Q9_W = np.array([4/9, 1/9, 1/9, 1/9, 1/9, 1/36, 1/36, 1/36, 1/36], dtype=np.float64)
D2Q9_OPP = np.array([0, 3, 4, 1, 2, 7, 8, 5, 6], dtype=np.int32)
CS2 = 1.0 / 3.0
CS4 = CS2 * CS2


# =============================================================================
# HELPER FUNCTIONS
# =============================================================================

def rasterize_polygon(mask, vertices):
    """Rasterize a polygon onto a boolean mask using matplotlib's path."""
    nx, ny = mask.shape
    vertices = np.array(vertices)
    path = MplPath(vertices)
    x_coords = np.arange(nx)
    y_coords = np.arange(ny)
    xx, yy = np.meshgrid(x_coords, y_coords, indexing='ij')
    points = np.column_stack([xx.ravel(), yy.ravel()])
    inside = path.contains_points(points).reshape(nx, ny)
    mask |= inside


# =============================================================================
# LBM SIMULATION CLASS
# =============================================================================

class LBMSimulation:
    """
    Lattice Boltzmann simulation with Smagorinsky turbulence model.
    """
    SIDE_LEFT = 0
    SIDE_RIGHT = 1
    SIDE_BOTTOM = 2
    SIDE_TOP = 3

    def __init__(self, nx, ny, omega, c_smagorinsky=0.1):
        """
        Initialize the LBM simulation.

        Parameters
        ----------
        nx, ny : int
            Grid dimensions
        omega : float
            Base relaxation rate (1/tau). Should be in (0, 2) for stability.
        c_smagorinsky : float
            Smagorinsky constant. Typical values: 0.1-0.2.
            Higher values = more turbulent viscosity = more mixing.
        """
        self.nx = nx
        self.ny = ny
        self.omega = omega
        self.c_smag = c_smagorinsky

        # Distribution functions
        self.f = np.zeros((nx, ny, 9), dtype=np.float64)
        self.f_post = np.zeros_like(self.f)

        # Macroscopic fields
        self.rho = np.ones((nx, ny), dtype=np.float64)
        self.ux = np.zeros((nx, ny), dtype=np.float64)
        self.uy = np.zeros((nx, ny), dtype=np.float64)

        # Initialize to equilibrium at rest
        for q in range(9):
            self.f[:, :, q] = D2Q9_W[q]

        # Boundary masks
        self.wall_mask = np.zeros((nx, ny), dtype=np.bool_)
        self.inlets = []
        self.outlets = []

    def add_wall_border(self, thickness=1):
        """Add solid walls at all domain boundaries."""
        t = thickness
        self.wall_mask[:t, :] = True
        self.wall_mask[-t:, :] = True
        self.wall_mask[:, :t] = True
        self.wall_mask[:, -t:] = True

    def carve_opening(self, side, start_idx, end_idx, thickness=1):
        """Remove wall in a region to create an opening."""
        t = thickness
        if side == self.SIDE_LEFT:
            self.wall_mask[:t, start_idx:end_idx] = False
        elif side == self.SIDE_RIGHT:
            self.wall_mask[-t:, start_idx:end_idx] = False
        elif side == self.SIDE_BOTTOM:
            self.wall_mask[start_idx:end_idx, :t] = False
        elif side == self.SIDE_TOP:
            self.wall_mask[start_idx:end_idx, -t:] = False

    def add_inlet(self, side, start_idx, end_idx, u_magnitude, thickness=1):
        """Add a velocity inlet boundary."""
        mask = np.zeros((self.nx, self.ny), dtype=np.bool_)
        t = thickness

        if side == self.SIDE_LEFT:
            mask[:t, start_idx:end_idx] = True
            u_vec = (u_magnitude, 0.0)
        elif side == self.SIDE_RIGHT:
            mask[-t:, start_idx:end_idx] = True
            u_vec = (-u_magnitude, 0.0)
        elif side == self.SIDE_BOTTOM:
            mask[start_idx:end_idx, :t] = True
            u_vec = (0.0, u_magnitude)
        elif side == self.SIDE_TOP:
            mask[start_idx:end_idx, -t:] = True
            u_vec = (0.0, -u_magnitude)

        self.inlets.append({'mask': mask, 'u': u_vec, 'side': side})
        self.carve_opening(side, start_idx, end_idx, thickness)

    def add_outlet(self, side, start_idx, end_idx, thickness=1):
        """Add an outlet boundary with extrapolation BC."""
        mask = np.zeros((self.nx, self.ny), dtype=np.bool_)
        t = thickness

        if side == self.SIDE_LEFT:
            mask[:t, start_idx:end_idx] = True
        elif side == self.SIDE_RIGHT:
            mask[-t:, start_idx:end_idx] = True
        elif side == self.SIDE_BOTTOM:
            mask[start_idx:end_idx, :t] = True
        elif side == self.SIDE_TOP:
            mask[start_idx:end_idx, -t:] = True

        self.outlets.append({'mask': mask, 'side': side})
        self.carve_opening(side, start_idx, end_idx, thickness)

    def add_obstacle_rectangle(self, x1, y1, x2, y2):
        """Add a rectangular obstacle."""
        x1, x2 = int(min(x1, x2)), int(max(x1, x2))
        y1, y2 = int(min(y1, y2)), int(max(y1, y2))
        self.wall_mask[x1:x2+1, y1:y2+1] = True

    def add_obstacle_polygon(self, vertices):
        """Add a polygon obstacle."""
        rasterize_polygon(self.wall_mask, vertices)

    def _compute_equilibrium(self):
        """Compute equilibrium distribution for entire field."""
        feq = np.zeros((self.nx, self.ny, 9), dtype=np.float64)
        usq = self.ux**2 + self.uy**2

        for q in range(9):
            cu = D2Q9_CX[q] * self.ux + D2Q9_CY[q] * self.uy
            feq[:, :, q] = D2Q9_W[q] * self.rho * (
                1.0 + cu / CS2 + 0.5 * cu**2 / CS4 - 0.5 * usq / CS2
            )
        return feq

    def _compute_macroscopic(self):
        """Compute density and velocity from distributions."""
        self.rho = np.sum(self.f, axis=2)
        mx = np.sum(self.f * D2Q9_CX[np.newaxis, np.newaxis, :], axis=2)
        my = np.sum(self.f * D2Q9_CY[np.newaxis, np.newaxis, :], axis=2)
        rho_safe = np.maximum(self.rho, 1e-10)
        self.ux = mx / rho_safe
        self.uy = my / rho_safe

    def _compute_strain_magnitude(self):
        """Compute strain rate magnitude for Smagorinsky model."""
        feq = self._compute_equilibrium()
        fneq = self.f - feq

        Pi_xx = np.zeros_like(self.rho)
        Pi_xy = np.zeros_like(self.rho)
        Pi_yy = np.zeros_like(self.rho)

        for q in range(9):
            Pi_xx += (D2Q9_CX[q]**2 - CS2) * fneq[:, :, q]
            Pi_xy += D2Q9_CX[q] * D2Q9_CY[q] * fneq[:, :, q]
            Pi_yy += (D2Q9_CY[q]**2 - CS2) * fneq[:, :, q]

        Q = Pi_xx**2 + 2.0 * Pi_xy**2 + Pi_yy**2
        return np.sqrt(2.0 * np.maximum(Q, 0.0))

    def _collision_smagorinsky(self):
        """BGK collision with Smagorinsky turbulence model."""
        S_mag = self._compute_strain_magnitude()

        # Clip strain magnitude for stability
        S_mag = np.clip(S_mag, 0, 100)

        tau_base = 1.0 / self.omega
        discriminant = tau_base**2 + 18.0 * self.c_smag**2 * S_mag / np.maximum(self.rho, 1e-10)
        tau_eff = 0.5 * (tau_base + np.sqrt(np.maximum(discriminant, tau_base**2)))
        omega_eff = 1.0 / tau_eff

        # Clip omega for stability
        omega_eff = np.clip(omega_eff, 0.5, 1.95)

        feq = self._compute_equilibrium()

        # BGK collision with stability check
        f_new = self.f - omega_eff[:, :, np.newaxis] * (self.f - feq)

        # Check for NaN and replace with equilibrium
        nan_mask = np.any(np.isnan(f_new), axis=2) | np.any(np.isinf(f_new), axis=2)
        if np.any(nan_mask):
            f_new[nan_mask] = feq[nan_mask]

        self.f = f_new

    def _streaming(self):
        """Streaming step using np.roll."""
        f_new = np.zeros_like(self.f)
        for q in range(9):
            f_new[:, :, q] = np.roll(
                np.roll(self.f[:, :, q], int(D2Q9_CX[q]), axis=0),
                int(D2Q9_CY[q]), axis=1
            )
        self.f = f_new

    def _apply_bounce_back(self):
        """Halfway bounce-back at walls."""
        for q in range(9):
            self.f[self.wall_mask, q] = self.f_post[self.wall_mask, D2Q9_OPP[q]]

    def _apply_inlet_bc(self, ramp=1.0):
        """Apply Zou-He velocity BC at inlets."""
        for inlet in self.inlets:
            mask = inlet['mask']
            u_x, u_y = inlet['u']
            u_x *= ramp
            u_y *= ramp
            side = inlet['side']

            idx = np.where(mask)
            if len(idx[0]) == 0:
                continue

            if side == self.SIDE_LEFT:
                rho_in = (self.f[idx[0], idx[1], 0] + self.f[idx[0], idx[1], 2] +
                          self.f[idx[0], idx[1], 4] + 2.0 * (self.f[idx[0], idx[1], 3] +
                          self.f[idx[0], idx[1], 6] + self.f[idx[0], idx[1], 7])) / (1.0 - u_x)

                self.f[idx[0], idx[1], 1] = self.f[idx[0], idx[1], 3] + (2.0/3.0) * rho_in * u_x
                self.f[idx[0], idx[1], 5] = (self.f[idx[0], idx[1], 7] -
                    0.5*(self.f[idx[0], idx[1], 2] - self.f[idx[0], idx[1], 4]) +
                    (1.0/6.0)*rho_in*u_x + 0.5*rho_in*u_y)
                self.f[idx[0], idx[1], 8] = (self.f[idx[0], idx[1], 6] +
                    0.5*(self.f[idx[0], idx[1], 2] - self.f[idx[0], idx[1], 4]) +
                    (1.0/6.0)*rho_in*u_x - 0.5*rho_in*u_y)

            elif side == self.SIDE_RIGHT:
                rho_in = (self.f[idx[0], idx[1], 0] + self.f[idx[0], idx[1], 2] +
                          self.f[idx[0], idx[1], 4] + 2.0 * (self.f[idx[0], idx[1], 1] +
                          self.f[idx[0], idx[1], 5] + self.f[idx[0], idx[1], 8])) / (1.0 + u_x)

                self.f[idx[0], idx[1], 3] = self.f[idx[0], idx[1], 1] - (2.0/3.0) * rho_in * u_x
                self.f[idx[0], idx[1], 7] = (self.f[idx[0], idx[1], 5] +
                    0.5*(self.f[idx[0], idx[1], 2] - self.f[idx[0], idx[1], 4]) -
                    (1.0/6.0)*rho_in*u_x - 0.5*rho_in*u_y)
                self.f[idx[0], idx[1], 6] = (self.f[idx[0], idx[1], 8] -
                    0.5*(self.f[idx[0], idx[1], 2] - self.f[idx[0], idx[1], 4]) -
                    (1.0/6.0)*rho_in*u_x + 0.5*rho_in*u_y)

            elif side == self.SIDE_BOTTOM:
                rho_in = (self.f[idx[0], idx[1], 0] + self.f[idx[0], idx[1], 1] +
                          self.f[idx[0], idx[1], 3] + 2.0 * (self.f[idx[0], idx[1], 4] +
                          self.f[idx[0], idx[1], 7] + self.f[idx[0], idx[1], 8])) / (1.0 - u_y)

                self.f[idx[0], idx[1], 2] = self.f[idx[0], idx[1], 4] + (2.0/3.0) * rho_in * u_y
                self.f[idx[0], idx[1], 5] = (self.f[idx[0], idx[1], 7] -
                    0.5*(self.f[idx[0], idx[1], 1] - self.f[idx[0], idx[1], 3]) +
                    0.5*rho_in*u_x + (1.0/6.0)*rho_in*u_y)
                self.f[idx[0], idx[1], 6] = (self.f[idx[0], idx[1], 8] +
                    0.5*(self.f[idx[0], idx[1], 1] - self.f[idx[0], idx[1], 3]) -
                    0.5*rho_in*u_x + (1.0/6.0)*rho_in*u_y)

            elif side == self.SIDE_TOP:
                rho_in = (self.f[idx[0], idx[1], 0] + self.f[idx[0], idx[1], 1] +
                          self.f[idx[0], idx[1], 3] + 2.0 * (self.f[idx[0], idx[1], 2] +
                          self.f[idx[0], idx[1], 5] + self.f[idx[0], idx[1], 6])) / (1.0 + u_y)

                self.f[idx[0], idx[1], 4] = self.f[idx[0], idx[1], 2] - (2.0/3.0) * rho_in * u_y
                self.f[idx[0], idx[1], 7] = (self.f[idx[0], idx[1], 5] +
                    0.5*(self.f[idx[0], idx[1], 1] - self.f[idx[0], idx[1], 3]) -
                    0.5*rho_in*u_x - (1.0/6.0)*rho_in*u_y)
                self.f[idx[0], idx[1], 8] = (self.f[idx[0], idx[1], 6] -
                    0.5*(self.f[idx[0], idx[1], 1] - self.f[idx[0], idx[1], 3]) +
                    0.5*rho_in*u_x - (1.0/6.0)*rho_in*u_y)

    def _apply_outlet_bc(self):
        """Apply extrapolation BC at outlets."""
        for outlet in self.outlets:
            mask = outlet['mask']
            side = outlet['side']

            if side == self.SIDE_LEFT:
                self.f[0, :, :][mask[0, :]] = self.f[1, :, :][mask[0, :]]
            elif side == self.SIDE_RIGHT:
                self.f[-1, :, :][mask[-1, :]] = self.f[-2, :, :][mask[-1, :]]
            elif side == self.SIDE_BOTTOM:
                self.f[:, 0, :][mask[:, 0]] = self.f[:, 1, :][mask[:, 0]]
            elif side == self.SIDE_TOP:
                self.f[:, -1, :][mask[:, -1]] = self.f[:, -2, :][mask[:, -1]]

    def initialize_with_inlet_velocity(self):
        """Initialize the domain with inlet velocity for faster convergence."""
        total_ux, total_uy, count = 0.0, 0.0, 0
        for inlet in self.inlets:
            n = np.sum(inlet['mask'])
            if n > 0:
                total_ux += inlet['u'][0] * n
                total_uy += inlet['u'][1] * n
                count += n

        if count > 0:
            self.ux[~self.wall_mask] = (total_ux / count) * 0.5
            self.uy[~self.wall_mask] = (total_uy / count) * 0.5

        # Initialize distributions to equilibrium
        feq = self._compute_equilibrium()
        self.f = feq.copy()

    def step(self, ramp=1.0):
        """Perform one LBM timestep."""
        self._compute_macroscopic()
        self._collision_smagorinsky()
        np.copyto(self.f_post, self.f)
        self._streaming()
        self._apply_bounce_back()
        self._apply_inlet_bc(ramp)
        self._apply_outlet_bc()

    def run(self, max_steps=100000, ramp_steps=5000, check_interval=1000,
            convergence_threshold=1e-6, verbose=True):
        """Run simulation to steady state."""
        self.initialize_with_inlet_velocity()
        prev_u_mag = None

        for step in range(1, max_steps + 1):
            ramp = min(1.0, step / ramp_steps)
            self.step(ramp)

            if step % check_interval == 0:
                self._compute_macroscopic()
                u_mag = np.sqrt(self.ux**2 + self.uy**2)
                u_mag_fluid = u_mag[~self.wall_mask]

                if prev_u_mag is not None:
                    diff = np.max(np.abs(u_mag_fluid - prev_u_mag))
                    if verbose:
                        print(f"Step {step}: max_change={diff:.2e}, "
                              f"max_vel={np.max(u_mag_fluid):.4f}")
                    if diff < convergence_threshold:
                        if verbose:
                            print(f"Converged at step {step}")
                        return True, step

                prev_u_mag = u_mag_fluid.copy()

        if verbose:
            print(f"Did not converge in {max_steps} steps")
        return False, max_steps

    def get_velocity_field(self):
        """Return velocity field."""
        self._compute_macroscopic()
        return self.ux.copy(), self.uy.copy()


# =============================================================================
# PARAMETER COMPUTATION
# =============================================================================

def compute_stable_parameters(u_phys, nu_phys, L_phys, dx,
                               target_u_lb=0.05, max_omega=1.85):
    """
    Compute LBM parameters ensuring stability.

    For high Reynolds number flows (Re > 1000), we use an effective
    turbulent viscosity approach where the Smagorinsky model provides
    the additional dissipation needed for stability.
    """
    Re = u_phys * L_phys / nu_phys

    # For very high Re, we need to use effective turbulent viscosity
    # The Smagorinsky model will add nu_t ~ (C_s * dx)^2 * |S|
    # We estimate this and adjust parameters accordingly
    if Re > 1000:
        print(f"High Reynolds number detected (Re={Re:.0f})")
        print("Using effective turbulent viscosity approach...")
        # Effective viscosity estimate for turbulent flow
        # nu_eff ~ u * L / Re_eff where Re_eff ~ 100-500 is stable for LBM
        Re_eff = 200  # Target effective Reynolds number for stability
        nu_eff = u_phys * L_phys / Re_eff
        print(f"Effective viscosity: nu_eff = {nu_eff:.2e} (vs nu_phys = {nu_phys:.2e})")
        nu_for_calc = nu_eff
    else:
        nu_for_calc = nu_phys

    u_lb = target_u_lb
    dt = u_lb * dx / u_phys
    nu_lb = nu_for_calc * dt / (dx * dx)
    omega = 1.0 / (3.0 * nu_lb + 0.5)

    if omega > max_omega:
        print(f"Adjusting omega from {omega:.3f} to {max_omega}")
        omega = max_omega
        nu_lb = (1.0 / omega - 0.5) / 3.0
        dt = nu_lb * dx * dx / nu_for_calc
        u_lb = u_phys * dt / dx

    if omega < 0.6:
        print(f"Warning: omega={omega:.3f} is very low, increasing...")
        omega = 0.6
        nu_lb = (1.0 / omega - 0.5) / 3.0
        dt = nu_lb * dx * dx / nu_for_calc
        u_lb = u_phys * dt / dx

    Ma = u_lb / np.sqrt(CS2)

    # Safety check for Mach number
    if Ma > 0.3:
        print(f"Warning: Ma={Ma:.3f} > 0.3, reducing lattice velocity")
        u_lb = 0.3 * np.sqrt(CS2)
        dt = u_lb * dx / u_phys
        nu_lb = nu_for_calc * dt / (dx * dx)
        omega = 1.0 / (3.0 * nu_lb + 0.5)
        omega = max(0.6, min(omega, max_omega))
        Ma = u_lb / np.sqrt(CS2)

    print(f"\n=== LBM Parameters ===")
    print(f"Physical Reynolds: Re = {Re:.1f}")
    print(f"Lattice velocity: u_lb = {u_lb:.5f}")
    print(f"Timestep: dt = {dt:.2e} s")
    print(f"Lattice viscosity: nu_lb = {nu_lb:.5f}")
    print(f"Relaxation rate: omega = {omega:.4f}")
    print(f"Mach number: Ma = {Ma:.4f}")
    print(f"=====================\n")

    return u_lb, dt, nu_lb, omega, Re


# =============================================================================
# GEOMETRY SETUP
# =============================================================================

def setup_geometry_from_scenario(geom_data, dx):
    """Create LBM simulation from parsed Vadere scenario geometry."""
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    Lx = x_max - x_min
    Ly = y_max - y_min
    nx = int(np.ceil(Lx / dx)) + 1
    ny = int(np.ceil(Ly / dx)) + 1

    print(f"Domain: [{x_min}, {x_max}] x [{y_min}, {y_max}]")
    print(f"Grid: {nx} x {ny} = {nx*ny} cells (dx = {dx} m)")

    u_phys = geom_data['inlet_velocity']
    nu_phys = geom_data['viscosity']

    inlet_widths = [abs(inlet['coords'][1] - inlet['coords'][0])
                    for inlet in geom_data['inlets']]
    L_char = min(inlet_widths) if inlet_widths else Lx

    u_lb, dt, nu_lb, omega, Re = compute_stable_parameters(
        u_phys, nu_phys, L_char, dx, target_u_lb=0.05, max_omega=1.85
    )

    c_smag = 0.12
    sim = LBMSimulation(nx, ny, omega, c_smagorinsky=c_smag)
    sim.add_wall_border(thickness=1)

    def side_to_enum(side_str):
        return {'left': sim.SIDE_LEFT, 'right': sim.SIDE_RIGHT,
                'bottom': sim.SIDE_BOTTOM, 'top': sim.SIDE_TOP}[side_str]

    for inlet in geom_data['inlets']:
        side = inlet['side']
        c0, c1 = inlet['coords']
        if side in ['left', 'right']:
            start_idx = max(1, int((c0 - y_min) / dx))
            end_idx = min(ny - 1, int((c1 - y_min) / dx))
        else:
            start_idx = max(1, int((c0 - x_min) / dx))
            end_idx = min(nx - 1, int((c1 - x_min) / dx))
        sim.add_inlet(side_to_enum(side), start_idx, end_idx, u_lb, thickness=1)
        print(f"Added inlet: side={side}, range=[{start_idx}, {end_idx}]")

    for outlet in geom_data['outlets']:
        side = outlet['side']
        c0, c1 = outlet['coords']
        if side in ['left', 'right']:
            start_idx = max(1, int((c0 - y_min) / dx))
            end_idx = min(ny - 1, int((c1 - y_min) / dx))
        else:
            start_idx = max(1, int((c0 - x_min) / dx))
            end_idx = min(nx - 1, int((c1 - x_min) / dx))
        sim.add_outlet(side_to_enum(side), start_idx, end_idx, thickness=1)
        print(f"Added outlet: side={side}, range=[{start_idx}, {end_idx}]")

    for obs_pts in geom_data['obstacles']:
        grid_pts = [((px - x_min) / dx, (py - y_min) / dx) for px, py in obs_pts]
        sim.add_obstacle_polygon(grid_pts)
        print(f"Added obstacle with {len(obs_pts)} vertices")

    scale = u_phys / u_lb
    return sim, u_lb, scale, dt, (nx, ny), (x_min, x_max, y_min, y_max)


# =============================================================================
# POST-PROCESSING
# =============================================================================

def postprocess_to_rectangular_grid(sim, scale, geom_data, bounds):
    """Interpolate LBM results to Vadere rectangular grid format."""
    x_min, x_max, y_min, y_max = bounds
    res = geom_data.get('rect_grid_cell_size', 0.1)

    nx_out = int(np.round((x_max - x_min) / res)) + 1
    ny_out = int(np.round((y_max - y_min) / res)) + 1

    x_rng = np.linspace(x_min, x_max, nx_out)
    y_rng = np.linspace(y_min, y_max, ny_out)
    X, Y = np.meshgrid(x_rng, y_rng)

    ux_lb, uy_lb = sim.get_velocity_field()

    x_lb = np.linspace(x_min, x_max, sim.nx)
    y_lb = np.linspace(y_min, y_max, sim.ny)

    interp_ux = RegularGridInterpolator((x_lb, y_lb), ux_lb,
                                        method='linear', bounds_error=False, fill_value=0.0)
    interp_uy = RegularGridInterpolator((x_lb, y_lb), uy_lb,
                                        method='linear', bounds_error=False, fill_value=0.0)

    points = np.stack([X, Y], axis=-1)
    Vx = interp_ux(points) * scale
    Vy = interp_uy(points) * scale

    for obs_pts in geom_data['obstacles']:
        path = MplPath(obs_pts)
        points_flat = np.column_stack([X.ravel(), Y.ravel()])
        inside = path.contains_points(points_flat).reshape(X.shape)
        Vx[inside] = 0.0
        Vy[inside] = 0.0

    vel_mag = np.hypot(Vx, Vy)

    print(f"\nOutput grid: {nx_out} x {ny_out} points (res={res}m)")
    print(f"Max velocity magnitude: {np.max(vel_mag):.4f} m/s")
    print(f"Mean velocity magnitude: {np.mean(vel_mag):.4f} m/s")

    return X, Y, Vx, Vy, vel_mag


def plot_results(X, Y, Vx, Vy, vel_mag, obstacles, path=None):
    """Plot velocity field results."""
    x_min, x_max = np.min(X), np.max(X)
    y_min, y_max = np.min(Y), np.max(Y)
    aspect = (x_max - x_min) / max(y_max - y_min, 0.1)

    fig, axes = plt.subplots(1, 2, figsize=(max(12 * aspect, 12), 6),
                             sharex=True, sharey=True, constrained_layout=True)

    vmax = np.max(vel_mag) if np.max(vel_mag) > 0 else 1.0
    levels = np.linspace(0, vmax, 50)

    for ax in axes:
        for obs in obstacles:
            obs_arr = np.array(obs)
            ax.fill(obs_arr[:, 0], obs_arr[:, 1], color='grey', zorder=10)

    cf1 = axes[0].contourf(X, Y, vel_mag, levels=levels, cmap='viridis', extend='max')
    speed = np.sqrt(Vx**2 + Vy**2)
    Vx_safe = np.where(speed > vmax * 1e-6, Vx, vmax * 1e-6)
    Vy_safe = np.where(speed > vmax * 1e-6, Vy, 0.0)
    try:
        axes[0].streamplot(X, Y, Vx_safe, Vy_safe, color='white', linewidth=0.5, density=1.5)
    except:
        pass
    axes[0].set_title("Streamlines")
    fig.colorbar(cf1, ax=axes[0], label='Velocity (m/s)')

    cf2 = axes[1].contourf(X, Y, vel_mag, levels=levels, cmap='viridis', extend='max')
    skip = max(1, len(X) // 25)
    axes[1].quiver(X[::skip, ::skip], Y[::skip, ::skip],
                   Vx[::skip, ::skip], Vy[::skip, ::skip],
                   color='white', alpha=0.8, scale=vmax*15)
    axes[1].set_title("Velocity Vectors")
    fig.colorbar(cf2, ax=axes[1], label='Velocity (m/s)')

    for ax in axes:
        ax.set_xlabel("x (m)")
        ax.set_ylabel("y (m)")
        ax.set_aspect('equal')
        ax.set_xlim(x_min, x_max)
        ax.set_ylim(y_min, y_max)

    if path:
        plt.savefig(path, dpi=150, bbox_inches='tight')
        print(f"Saved: {path}")
    plt.close()


# =============================================================================
# MAIN
# =============================================================================

def main():
    start_time = time.time()

    parser = argparse.ArgumentParser(description='LBM Airflow Simulation')
    parser.add_argument('scenario', help='Path to Vadere scenario JSON file')
    parser.add_argument('hash', help='Unique hash for output files')
    parser.add_argument('--max-steps', type=int, default=100000)
    parser.add_argument('--convergence', type=float, default=1e-6)
    parser.add_argument('--dx', type=float, default=None)
    args = parser.parse_args()

    print("=" * 60)
    print("LBM Airflow Simulation with Smagorinsky Turbulence Model")
    print("=" * 60)

    geom_data = extract_attributes(args.scenario)
    dx = args.dx if args.dx else geom_data.get('max_triangle_edge_len', 0.1)

    sim, u_lb, scale, dt, grid_size, bounds = setup_geometry_from_scenario(geom_data, dx)

    print(f"\nSetup time: {time.time() - start_time:.2f} s")
    print("\nRunning LBM simulation...")

    solve_start = time.time()
    converged, final_step = sim.run(
        max_steps=args.max_steps,
        ramp_steps=min(5000, args.max_steps // 10),
        check_interval=max(500, args.max_steps // 100),
        convergence_threshold=args.convergence
    )

    print(f"\nSolver time: {time.time() - solve_start:.2f} s")

    X, Y, Vx, Vy, vel_mag = postprocess_to_rectangular_grid(sim, scale, geom_data, bounds)

    ny, nx = X.shape
    parameter_string = get_parameter_string(geom_data)
    cache_dir, scenario_name = get_cache_dir(args.scenario)

    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vx.txt", Vx,
               header=f'{ny}_{nx}_{parameter_string}')
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vy.txt", Vy,
               header=f'{ny}_{nx}_{parameter_string}')

    plot_results(X, Y, Vx, Vy, vel_mag, geom_data['obstacles'],
                path=f"{cache_dir / scenario_name}_{args.hash}_results.png")

    print(f"\nTotal time: {time.time() - start_time:.2f} s")


# =============================================================================
# DEMO
# =============================================================================

def demo_room_with_mixing():
    """Demo: Room with turbulent mixing."""
    print("=" * 60)
    print("Demo: Room with Turbulent Mixing")
    print("=" * 60)

    # Smaller grid for faster demo
    nx, ny = 100, 70
    u_phys = 0.5  # m/s - typical HVAC airflow
    nu_phys = 1.5e-5  # m²/s - air kinematic viscosity
    dx = 0.03  # m - grid spacing (coarser for speed)
    inlet_width = 10
    L_char = inlet_width * dx

    u_lb, dt, nu_lb, omega, Re = compute_stable_parameters(
        u_phys, nu_phys, L_char, dx, target_u_lb=0.04
    )

    sim = LBMSimulation(nx, ny, omega, c_smagorinsky=0.15)
    sim.add_wall_border(thickness=2)

    inlet_start = nx // 6
    inlet_end = inlet_start + inlet_width
    sim.add_inlet(sim.SIDE_TOP, inlet_start, inlet_end, u_lb, thickness=2)

    outlet_start = nx - nx // 4 - inlet_width
    outlet_end = outlet_start + inlet_width
    sim.add_outlet(sim.SIDE_BOTTOM, outlet_start, outlet_end, thickness=2)

    sim.add_obstacle_rectangle(40, 25, 60, 35)
    sim.add_obstacle_rectangle(25, 50, 32, 60)
    sim.add_obstacle_rectangle(68, 50, 75, 60)

    converged, final_step = sim.run(max_steps=15000, check_interval=1500,
                                     convergence_threshold=5e-3)

    ux, uy = sim.get_velocity_field()
    scale = u_phys / u_lb
    vx, vy = ux * scale, uy * scale

    vx_T, vy_T = vx.T, vy.T
    mask_T = sim.wall_mask.T
    vel_mag = np.sqrt(vx_T**2 + vy_T**2)
    vel_masked = np.ma.masked_where(mask_T, vel_mag)

    X, Y = np.meshgrid(np.arange(nx), np.arange(ny))

    fig, ax = plt.subplots(figsize=(12, 8))
    levels = np.linspace(0, np.max(vel_mag), 50)
    cf = ax.contourf(X, Y, vel_masked, levels=levels, cmap='viridis', extend='max')
    ax.imshow(mask_T, origin='lower', cmap='binary', alpha=0.3, extent=[0, nx, 0, ny])

    speed = np.sqrt(vx_T**2 + vy_T**2)
    vx_safe = np.where(speed > 1e-8, vx_T, 1e-8)
    vy_safe = np.where(speed > 1e-8, vy_T, 0)
    try:
        ax.streamplot(X, Y, vx_safe, vy_safe, color='white', density=1.5, linewidth=0.5)
    except:
        pass

    ax.set_title(f'LBM Airflow with Smagorinsky Turbulence (Re={Re:.0f})')
    ax.set_xlabel('X (grid)')
    ax.set_ylabel('Y (grid)')
    ax.set_aspect('equal')
    fig.colorbar(cf, label='Velocity (m/s)')

    plt.tight_layout()
    plt.savefig('/home/claude/room_mixing_demo.png', dpi=150)
    print("Saved: /home/claude/room_mixing_demo.png")
    plt.close()

    return sim


if __name__ == '__main__':
    if len(sys.argv) > 1 and not sys.argv[1].startswith('--demo'):
        main()
    else:
        demo_room_with_mixing()