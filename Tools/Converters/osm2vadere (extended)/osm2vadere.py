"""
Convert an OpenStreetMap XML file exported from https://www.openstreetmap.org/
to a Vadere topology (in Cartesian coordinates).

converter defined in osm_converter.py
helper and osm structures in osm_helper.py

"""
import argparse
import logging
import math
import os
from random import sample
from typing import List
import matplotlib.pyplot as plt
from shapely.geometry import asPolygon, asPoint, asLineString
import igraph as ig
import json
import numpy as np
from multiprocessing import set_start_method, get_context, cpu_count


from osm_converter import OsmConverter
from osm_helper import OsmArg, OsmConfig, OsmData, PolyObjectWidthId, Node, Way, Nd


class HullAction(argparse.Action):
    def __call__(self, parser, namespace, values, option_string=None):
        if not hasattr(namespace, self.dest):
            raise argparse.ArgumentError(f"namespace should contain {self.dest}")

        attr = getattr(namespace, self.dest)
        attr.append(values)


def random_source_target_match(
        sources: List[PolyObjectWidthId], targets: List[PolyObjectWidthId]
):
    target_ids = [t.template_data.get("id") for t in targets]

    for source in sources:
        if "targetIds" not in source.template_data:
            t_ids = sample(target_ids, 2)
            if source.id in t_ids:
                t_ids.remove(source.id)
            source.template_data.setdefault("targetIds", f"[ {t_ids[0]} ]")


def str2bool(v):
    # see https://stackoverflow.com/a/43357954
    if isinstance(v, bool):
        return v
    if v.lower() in ("yes", "true", "t", "y", "1"):
        return True
    elif v.lower() in ("no", "false", "f", "n", "0"):
        return False
    else:
        raise argparse.ArgumentTypeError("Boolean value expected.")


def parse_command_line_arguments(manual_args=None):
    main = argparse.ArgumentParser(
        prog="OpenStreetMap (OSM) Util for (Vadere, OMNeT++)",
        description="Collection of small commands to manipulate an OSM xml file to "
                    "preparer it for conversion to Vadere or OMNeT++ structures",
    )

    parent_parser = argparse.ArgumentParser(add_help=False)
    parent_parser.add_argument(
        "-i", "--input", dest="input", nargs="?", required=True, help="OSM input file"
    )

    parent_parser.add_argument(
        "-o",
        "--output",
        dest="output",
        nargs="?",
        required=False,
        help="OSM output. If not set the name derived from input file",
    )

    subparsers = main.add_subparsers(title="Commands")

    #
    # COMMAND 1
    #
    convert_parser = subparsers.add_parser(
        "convert",
        parents=[parent_parser],
        description="Convert and OpenStreetMap file to a Vadere Topography or update an existing scenario file.",
    )

    convert_parser.add_argument(
        "--use-osm-id",
        dest="use_osm_id",
        type=str2bool,
        const=True,
        nargs="?",
        default=True,
        help="Set to use osm ids for obstacles",
    )

    convert_parser.add_argument(
        "--use-aoi",
        dest="use_aoi",
        type=str2bool,
        const=True,
        nargs="?",
        default=False,
        help="Set to reduce export to elements within an area of interest. "
             "(way taged with vadere:area-of-intrest) ",
    )

    convert_parser.add_argument(
        "--template",
        dest="template",
        default=None,
        required=False,
        help="path to scenario file to use as template."
    )

    convert_parser.set_defaults(main_func=main_convert)

    #
    # COMMAND 2
    #
    hull_parser = subparsers.add_parser(
        "convex-hull",
        parents=[parent_parser],
        description="Create a convex hull around each list of given way ids.",
    )
    hull_parser.set_defaults(main_func=main_convex_hull)

    hull_parser.add_argument(
        "-w",
        "--ways",
        dest="way_list",
        default=[],
        action=HullAction,
        nargs="+",
        help="list of way ids which span the convex hull",
    )

    #
    # COMMAND 3
    #
    wall_parser = subparsers.add_parser(
        "wall",
        parents=[parent_parser],
        description="Create obstacles around a line segment list.",
    )
    wall_parser.set_defaults(main_func=main_walls)

    wall_parser.add_argument(
        "-w",
        "--ways",
        dest="way_list",
        default=[],
        nargs="+",
        help="list of way ids which define a line segment list",
    )

    wall_parser.add_argument(
        "-d",
        "--dist",
        dest="dist",
        default=0.25,
        nargs="?",
        help="The perpendicular distance between the line defined by the way element and the "
             "parallel line used to build the polygon. The width of the polygon will thus be "
             "2*dist",
    )

    #
    # COMMAND 4
    #
    lint_parser = subparsers.add_parser(
        "lint",
        parents=[parent_parser],
        description="Check for unique ids, add id-tag if missing, check for non  "
                    "normalized obstacles",
    )
    lint_parser.set_defaults(main_func=main_lint)

    lint_parser.add_argument(
        "-a", "--all", dest="all", action="store_true", help="execute all test"
    )

    lint_parser.add_argument(
        "--dry-run",
        dest="dry_run",
        action="store_true",
        help="only print what would be done but do not change anything",
    )

    lint_parser.add_argument(
        "--add-ids",
        dest="add_ids",
        action="store_true",
        help="Ensure all manually added elements contain an id tag",
    )

    lint_parser.add_argument(
        "--unique-ids",
        dest="unique_ids",
        action="store_true",
        help="Check for unique ids",
    )

    lint_parser.add_argument(
        "--check-obstacles",
        dest="check_obstacles",
        action="store_true",
        help="check if file contains any touching obstacles",
    )

    #
    # COMMAND 5
    #
    config_parser = subparsers.add_parser("use-config")

    config_parser.add_argument(
        "-c",
        "--config",
        dest="config",
        nargs="?",
        help="Execute commands in configuration file.",
    )

    config_parser.add_argument(
        "-n",
        "--new-config-file",
        dest="new_config",
        default="osm.config",
        nargs="?",
        help="create a default config file including all possible commands. If -c is used this"
             "option is ignored",
    )

    config_parser.set_defaults(main_func=main_apply_config)

    #
    # Parse and return Namespace
    #
    if manual_args is not None:
        cmd_args = main.parse_args(manual_args)
    else:
        cmd_args = main.parse_args()

    return cmd_args


