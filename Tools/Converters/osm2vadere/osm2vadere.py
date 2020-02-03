"""
Convert an OpenStreetMap XML file exported from https://www.openstreetmap.org/
to a Vadere topology (in Cartesian coordinates).

converter defined in osm_converter.py
helper and osm structures in osm_helper.py

"""
import argparse
import logging
import os
from random import sample
from typing import List

from osm_converter import OsmConverter
from osm_helper import OsmArg, OsmConfig, OsmData, PolyObjectWidthId


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
    OsmConverter.print_output(cmd_args.output, vadere_topography_output)
    # converter.osm.save()


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

    default_args = {"input": cfg["Config"]["input"], "output": cfg["Config"]["output"]}
    for cmd in cfg["Config"]["command-order"]:
        if f"{cmd}:options" not in cfg:
            logging.warning(
                f"no option specified for command '{cmd}' will use default if possible"
            )

        cmd_args = {}
        cmd_args.update(default_args)
        cmd_args.update(cfg[f"{cmd}:options"])
        if f"{cmd}:way-list" in cfg:
            cmd_args.update({"way-list": cfg.get_way_list(cmd)})
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

if __name__ == "__main__":
    path = "/home/sts/repos/vadere/Scenarios/Demos/roVer/scenarios/test.config"
    args = parse_command_line_arguments(["use-config", "-c", path])
    args.main_func(args)
