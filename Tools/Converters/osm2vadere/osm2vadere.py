# Convert an OpenStreetMap XML file exported from https://www.openstreetmap.org/
# to a Vadere topology (in Cartesian coordinates).
#
# Steps to run this script:
#
# 1. Go to https://www.openstreetmap.org/.
# 2. Click "Export" and adjust region of intereset and zoom level.
# 3. Call script and pass exported file from (2):
#
#      python3 <script> <exported_file>
#
# 4. Insert output into "topography" tab of Vadere.
#
# Note: currently, the scripts converts only buildings to Vadere obstacles.
#
# Watch out: before running this script, install its dependencies using pip: 
#
#   pip install -r requirements.txt
#
# An OpenStreetMap XML file has following structure:
#
# 1. The boundary box of the exported tile.
# 2. List of all nodes in the tile containing latitude and longitude.
# 3. Paths are formed using references to (2).
#
# Example OSM file:
#
#   <?xml version="1.0" encoding="UTF-8"?>
#   <osm version="0.6" ...>
#    <bounds minlat="47.8480100" minlon="11.8207100" maxlat="47.8495600" maxlon="11.8249200"/>
#    <node id="31413334" ... lat="47.8563764" lon="11.8186396"/>
#    ...
#    <way id="192686406" ...>
#     <nd ref="31413334"/>
#     <nd ref="3578052113"/>
#     <nd ref="394769402"/>
#     <nd ref="3828186058"/>
#     <tag k="highway" v="unclassified"/>
#    </way>
#   ...
#
# A Vadere obstacle looks like this:
#
#   {
#   "shape" : {
#       "type" : "POLYGON",
#       "points" : [ { "x" : 43.7, "y" : 3.4 }, ... ]
#     },
#    "id" : -1
#   }

from lxml import etree
from string import Template

import argparse
import utm
import math

def parse_command_line_arguments():
    parser = argparse.ArgumentParser(description="Convert and OpenStreetMap file to a Vadere topology description.")
    parser.add_argument("filename", type=str, nargs="?",
                        help="An OSM map in XML format.",
                        default="maps/paris_eiffel_tower.osm",
                        )
    parser.add_argument("-o", "--output", type=str, nargs="?",
                  help="Specify filename if you want the output in a file.")

    args = parser.parse_args()

    return args


def extract_latitude_and_longitude_for_each_xml_node(xml_tree):
    # Select all nodes (not only buildings).
    nodes = xml_tree.xpath("/osm/node")

    nodes_dictionary_with_lat_and_lon = {node.get("id"): (node.get("lat"), node.get("lon")) for node in nodes}

    return nodes_dictionary_with_lat_and_lon


def filter_for_buildings(xml_tree):
    # Select "way" nodes with a child node "tag" annotated with attribute "k='building'".
    buildings = xml_tree.xpath("/osm/way[./tag/@k='building']")

    return buildings


def filter_for_buildings_in_relations(xml_tree):
    # Note: A relation describes a shape with "cutting holes".

    # Select "relation" nodes with a child node "tag" annotated with attribute "k='building'".
    buildings = xml_tree.xpath("/osm/relation[./tag/@k='building']")

    # We only want the shapes and only the outer parts. role='inner' is for "cutting holes" in the shape.
    members_in_the_relations = [building.xpath("./member[./@type='way' and ./@role='outer']") for building in buildings]
    way_ids = []
    for element in members_in_the_relations:
        for way in element:
            way_ids.append(way.get("ref"))
    ways = xml_tree.xpath("/osm/way")
    ways_as_dict_with_id_key = {way.get("id"): way for way in ways}
    buildings_from_relations = [ways_as_dict_with_id_key[id] for id in way_ids]
    return buildings_from_relations


def extract_base_point(xml_tree):
    base_node = xml_tree.xpath("/osm/bounds")[0]
    base_point = (base_node.get("minlat"), base_node.get("minlon"))

    return base_point


def assert_that_start_and_end_point_are_equal(node_references):
    assert node_references[0].get("ref") == node_references[-1].get("ref")


def convert_nodes_to_cartesian_points(nodes, lookup_table_latitude_and_longitude):
    cartesian_points = []

    # Omit last node because it should be the same as the first one.
    for node in nodes[:len(nodes) - 1]:
        reference = node.get("ref")
        latitude, longitude = lookup_table_latitude_and_longitude[reference]

        x, y, zone_number, zone_letter = utm.from_latlon(float(latitude), float(longitude))
        point = (x, y)

        # TODO: assert that ALL nodes fall into same zone_number and zone_letter!
        cartesian_points.append(point)

    return cartesian_points