#
# Entry point functions.
#


def main_convert(cmd_args):
    """
    Entry point
    osm2vadere.pu mf.osm convert -h // for sub command specific help
    osm2vadere.py mf.osm convert --output map.json
    """
    if cmd_args.output is None:
        dirname, basename = os.path.split(cmd_args.input)
        cmd_args.output = os.path.join(dirname, f"{basename.split('.')[0]}.txt")

    print(cmd_args)
    converter = OsmConverter.from_args(cmd_args)

    if converter.aoi:
        base = converter.get_base_point_from_aoi()
        (
            obstacles_as_utm,
            base_point_utm,
            zone_string,
        ) = converter.convert_to_utm_poly_object(
            data=converter.obstacles, base_point=base
        )
    else:
        (
            obstacles_as_utm,
            base_point_utm,
            zone_string,
        ) = converter.convert_to_utm_poly_object(data=converter.obstacles)

    sources_as_utm, _, _ = converter.convert_to_utm_poly_object(
        data=converter.sources,
        base_point=base_point_utm,
        tag_name_space="rover:source:",
    )
    targets_as_utm, _, _ = converter.convert_to_utm_poly_object(
        data=converter.targets,
        base_point=base_point_utm,
        tag_name_space="rover:target:",
    )
    measurement_as_utm, _, _ = converter.convert_to_utm_poly_object(
        data=converter.measurement,
        base_point=base_point_utm,
        tag_name_space="rover:measurementArea:",
    )

    # random preset for targetIds list for sources
    # random_source_target_match(sources_as_utm, targets_as_utm)

    # make sure everything lies within the topography
    if converter.aoi:
        width_topography, height_topography = converter.find_width_height_from_aoi(
            base_point_utm
        )
    else:
        width_topography, height_topography = OsmConverter.find_width_and_height(
            obstacles_as_utm
        )

    list_of_vadere_obstacles_as_strings = OsmConverter.to_vadere_obstacles(
        obstacles_as_utm
    )
    list_of_vadere_sources_as_strings = OsmConverter.to_vadere_sources(sources_as_utm)
    list_of_vadere_target_as_strings = OsmConverter.to_vadere_targets(targets_as_utm)
    list_of_vadere_measurement_as_strings = OsmConverter.to_vadere_measurement_area(
        measurement_as_utm
    )

    obstacles_joined = ",\n".join(list_of_vadere_obstacles_as_strings)
    sources_joined = ",\n".join(list_of_vadere_sources_as_strings)
    targets_joined = ",\n".join(list_of_vadere_target_as_strings)
    measurement_joined = ",\n".join(list_of_vadere_measurement_as_strings)

    vadere_topography_output = OsmConverter.to_vadere_topography(
        width_topography,
        height_topography,
        base_point_utm,
        zone_string,
        obstacles=obstacles_joined,
        sources=sources_joined,
        targets=targets_joined,
        measurement_areas=measurement_joined,
    )


    if cmd_args.template is not None:
        # read template scenario and repalce topogrpahy
        if not os.path.exists(cmd_args.template):
            raise ValueError(f"template does not exist {cmd_args.template}")
        with open(cmd_args.template, 'r', encoding='utf-8') as fd:
            tmpl = json.load(fd)

        name = os.path.basename(os.path.split(cmd_args.output)[1])
        topography = json.loads(vadere_topography_output)
        tmpl["scenario"]["topography"] = topography
        tmpl["name"] = name

        print(f"write output {cmd_args.output}")
        with open(cmd_args.output, "w", encoding="utf-8") as fd:
            json.dump(tmpl, fd, indent=2)

    else:
      OsmConverter.print_output(cmd_args.output, vadere_topography_output)



