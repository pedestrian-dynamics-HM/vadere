"""
Unit and integration tests for the airflow simulation project.

Covers:
  - helpers.py:  extract_attributes, convert_reynolds_to_viscosity,
                 get_initial_velocity_at_point, get_parameter_string,
                 postprocess_solution
  - build_mesh.py: build_mesh (geometry, boundary tagging, obstacles)

Plotting helpers and the full navier_stokes main() are excluded because
they are pure I/O / visualisation and require a full solver stack.
"""

import json
import tempfile
import os
import pytest
import numpy as np
from pathlib import Path
import sys
from unittest.mock import patch, MagicMock
sys.path.append(str(Path(__file__).resolve().parents[7] / "src"))
from org.vadere.simulator.models.airflow.python.helpers import *
from org.vadere.simulator.models.airflow.python.build_mesh import *
from sfepy.discrete.fem import Mesh


# helpers
def _minimal_scenario(
    bounds=None,
    inlets=None,
    outlets=None,
    obstacles=None,
    blocking_obstacles=None,
    airflow_bounds=None,
    reynolds=100.0,
    inlet_velocity=1.0,
    rect_grid_cell_size=0.5,
    max_triangle_edge_len=1.0,
    bounding_box_width=0.0,
):
    """Return a dict matching the Vadere JSON scenario schema."""
    if bounds is None:
        bounds = {"x": 0.0, "y": 0.0, "width": 10.0, "height": 5.0}
    if airflow_bounds is None:
        airflow_bounds = {
            "xmin": bounds["x"],
            "xmax": bounds["x"] + bounds["width"],
            "ymin": bounds["y"],
            "ymax": bounds["y"] + bounds["height"],
        }
    if inlets is None:
        inlets = [{"side": "west", "start": 1.0, "width": 2.0}]
    if outlets is None:
        outlets = [{"side": "east", "start": 1.0, "width": 2.0}]
    if obstacles is None:
        obstacles = []
    if blocking_obstacles is None:
        blocking_obstacles = []

    return {
        "scenario": {
            "topography": {
                "attributes": {
                    "bounds": bounds,
                    "boundingBoxWidth": bounding_box_width,
                },
                "obstacles": obstacles,
            },
            "attributesModel": {
                "org.vadere.state.attributes.models.airflow.AttributesAirFlowModel": {
                    "bounds": airflow_bounds,
                    "rectangularGridCellSize": rect_grid_cell_size,
                    "maxTriangleEdgeLen": max_triangle_edge_len,
                    "inletVelocity": inlet_velocity,
                    "reynoldsNumber": reynolds,
                    "blockingObstacles": blocking_obstacles,
                    "inlets": inlets,
                    "outlets": outlets,
                }
            },
        }
    }


def _write_scenario(tmp_path, scenario_dict, name="test_scenario.json"):
    """Write scenario dict to a JSON file and return the path."""
    p = tmp_path / name
    p.write_text(json.dumps(scenario_dict))
    return str(p)


def _make_geom_data(**overrides):
    """Return a minimal geom_data dict (as returned by extract_attributes)."""
    base = {
        "rect_grid_cell_size": 0.5,
        "max_triangle_edge_len": 1.0,
        "x_min": 0.0,
        "x_max": 10.0,
        "y_min": 0.0,
        "y_max": 5.0,
        "inlet_velocity": 1.0,
        "viscosity": 0.075,
        "inlets": [{"id": 0, "side": "west", "coords": [1.0, 3.0]}],
        "outlets": [{"id": 0, "side": "east", "coords": [1.0, 3.0]}],
        "obstacles": [],
    }
    base.update(overrides)
    return base


# helpers.py: convert_reynolds_to_viscosity
class TestConvertReynoldsToViscosity:
    """Tests for the Reynolds-to-viscosity conversion."""

    def test_basic_calculation(self):
        # char_length = ((10-0) + (5-0)) / 2 = 7.5
        # viscosity = (1.0 * 7.5) / 100 = 0.075
        result = convert_reynolds_to_viscosity(100.0, 1.0, 0.0, 10.0, 0.0, 5.0)
        assert result == pytest.approx(0.075)

    def test_symmetric_domain(self):
        # char_length = ((5-0) + (5-0)) / 2 = 5.0
        result = convert_reynolds_to_viscosity(50.0, 2.0, 0.0, 5.0, 0.0, 5.0)
        assert result == pytest.approx(0.2)

    def test_high_reynolds(self):
        result = convert_reynolds_to_viscosity(1e6, 1.0, 0.0, 10.0, 0.0, 10.0)
        assert result == pytest.approx(1e-5)

    def test_nonzero_origin(self):
        # char_length = ((12-2) + (7-2)) / 2 = 7.5
        result = convert_reynolds_to_viscosity(100.0, 1.0, 2.0, 12.0, 2.0, 7.0)
        assert result == pytest.approx(0.075)


