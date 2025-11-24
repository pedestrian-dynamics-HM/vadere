from skfem.visuals.matplotlib import draw
from matplotlib import pyplot as plt
import numpy as np


def plot_results(mesh, X, Y, Vx, Vy, vel_mag, obstacles):
    """
    Plots Mesh, Streamlines, and Vectors side-by-side with equal sizing.
    """
    from skfem.visuals.matplotlib import draw

    # 1. Calculate aspect ratio to determine Figure dimensions
    # We want 3 columns.
    x_min, x_max = np.min(X), np.max(X)
    y_min, y_max = np.min(Y), np.max(Y)

    domain_width = x_max - x_min
    domain_height = y_max - y_min

    # If height is small vs width, we need a wide figure.
    # We aim for a standard height of 5 inches.
    plot_height = 5.0
    plot_width = plot_height * (domain_width / domain_height)

    # Total fig width = 3 plots * width per plot
    fig_width = plot_width * 3

    # 2. Setup Plot with constrained_layout (keeps sizes consistent)
    # sharex/sharey ensures they look identical and you only need labels on the left
    fig, axes = plt.subplots(1, 3,
                             figsize=(fig_width, plot_height),
                             sharex=True,
                             sharey=True,
                             constrained_layout=True)

    ax_mesh, ax_stream, ax_quiver = axes

    # --- Helper to draw obstacles on any axis ---
    def draw_obstacles(ax):
        for obs in obstacles:
            x_coords = [vertex[0] for vertex in obs]
            y_coords = [vertex[1] for vertex in obs]
            ax.fill(x_coords, y_coords, color='grey', alpha=1.0, zorder=10)

    # --- Plot 1: Mesh (Left) ---
    draw(mesh, ax=ax_mesh)
    draw_obstacles(ax_mesh)
    ax_mesh.set_title(f"Mesh")
    ax_mesh.set_ylabel("y (m)") # Only needed on far left due to sharey

    # --- Plot 2: Streamlines (Center) ---
    # Use same levels for both contour plots for consistency
    levels = np.linspace(0, np.max(vel_mag), 50)

    cf1 = ax_stream.contourf(X, Y, vel_mag, levels=levels, cmap='viridis')
    ax_stream.streamplot(X, Y, Vx, Vy, color='white', linewidth=0.5,
                         density=1.5, arrowsize=1, arrowstyle='->')
    draw_obstacles(ax_stream)
    ax_stream.set_title(f"Streamlines")

    # Add colorbar to the bottom of this specific axis (optional, or shared on right)
    # To keep sizes EXACTLY equal, usually better to put one colorbar on the far right
    # or let constrained_layout handle it. Here we add it to the plot.
    cb1 = fig.colorbar(cf1, ax=ax_stream, location='bottom', fraction=0.05, pad=0.05)
    cb1.set_label('Velocity (m/s)')

    # --- Plot 3: Vectors (Right) ---
    cf2 = ax_quiver.contourf(X, Y, vel_mag, levels=levels, cmap='viridis')
    # Scale: Adjust 'scale' (higher is shorter arrows) and 'width' as needed
    ax_quiver.quiver(X, Y, Vx, Vy, color='white', scale=10, width=0.003, alpha=0.8)
    draw_obstacles(ax_quiver)
    ax_quiver.set_title("Velocity Vectors")

    cb2 = fig.colorbar(cf2, ax=ax_quiver, location='bottom', fraction=0.05, pad=0.05)
    cb2.set_label('Velocity (m/s)')

    # Common X labels
    for ax in axes:
        ax.set_xlabel("x (m)")
        ax.set_aspect('equal', adjustable='box')

        # Reset limits because axis('equal') might slightly shift them
        ax.set_xlim(x_min, x_max)
        ax.set_ylim(y_min, y_max)

    plt.show()