def main_walls(args):
    if args.output is None:
        args.output = args.input

    osm = OsmData(args.input, args.output)
    for way_id in args.way_list:
        utm_points = osm.nodes_to_utm(osm.way_node_refs(way_id))
        osm.way_create_from_polyline(utm_points, dist=args.dist)

    osm.save()


def main_lint(args):
    if args.output is None:
        args.output = args.input
    osm = OsmData(args.input, args.output, strict=False)
    if args.all:
        dup_way, dup_node = osm.lint_unique_ids()
        if len(dup_way) > 0 or len(dup_node) > 0:
            print("error")
            exit(-1)
        osm.lint_check_obstacles()
        osm.lint_cleanup_unused_nodes(args.dry_run)

    osm.save()


def main_apply_config(args):
    cfg = OsmConfig(args.config)

    default_args = cfg.default_args()
    for cmd in cfg["command-order"]:
        if cmd not in cfg:
            logging.warning(
                f"no option specified for command '{cmd}' will use default if possible"
            )

        cmd_args = {}
        cmd_args.update(default_args)
        cmd_args.update(cfg.cmd_args(cmd))
        cmd_args = OsmArg(cmd_args)
        main_func = cmd_entry[cmd]
        print(
            "################################################################################"
        )
        print(f"### executing command {cmd}")
        print(
            "################################################################################"
        )
        main_func(cmd_args)


def main_convex_hull(args):
    if args.output is None:
        args.output = args.input

    osm = OsmData(args.input, args.output)
    for way_ids in args.way_list:
        osm.create_convex_hull(way_ids)

    osm.save()


cmd_entry = {
    "lint": main_lint,
    "wall": main_walls,
    "convex-hull": main_convex_hull,
    "convert": main_convert,
}



class Block:

    def __init__(self, ways):
        self.ways = ways
        self.block_to_node = {}
        self.block_to_way = {}
        self.next_block_id = 0

    def merge(self, block_ids):
        nodes = set()
        ways = set()
        for b in block_ids:
            nodes.update(self.block_to_node.pop(b))
            ways.update(self.block_to_way.pop(b))

        b_id = self.next_block()
        self.block_to_node[b_id] = nodes
        self.block_to_way[b_id] = ways
        return b_id

    def next_block(self):
        r = self.next_block_id
        self.next_block_id += 1
        self.block_to_way.setdefault(r, set())
        self.block_to_node.setdefault(r, set())
        return r

    def create(self):
        for w in self.ways:
            b_id = []
            for block, nodes in self.block_to_node.items():
                if not nodes.isdisjoint(set(w.nds)):
                    b_id.append(block)

            if len(b_id) == 0:
                # new block
                b_id = self.next_block()
            elif len(b_id) == 1:
                b_id = b_id[0]
            else:
                # merge existing blocks because current way is part of two blocks
                b_id = self.merge(b_id)

            self.block_to_node[b_id].update(set(w.nds))
            self.block_to_way[b_id].add(w)

        return self.block_to_way, self.block_to_node