def create_vadere_obstacles_from_points(cartesian_points):
    vadere_obstacle_string = """{
        "shape" : {
            "type" : "POLYGON",
            "points" : [ $points ]
        },
        "id" : -1
}"""
    vadere_point_string = '{ "x" : $x, "y" : $y }'

    obstacle_string_template = Template(vadere_obstacle_string)
    point_string_template = Template(vadere_point_string)

    points_as_string = [point_string_template.substitute(x=x, y=y) for x, y in cartesian_points]
    points_as_string_concatenated = ",\n".join(points_as_string)

    vadere_obstacle_as_string = obstacle_string_template.substitute(points=points_as_string_concatenated)

    return vadere_obstacle_as_string


def build_vadere_topography_input_with_obstacles(obstacles, width, height):
    with open("vadere_topography.template", "r") as myfile:
        vadere_topography_input = myfile.read().replace('\n', '')

    vadere_topography_output = Template(vadere_topography_input).substitute(width=width, height=height, obstacles=obstacles)

    return vadere_topography_output


def print_xml_parsing_statistics(filename, nodes_dictionary, simple_buildings, complex_buildings, base_point):
    print("File: {}".format(filename))
    print("  Nodes: {}".format(len(nodes_dictionary)))
    print("  Simple buildings: {}".format(len(simple_buildings)))
    print("  Complex buildings: {}".format(len(complex_buildings)))
    print("  Base point: {}".format(base_point))


def print_output(outputfile, output):
    if outputfile == None:
        print(output)
    else:
        with open(outputfile, "w") as text_file:
            print(output, file=text_file)


def find_width_and_height(buildings_cartesian):
    # search for the highest x- and y-coordinates within the points
    width = 0
    height = 0
    for cartesian_points in buildings_cartesian:
        for point in cartesian_points:
            width = max(width, point[0])
            height = max(height, point[1])
    return math.ceil(width), math.ceil(height)

def find_new_basepoint(buildings_cartesian):
    # "buildings_cartesian" is a list of lists!!!
    # The inner list contains the (x,y) tuples!
    # search for the lowest x- and y-coordinates within the points
    all_points = [point for building in buildings_cartesian for point in building]

    tuple_with_min_x = min(all_points, key=lambda point: point[0])
    tuple_with_min_y = min(all_points, key=lambda point: point[1])

    return (tuple_with_min_x[0], tuple_with_min_y[1])


def shift_points(buildings_utm, shift_in_x_direction, shift_in_y_direction):
    new_buildings = []
    for cartesian_points in buildings_utm:
        shifted_cartesian_points = \
            [(point[0] + shift_in_x_direction, point[1] + shift_in_y_direction) for point in cartesian_points]
        new_buildings.append(shifted_cartesian_points)
    return new_buildings


def convert_buildings_to_cartesian(buildings_as_xml_nodes):
    buildings_in_cartesian = []
    for building in buildings_as_xml_nodes:
        # Collect nodes that belong to the current building.
        node_references = building.xpath("./nd")

        assert_that_start_and_end_point_are_equal(node_references)
        cartesian_points = convert_nodes_to_cartesian_points(node_references, nodes_dictionary_with_lat_and_lon)

        buildings_in_cartesian.append(cartesian_points)
    return buildings_in_cartesian


def convert_buildings_as_cartesian_to_buildings_as_vadere_obstacles(buildings_as_cartesian):
    list_of_vadere_obstacles_as_strings = []
    for cartesian_points in buildings_as_cartesian:
        vadere_obstacles_as_strings = create_vadere_obstacles_from_points(cartesian_points)
        list_of_vadere_obstacles_as_strings.append(vadere_obstacles_as_strings)
    return list_of_vadere_obstacles_as_strings


if __name__ == "__main__":
    args = parse_command_line_arguments()

    xml_tree = etree.parse(args.filename)

    nodes_dictionary_with_lat_and_lon = extract_latitude_and_longitude_for_each_xml_node(xml_tree)

    simple_buildings = filter_for_buildings(xml_tree)
    complex_buildings = filter_for_buildings_in_relations(xml_tree)
    extracted_base_point = extract_base_point(xml_tree)

    print_xml_parsing_statistics(args.filename, nodes_dictionary_with_lat_and_lon, simple_buildings, complex_buildings, extracted_base_point)

    buildings_as_cartesian = convert_buildings_to_cartesian(simple_buildings + complex_buildings)

    # make sure everything lies within the topography
    new_base = find_new_basepoint(buildings_as_cartesian)
    buildings_as_cartesian = shift_points(buildings_as_cartesian, -new_base[0], -new_base[1])
    width_topography, height_topography = find_width_and_height(buildings_as_cartesian)

    list_of_vadere_obstacles_as_strings = convert_buildings_as_cartesian_to_buildings_as_vadere_obstacles(buildings_as_cartesian)

    obstacles_joined = ",\n".join(list_of_vadere_obstacles_as_strings)

    vadere_topography_output = build_vadere_topography_input_with_obstacles(obstacles_joined, width_topography, height_topography)
    print_output(args.output, vadere_topography_output)