# helpers.py: get_initial_velocity_at_point
class TestGetInitialVelocityAtPoint:
    """Tests for the initial velocity assignment at boundary points."""

    def setup_method(self):
        self.func = get_initial_velocity_at_point
        self.kwargs = dict(x_min=0.0, x_max=10.0, y_min=0.0, y_max=5.0, velocity=2.0)

    def test_west_boundary(self):
        assert self.func(0.0, 2.5, **self.kwargs) == [2.0, 0.0]

    def test_east_boundary(self):
        assert self.func(10.0, 2.5, **self.kwargs) == [-2.0, 0.0]

    def test_south_boundary(self):
        assert self.func(5.0, 0.0, **self.kwargs) == [0.0, 2.0]

    def test_north_boundary(self):
        assert self.func(5.0, 5.0, **self.kwargs) == [0.0, -2.0]

    def test_interior_point(self):
        assert self.func(5.0, 2.5, **self.kwargs) == [0.0, 0.0]

    def test_corner_west_takes_priority(self):
        # At corner (0, 0), x==x_min is checked first
        result = self.func(0.0, 0.0, **self.kwargs)
        assert result == [2.0, 0.0]

    def test_near_boundary_within_eps(self):
        result = self.func(0.0005, 2.5, **self.kwargs)
        assert result == [2.0, 0.0]

    def test_near_boundary_outside_eps(self):
        result = self.func(0.01, 2.5, **self.kwargs)
        assert result == [0.0, 0.0]


# helpers.py: extract_attributes
class TestExtractAttributes:
    """Tests for the JSON scenario parser."""

    def test_basic_extraction(self, tmp_path):
        scenario = _minimal_scenario()
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        assert result["x_min"] == 0.0
        assert result["x_max"] == 10.0
        assert result["y_min"] == 0.0
        assert result["y_max"] == 5.0
        assert result["inlet_velocity"] == 1.0
        assert result["viscosity"] == pytest.approx(0.075)
        assert len(result["inlets"]) == 1
        assert result["inlets"][0]["side"] == "west"
        assert result["inlets"][0]["coords"] == [1.0, 3.0]
        assert len(result["outlets"]) == 1

    def test_bounding_box_width_shrinks_domain(self, tmp_path):
        scenario = _minimal_scenario(bounding_box_width=1.0)
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        # bounds: x=0, width=10: topo_xmin = 0+1 = 1, topo_xmax = 10-1 = 9
        assert result["x_min"] == 1.0
        assert result["x_max"] == 9.0
        assert result["y_min"] == 1.0
        assert result["y_max"] == 4.0

    def test_airflow_bounds_clip_domain(self, tmp_path):
        scenario = _minimal_scenario(
            airflow_bounds={"xmin": 2.0, "xmax": 8.0, "ymin": 1.0, "ymax": 4.0}
        )
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        assert result["x_min"] == 2.0
        assert result["x_max"] == 8.0
        assert result["y_min"] == 1.0
        assert result["y_max"] == 4.0

    def test_multiple_inlets_and_outlets(self, tmp_path):
        scenario = _minimal_scenario(
            inlets=[
                {"side": "west", "start": 1.0, "width": 1.0},
                {"side": "south", "start": 3.0, "width": 2.0},
            ],
            outlets=[
                {"side": "east", "start": 1.0, "width": 1.0},
                {"side": "north", "start": 4.0, "width": 1.5},
            ],
        )
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        assert len(result["inlets"]) == 2
        assert result["inlets"][1]["side"] == "south"
        assert result["inlets"][1]["coords"] == [3.0, 5.0]
        assert len(result["outlets"]) == 2
        assert result["outlets"][1]["coords"] == [4.0, 5.5]

    def test_no_inlets_raises(self, tmp_path):
        scenario = _minimal_scenario(inlets=[])
        path = _write_scenario(tmp_path, scenario)
        with pytest.raises(ValueError, match="No inlets"):
            extract_attributes(path)

    def test_no_outlets_raises(self, tmp_path):
        scenario = _minimal_scenario(outlets=[])
        path = _write_scenario(tmp_path, scenario)
        with pytest.raises(ValueError, match="No outlets"):
            extract_attributes(path)

    def test_rectangle_obstacle(self, tmp_path):
        obs = [
            {
                "id": 42,
                "shape": {
                    "type": "RECTANGLE",
                    "x": 3.0,
                    "y": 1.0,
                    "width": 2.0,
                    "height": 1.0,
                },
            }
        ]
        scenario = _minimal_scenario(obstacles=obs, blocking_obstacles=[42])
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        assert len(result["obstacles"]) == 1
        corners = result["obstacles"][0]
        xs = [c[0] for c in corners]
        ys = [c[1] for c in corners]
        assert min(xs) == 3.0 and max(xs) == 5.0
        assert min(ys) == 1.0 and max(ys) == 2.0

    def test_polygon_obstacle(self, tmp_path):
        obs = [
            {
                "id": 7,
                "shape": {
                    "type": "POLYGON",
                    "points": [
                        {"x": 2.0, "y": 1.0},
                        {"x": 3.0, "y": 2.0},
                        {"x": 2.5, "y": 0.5},
                    ],
                },
            }
        ]
        scenario = _minimal_scenario(obstacles=obs, blocking_obstacles=[7])
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        assert len(result["obstacles"]) == 1
        assert len(result["obstacles"][0]) == 3

    def test_nonblocking_obstacle_excluded(self, tmp_path):
        obs = [
            {
                "id": 99,
                "shape": {"type": "RECTANGLE", "x": 1, "y": 1, "width": 1, "height": 1},
            }
        ]
        scenario = _minimal_scenario(obstacles=obs, blocking_obstacles=[])
        path = _write_scenario(tmp_path, scenario)
        result = extract_attributes(path)

        assert len(result["obstacles"]) == 0