class ConvaceHull:
    lon = 0
    lat = 1
    earth_radius = 6371000  # meter

    def __init__(self, points, prime_ix=0):
        """
        points np.array of the form (N,2) with (longitute, latitude) values
        """

        self.data_set = points
        # no duplicates
        self.data_set = np.unique(self.data_set, axis=0)

        # select all at the beginning
        self.indices = np.ones(self.data_set.shape[0], dtype=bool)

        self.prime_k = np.array([3, 5, 7, 11, 13, 17, 21, 23, 29, 31, 37, 41, 43,
                                 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97])
        self.prime_ix = prime_ix

    def min_lat_index(self, points):
        indices = np.argsort(points[:, self.lat])
        return indices[0]

    def next_k(self):
        if self.prime_ix < len(self.prime_k):
            return self.prime_k[self.prime_ix]
        else:
            return -1

    def haversine_dist(self, loc_ini, loc_end):
        """
        loc_ini: point,
        loc_end: list of points?
        https://en.wikipedia.org/wiki/Haversine_formula
        """
        lon1, lat1, lon2, lat2 = map(np.radians,
                                     [
                                         loc_ini[self.lon], loc_ini[self.lat],
                                         loc_end[:, self.lon], loc_end[:, self.lat]
                                     ])
        delta_lon = lon2 - lon1
        delta_lat = lat2 - lat1
        a = np.square(np.sin(delta_lat / 2.0)) + \
            np.cos(lat1) * np.cos(lat2) * np.square(np.sin(delta_lon / 2.0))

        c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1.0 - a))
        return  c * self.earth_radius

    def k_nearest(self, ix, k):
        """
        ix: index into data_set
        k:  number of neighbors
        """
        # index based on data_set
        ixs = self.indices


        # remove visited/removed points
        base_indices = np.arange(len(ixs))[ixs]  # ixs is a boolean mask!
        distances = self.haversine_dist(self.data_set[ix, :], self.data_set[ixs, :])

        # arg sorted (smallest dist index at front of list)
        sorted_indices = np.argsort(distances)
        kk = min(k, len(sorted_indices))

        k_nearest = sorted_indices[range(kk)]

        # get index of point based on base set
        return base_indices[k_nearest]

    def headings(self, ix, ixs, ref_heading=0.0):
        if ref_heading < 0 or ref_heading >= 360.0:
            raise ValueError("Heading must be in the range of [0, 360)")

        # reference point
        r_ix = np.radians(self.data_set[ix, :])
        # point to which the heading is calculated
        r_ixs = np.radians(self.data_set[ixs, :])

        delta_lons = r_ixs[:, self.lon] - r_ix[self.lon]
        y = np.multiply(np.sin(delta_lons), np.cos(r_ixs[:, self.lat]))
        x = math.cos(r_ix[self.lat]) * np.sin(r_ixs[:, self.lat]) - \
            math.sin(r_ix[self.lat]) * np.multiply(np.cos(r_ixs[:, self.lat]), np.cos(delta_lons))
        bearings = (np.degrees(np.arctan2(y,x)) + 360.0) % 360.0 - ref_heading
        bearings[bearings < 0.0] += 360.0
        return bearings

    def calc_recursive(self):
        recurse = ConvaceHull(self.data_set, self.prime_ix + 1)
        next_k = recurse.next_k()
        if next_k == -1:
            return None
        return recurse.calc(next_k)

    def calc(self, k=3):

        if self.data_set.shape[0] < 3:
            return None

        if self.data_set.shape[0] == 3:
            return self.data_set

        kk = min(k, self.data_set.shape[0])

        first_point = self.min_lat_index(self.data_set)
        current_point = first_point

        hull = np.reshape(np.array(self.data_set[first_point, :]), (1,2))
        test_hull = hull

        # remove the first point
        self.indices[first_point] = False

        prev_angle = 270 # Initial reference id due west. North is zero, measured clockwise.
        step = 2
        stop = 2 + kk

        while((current_point != first_point) or (step == 2)) and len(self.indices[self.indices]) > 0:
            if step == stop:
                self.indices[first_point] = True

            knn = self.k_nearest(current_point, kk)

            # Calc headings between first_point and the knn points
            # Returns angles in the same indexing sequence as in knn
            angles = self.headings(current_point, knn, prev_angle)

            # Calculate the candidate indexes (largest angles first)
            candidates = np.argsort(-angles)

            i = 0
            invalid_hull = True
            while invalid_hull and i < len(candidates):
                candidate = candidates[i]

                # Create a test hull to check if there are any self-intersections
                next_point = np.reshape(self.data_set[knn[candidate]], (1,2))
                test_hull = np.append(hull, next_point, axis=0)

                line = asLineString(test_hull)
                invalid_hull = not line.is_simple
                i += 1

            if invalid_hull:
                return self.calc_recursive()

            prev_angle = self.headings(knn[candidate], np.array([current_point]))
            current_point = knn[candidate]
            hull = test_hull

            self.indices[current_point] = False
            step += 1

        poly = asPolygon(hull)

        count = 0
        total = self.data_set.shape[0]
        for ix in range(total):
            pt = asPoint(self.data_set[ix, :])
            if poly.intersects(pt) or pt.within(poly):
                count += 1
            else:
                d = poly.distance(pt)
                if d < 1e-5:
                    count += 1

        if count == total:
            return hull
        else:
            return self.calc_recursive()

