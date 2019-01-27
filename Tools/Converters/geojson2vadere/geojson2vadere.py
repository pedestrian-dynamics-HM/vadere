# Convert a GeoJSON file to a Vadere topology (in Cartesian coordinates).
#
# Steps to run this script:
#
# 1. Go to https://opmops.virtualcitysystems.de/dss/kaiserslautern/#/
# 2. Click "Weitere Funktionen" icon -> "Export" icon -> "Vektor" -> "ALK Gebäudegrundrisse" -> "Weiter".
# 3. Adjust region of intereset and click "Weiter".
# 4. Set "Ausgabformat" = JSON and "Rückgabe" = EPSG:25832, and click "Anfrage senden".
# 3. Call script and pass exported file from (4):
#
#      python3 <script> <exported_file>
#
# 4. Insert output into "topography" tab of Vadere.
#
# Watch out: before running this script, install its dependencies using pip: 
#
#   pip install -r requirements.txt
#
# Example GeoJSON file:
#
#   {
#     "type":"FeatureCollection",
#     "totalFeatures":332,
#     "features":[
#       {
#         "type":"Feature",
#         "id":"Gebaeude.45",
#         "geometry":{
#           "type":"MultiPolygon",
#           "coordinates":[
#             [
#               [
#                 [
#                   410260.098,
#                   5476786.396
#                 ],
#                 ...
#               ]
#             ]
#           ]
#         },
#         "geometry_name":"the_geom",
#         "properties":{
#           "LAYER":"Unknown Area Type",
#           "ELEVATION":0,
#           "ID":0
#         }
#       },
#       ...
#     ],
#     "crs":{
#       "type":"name",
#       "properties":{
#         "name":"urn:ogc:def:crs:EPSG::25832"
#       }
#     }
#   }
#
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

from string import Template

import argparse
import json
import math

def parse_command_line_arguments():
    parser = argparse.ArgumentParser(description="Convert a GeoJSON file to a Vadere topology description.")
    parser.add_argument("filename", type=str, nargs="?",
                        help="A GeoJSON file.",
                        default="maps/map_kaiserslautern_pollichstraße.json",
                        )
    parser.add_argument("-o", "--output", type=str, nargs="?",
                  help="Specify filename to write the output to file instead of stdout.")

    args = parser.parse_args()

    return args

def parse_geojson_file(filename):
    with open(filename, "r") as json_file:
        return json.load(json_file)

def filter_for_buildings(geojson_content):
    # Top-level keys of a GeoJSON file: type, totalFeatures, features, crs
    buildings = []

    for feature in geojson_content["features"]:
        geometry = feature["geometry"]

        if geometry["type"] == "MultiPolygon":
            # Watch out: the coordinates of a building are wrapped within two lists (each has length one).
            vertices_of_building_as_a_single_list = geometry["coordinates"][0][0]
            buildings.append(vertices_of_building_as_a_single_list)

    return buildings

def convert_buildings_to_vadere_obstacle_strings(buildings):
    list_of_vadere_obstacles_as_strings = []

    # A single building is a list of vertices.
    for building in buildings:
        vadere_obstacle_as_string = create_vadere_obstracle_from_vertices(building)
        list_of_vadere_obstacles_as_strings.append(vadere_obstacle_as_string)

    return list_of_vadere_obstacles_as_strings

def create_vadere_obstracle_from_vertices(cartesian_points):
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

def find_minimal_coordinates(buildings):
    all_vertices = [vertex for building in buildings for vertex in building]
    
    tuple_with_min_x = min(all_vertices, key=lambda point: point[0])
    tuple_with_min_y = min(all_vertices, key=lambda point: point[1])

    return (tuple_with_min_x[0], tuple_with_min_y[1])

def find_maximum_coordinates(buildings):
    all_vertices = [vertex for building in buildings for vertex in building]
    
    tuple_with_max_x = max(all_vertices, key=lambda point: point[0])
    tuple_with_max_y = max(all_vertices, key=lambda point: point[1])

    rounded_max_x = math.ceil(tuple_with_max_x[0])
    rounded_max_y = math.ceil(tuple_with_max_y[1])
    
    return (rounded_max_x, rounded_max_y)

def shift_buildings(buildings, shift_in_x_direction, shift_in_y_direction):
    shifted_buildings = []

    for building in buildings:
        shifted_building = \
            [(vertex[0] + shift_in_x_direction, vertex[1] + shift_in_y_direction) for vertex in building]
        shifted_buildings.append(shifted_building)

    return shifted_buildings

if __name__ == "__main__":
    args = parse_command_line_arguments()

    geojson_content = parse_geojson_file(args.filename)

    # A single building is a list of vertices.
    buildings = filter_for_buildings(geojson_content)

    # Find minimal coordinates to shift to (0,0) to be able to run simulation in Vadere.
    minimal_coordinates = find_minimal_coordinates(buildings)
    shifted_buildings = shift_buildings(buildings, shift_in_x_direction=-minimal_coordinates[0], shift_in_y_direction=-minimal_coordinates[1])
    
    vadere_obstacles_as_strings = convert_buildings_to_vadere_obstacle_strings(shifted_buildings)

    topography_width, topography_height = find_maximum_coordinates(shifted_buildings)

    # TODO: Create Vadere topology and print topology stdout/file.