# helpers.py: get_parameter_string
class TestGetParameterString:
    """Tests for the cache key / parameter string builder."""

    def test_deterministic_output(self):
        gd = _make_geom_data()
        s1 = get_parameter_string(gd)
        s2 = get_parameter_string(gd)
        assert s1 == s2

    def test_contains_key_fields(self):
        gd = _make_geom_data()
        s = get_parameter_string(gd)

        assert "0.5" in s          # rect_grid_cell_size
        assert "1.0" in s          # max_triangle_edge_len or inlet_velocity
        assert "west" in s
        assert "east" in s
        assert "xmin" in s
        assert "xmax" in s

    def test_different_params_produce_different_strings(self):
        gd1 = _make_geom_data(inlet_velocity=1.0)
        gd2 = _make_geom_data(inlet_velocity=2.0)
        assert get_parameter_string(gd1) != get_parameter_string(gd2)


# helpers.py — postprocess_solution
class TestPostprocessSolution:
    """Tests for the FEM → rectangular grid interpolation."""

    def _make_simple_mesh_and_velocity(self):
        """
        Build a tiny 2-triangle mesh on [0,2]x[0,1] with a known
        uniform velocity field u=(1,0) so interpolation is exact.
        """
        coords = np.array([
            [0.0, 0.0],
            [2.0, 0.0],
            [2.0, 1.0],
            [0.0, 1.0],
        ])
        conn = np.array([[0, 1, 2], [0, 2, 3]], dtype=np.int32)
        mat_ids = np.zeros(2, dtype=np.int32)
        mesh = Mesh.from_data(
            "test", coords, None, [conn], [mat_ids], ["2_3"]
        )
        # uniform rightward velocity
        u_vals = np.array([[1.0, 0.0]] * 4)
        return mesh, u_vals

    def test_output_shapes(self):
        mesh, u_vals = self._make_simple_mesh_and_velocity()
        gd = _make_geom_data(
            x_min=0.0, x_max=2.0, y_min=0.0, y_max=1.0,
            rect_grid_cell_size=0.5,
        )
        X, Y, Vx, Vy, mag = postprocess_solution(u_vals, mesh, gd)

        # nx = round(2/0.5)+1 = 5, ny = round(1/0.5)+1 = 3
        assert X.shape == (3, 5)
        assert Y.shape == (3, 5)
        assert Vx.shape == (3, 5)
        assert Vy.shape == (3, 5)
        assert mag.shape == (3, 5)

    def test_uniform_field_interpolation(self):
        mesh, u_vals = self._make_simple_mesh_and_velocity()
        gd = _make_geom_data(
            x_min=0.0, x_max=2.0, y_min=0.0, y_max=1.0,
            rect_grid_cell_size=0.5,
        )
        X, Y, Vx, Vy, mag = postprocess_solution(u_vals, mesh, gd)

        np.testing.assert_allclose(Vx, 1.0, atol=1e-10)
        np.testing.assert_allclose(Vy, 0.0, atol=1e-10)
        np.testing.assert_allclose(mag, 1.0, atol=1e-10)

    def test_grid_coordinates(self):
        mesh, u_vals = self._make_simple_mesh_and_velocity()
        gd = _make_geom_data(
            x_min=0.0, x_max=2.0, y_min=0.0, y_max=1.0,
            rect_grid_cell_size=1.0,
        )
        X, Y, Vx, Vy, mag = postprocess_solution(u_vals, mesh, gd)

        np.testing.assert_allclose(X[0, :], [0.0, 1.0, 2.0])
        np.testing.assert_allclose(Y[:, 0], [0.0, 1.0])

    def test_velocity_magnitude_consistency(self):
        """Magnitude must equal sqrt(Vx² + Vy²)."""
        mesh, u_vals = self._make_simple_mesh_and_velocity()
        # set a diagonal velocity field
        u_vals = np.array([[1.0, 1.0]] * 4)
        gd = _make_geom_data(
            x_min=0.0, x_max=2.0, y_min=0.0, y_max=1.0,
            rect_grid_cell_size=0.5,
        )
        X, Y, Vx, Vy, mag = postprocess_solution(u_vals, mesh, gd)

        expected_mag = np.hypot(Vx, Vy)
        np.testing.assert_allclose(mag, expected_mag, atol=1e-10)