class ConvexBlock:

    def __init__(self, osm: OsmData, ways: List[Way]):
        self.osm = osm
        self.ways = ways
        # build convex hull
        n, hull = osm.convex_hull([w.id for w in ways])
        self.nodes = n
        self.convex_hull_idx = hull

        _hull = np.append(n[hull], n[hull[0]])
        hull_edges = []
        for i in range(0, len(_hull)-1):
            hull_edges.append(list(_hull[i:i+2]))

        self.hull_edges = hull_edges

        self.graph = ig.Graph()
        self.nd_to_edge = {}
        self.nd_to_vertex = {}

        # create directed graph
        for w in self.ways:
            for e in w.edges():
                nd_e = self.nd_edge(e)
                if nd_e[0] not in self.nd_to_vertex:
                    v = self.graph.add_vertex(nd_e[0])
                    self.nd_to_vertex[nd_e[0]] = v
                if nd_e[1] not in self.nd_to_vertex:
                    v = self.graph.add_vertex(nd_e[1])
                    self.nd_to_vertex[nd_e[1]] = v

                e = self.graph.add_edge(*self.g_edge(nd_e))
                self.nd_to_edge[nd_e] = e

    def g_edge(self, nd_edge):
        start = self.nd_to_vertex[nd_edge[0]]
        end = self.nd_to_vertex[nd_edge[1]]
        return start, end

    def nd_edge(self, nd_array):
        e = (nd_array[0].id, nd_array[1].id)
        return e

    def valid_edge(self, edge):
        """
        check if given edge is present in graph
        """
        _edge_key = (edge[0], edge[1])
        ret = self.nd_to_edge.get(_edge_key, None)
        if ret is not None:
            return True
        _edge_key = (edge[1], edge[0])
        ret = self.nd_to_edge.get(_edge_key, None)
        if ret is not None:
            return True
        return False

    def plot(self):
        layout = self.graph.layout("kk")
        ig.plot(self.graph, layout=layout)

    def plot_block(self):
        points = [self.osm.utm_lookup(n) for n in self.nodes[self.convex_hull_idx]]
        points = np.array(points)
        points = np.append(points, points[0, :]).reshape((-1, 2))
        plt.plot(points[:, 0], points[:, 1])
        plt.show()

    def calc(self):
        # loop over convex hull edges
        hull_nodes = self.nodes[self.convex_hull_idx]

        block_hull = []
        for edge in self.hull_edges:
            is_valid = self.valid_edge(edge)
            if is_valid:
                block_hull.append(edge)
                print(f"add edge {edge}")
            else:
                print(f"edge invalid find valid path from {edge[0]} --- {edge[1]} ")
                vert_1 = self.nd_to_vertex[edge[0]]
                vert_2 = self.nd_to_vertex[edge[1]]
                path  = self.graph.get_all_simple_paths(v=vert_1, to=vert_2)
                print(f"found {len(path)} paths")
                print("hi")

if __name__ == "__main__":
    set_start_method("spawn")
    # path = f"{os.environ['HOME']}/repos/crownet/vadere/Scenarios/Demos/roVer/scenarios/mf_base.config"
    # args = parse_command_line_arguments(["use-config", "-c", path])
    args = parse_command_line_arguments()
    # p = "/home/vm-sts/repos/crownet/vadere/Scenarios/Demos/roVer/scenarios/layer1.osm"
    # p_out = "/home/vm-sts/repos/crownet/vadere/Scenarios/Demos/roVer/scenarios/layer1_out.osm"
    # osm = OsmData(p, p_out)
    # n = [Node.from_xml(e) for e in osm.nodes]
    # ways = [Way.from_xml(e) for e in osm.ways]
    # b = Block(ways)
    # block_way, block_nodes = b.create()
    # block = block_way[10]
    # g = ConvexBlock(osm, block_way[10])
    # g.calc()
    # g.plot_block()
    # print("hi")

    #
    # p = np.array([osm.lonlat_lookup(n.id) for n in ret[1][10]])
    # h = ConvaceHull(p)
    # hull_array = h.calc(k=21)
    #
    #
    # ax = plt.scatter(p[:,0], p[:,1])
    # x, y = asPolygon(hull_array).exterior.xy
    # plt.plot(x,y)
    # # ax.plot(x,y)
    # plt.show()
    #
    # print("hi")
    args.main_func(args)