# helpers.py: get_cache_dir
class TestGetCacheDir:
    def test_creates_cache_dir(self, tmp_path):
        scenario_path = tmp_path / "scenarios" / "my_scenario.json"
        scenario_path.parent.mkdir(parents=True)
        scenario_path.touch()

        cache_dir, name = get_cache_dir(str(scenario_path))

        assert cache_dir.exists()
        assert cache_dir.name == "cache"
        assert name == "my_scenario"

    def test_idempotent(self, tmp_path):
        scenario_path = tmp_path / "my_scenario.json"
        scenario_path.touch()

        d1, _ = get_cache_dir(str(scenario_path))
        d2, _ = get_cache_dir(str(scenario_path))
        assert d1 == d2


# build_mesh.py
class TestBuildMesh:
    """
    Tests for mesh generation.  These require gmsh and sfepy to be installed.
    """

    def test_basic_mesh_creation(self):
        gd = _make_geom_data(max_triangle_edge_len=1.5)
        mesh, bdry = build_mesh(gd)

        assert mesh.n_nod > 4      # more than corner points
        assert mesh.n_el > 0
        assert mesh.coors.shape[1] == 2

    def test_all_nodes_inside_domain(self):
        gd = _make_geom_data(max_triangle_edge_len=2.0)
        mesh, _ = build_mesh(gd)
        eps = 1e-8

        assert np.all(mesh.coors[:, 0] >= gd["x_min"] - eps)
        assert np.all(mesh.coors[:, 0] <= gd["x_max"] + eps)
        assert np.all(mesh.coors[:, 1] >= gd["y_min"] - eps)
        assert np.all(mesh.coors[:, 1] <= gd["y_max"] + eps)

    def test_inlet_and_outlet_nodes_exist(self):
        gd = _make_geom_data(max_triangle_edge_len=1.5)
        mesh, bdry = build_mesh(gd)

        assert len(bdry["inlet"]) > 0
        assert len(bdry["outlet"]) > 0

    def test_inlet_nodes_on_correct_boundary(self):
        gd = _make_geom_data(
            max_triangle_edge_len=1.5,
            inlets=[{"id": 0, "side": "west", "coords": [1.0, 3.0]}],
        )
        mesh, bdry = build_mesh(gd)
        inlet_coords = mesh.coors[bdry["inlet"]]
        eps = 1e-5

        # all inlet nodes must sit on the west wall
        assert np.all(np.abs(inlet_coords[:, 0] - gd["x_min"]) < eps)
        # and within the specified y-range
        assert np.all(inlet_coords[:, 1] >= 1.0 - eps)
        assert np.all(inlet_coords[:, 1] <= 3.0 + eps)

    def test_outlet_nodes_on_correct_boundary(self):
        gd = _make_geom_data(
            max_triangle_edge_len=1.5,
            outlets=[{"id": 0, "side": "east", "coords": [1.0, 3.0]}],
        )
        mesh, bdry = build_mesh(gd)
        outlet_coords = mesh.coors[bdry["outlet"]]
        eps = 1e-5

        assert np.all(np.abs(outlet_coords[:, 0] - gd["x_max"]) < eps)
        assert np.all(outlet_coords[:, 1] >= 1.0 - eps)
        assert np.all(outlet_coords[:, 1] <= 3.0 + eps)

    def test_inlet_outlet_disjoint(self):
        gd = _make_geom_data(max_triangle_edge_len=1.5)
        mesh, bdry = build_mesh(gd)
        overlap = np.intersect1d(bdry["inlet"], bdry["outlet"])
        assert len(overlap) == 0

    def test_south_inlet(self):
        gd = _make_geom_data(
            max_triangle_edge_len=2.0,
            inlets=[{"id": 0, "side": "south", "coords": [2.0, 5.0]}],
            outlets=[{"id": 0, "side": "north", "coords": [2.0, 5.0]}],
        )
        mesh, bdry = build_mesh(gd)
        inlet_coords = mesh.coors[bdry["inlet"]]
        eps = 1e-5

        assert len(bdry["inlet"]) > 0
        assert np.all(np.abs(inlet_coords[:, 1] - gd["y_min"]) < eps)

    def test_mesh_with_rectangular_obstacle(self):
        obs = [[(3.0, 1.5), (3.0, 3.5), (5.0, 3.5), (5.0, 1.5)]]
        gd = _make_geom_data(max_triangle_edge_len=1.5, obstacles=obs)
        mesh, bdry = build_mesh(gd)

        # The obstacle should create a hole, no mesh nodes inside it
        inside_obs = (
            (mesh.coors[:, 0] > 3.0 + 0.05)
            & (mesh.coors[:, 0] < 5.0 - 0.05)
            & (mesh.coors[:, 1] > 1.5 + 0.05)
            & (mesh.coors[:, 1] < 3.5 - 0.05)
        )
        assert np.sum(inside_obs) == 0, "Nodes found inside the obstacle region"

    def test_finer_mesh_gives_more_elements(self):
        gd_coarse = _make_geom_data(max_triangle_edge_len=2.5)
        gd_fine = _make_geom_data(max_triangle_edge_len=0.8)

        mesh_coarse, _ = build_mesh(gd_coarse)
        mesh_fine, _ = build_mesh(gd_fine)

        assert mesh_fine.n_el > mesh_coarse.n_el

    def test_connectivity_valid(self):
        gd = _make_geom_data(max_triangle_edge_len=2.0)
        mesh, _ = build_mesh(gd)

        conn = mesh.get_conn("2_3")
        assert conn.shape[1] == 3
        assert np.all(conn >= 0)
        assert np.all(conn < mesh.n_nod)


# integration level
class TestRoundTrip:
    """End-to-end test: parse scenario, build mesh, verify boundaries."""

    def test_scenario_to_mesh(self, tmp_path):
        scenario = _minimal_scenario(
            inlets=[{"side": "west", "start": 1.0, "width": 2.0}],
            outlets=[{"side": "east", "start": 1.0, "width": 2.0}],
            max_triangle_edge_len=2.0,
        )
        path = _write_scenario(tmp_path, scenario)
        gd = extract_attributes(path)
        mesh, bdry = build_mesh(gd)

        assert mesh.n_nod > 0
        assert len(bdry["inlet"]) > 0
        assert len(bdry["outlet"]) > 0

        # inlet on west, outlet on east
        eps = 1e-5
        assert np.all(np.abs(mesh.coors[bdry["inlet"], 0] - gd["x_min"]) < eps)
        assert np.all(np.abs(mesh.coors[bdry["outlet"], 0] - gd["x_max"]) < eps)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])