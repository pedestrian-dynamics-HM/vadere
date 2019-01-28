# In the given Vadere topography file, filter out all obstacles which are
# outside a boundary box (given by lower left and upper right corner).
#
# A Vadere topography looks like this:
#
#   {
#     ...,
#     "obstacles" : [
#       {
#               "shape" : {
#                   "type" : "POLYGON",
#                   "points" : [
#                     { "x" : 295.1790000000037, "y" : 193.12900000065565 },
#                     { "x" : 299.71000000002095, "y" : 195.3730000006035 },
#                     { "x" : 300.445000000007, "y" : 193.89099999982864 },
#                     { "x" : 295.91399999998976, "y" : 191.6469999998808 },
#                     { "x" : 295.1790000000037, "y" : 193.12900000065565 } ]
#               },
#               "id" : -1
#       } ],
#     "stairs" : [ ],
#     "targets" : [ ],
#     "sources" : [ ],
#     ...
#   }

from string import Template

import argparse
import json
import math

def parse_command_line_arguments():
    parser = argparse.ArgumentParser(description="Filter out all obstacles which are outside a boundary box (given by lower left and upper right corner).")
    parser.add_argument("filename", type=str,
                        help="A Vadere topography file.",
                        )
    parser.add_argument("boundary_box", type=json.loads,
                        help="Boundary box consisting of lower left and upper right corner (syntax: '{\"x1\": 0.0, \"y1\": 0.0, \"x2\": 10.0, \"y2\": 10.0}')",
                        )
    parser.add_argument("-o", "--output", type=str, nargs="?",
                  help="Specify filename to write the output to file instead of stdout.")

    args = parser.parse_args()

    raise_exception_on_invalid_boundary_box(args.boundary_box)

    return args

def raise_exception_on_invalid_boundary_box(boundary_box):
    expected_keys = set(["x1", "y1", "x2", "y2"])
    actual_keys = boundary_box.keys()
    
    is_no_empty_set = bool(expected_keys.symmetric_difference(actual_keys))
    
    if is_no_empty_set:
        raise ValueError("Given boundary box does not contain required keys: {}".format(expected_keys))
    
    if boundary_box["x2"] <= boundary_box["x1"]:
        raise ValueError("Invalid boundary box: x2 <= x1 ({} <= {})".format(boundary_box["x2"], boundary_box["x1"]))
    
    if boundary_box["y2"] <= boundary_box["y1"]:
        raise ValueError("Invalid boundary box: y2 <= y1 ({} <= {})".format(boundary_box["y2"], boundary_box["y1"]))

def parse_vadere_topography_file(filename):
    with open(filename, "r") as json_file:
        return json.load(json_file)
    
def filter_out_obstacles_outside_boundary_box(vadere_topography, boundary_box):
    # Note: Obstacles crossing the boundary box are NOT filtered out.
    
    obstacles = vadere_topography["obstacles"]
    polygon_obstacles = [obstacle for obstacle in obstacles if obstacle["shape"]["type"].lower() == "polygon"]
    
    obstacles_inside_boundary_box = []
    
    for polygon_obstacle in polygon_obstacles:
        vertices = polygon_obstacle["shape"]["points"]
        
        obstacle_is_inside_boundary = False
        
        for vertex in vertices:
            inside_x_boundary = vertex["x"] >= boundary_box["x1"] and vertex["x"] <= boundary_box["x2"]
            inside_y_boundary = vertex["y"] >= boundary_box["y1"] and vertex["y"] <= boundary_box["y2"]
            
            if inside_x_boundary and inside_y_boundary:
                obstacle_is_inside_boundary = True
                break
            
        if obstacle_is_inside_boundary:
            obstacles_inside_boundary_box.append(polygon_obstacle)
            
    return obstacles_inside_boundary_box

def convert_obstacles_to_list(obstacles):
    # Convert "obstacles" into a list of lists. I.e., result[0] returns a list
    # of vertices for first obstacle (a vertice is another list containing two
    # elements). This method is necessary to use methods the following methods
    # (e.g., find_minimal_coordinates(obstacles) which expect a list of lists.
    obstacles_as_list = []
    
    for obstacle in obstacles:
        vertices = obstacle["shape"]["points"]
        
        # Create a list of two elements for each vertice. All vertices are
        # combined into one single list by list comprehension.
        vertices_as_list = [[vertice["x"], vertice["y"]] for vertice in vertices]
        
        obstacles_as_list.append(vertices_as_list)
        
    return obstacles_as_list

def find_minimal_coordinates(obstacles):
    all_vertices = [vertex for obstacle in obstacles for vertex in obstacle]
    
    tuple_with_min_x = min(all_vertices, key=lambda point: point[0])
    tuple_with_min_y = min(all_vertices, key=lambda point: point[1])

    return (tuple_with_min_x[0], tuple_with_min_y[1])

def find_maximum_coordinates(obstacles):
    all_vertices = [vertex for obstacle in obstacles for vertex in obstacle]
    
    tuple_with_max_x = max(all_vertices, key=lambda point: point[0])
    tuple_with_max_y = max(all_vertices, key=lambda point: point[1])

    rounded_max_x = math.ceil(tuple_with_max_x[0])
    rounded_max_y = math.ceil(tuple_with_max_y[1])
    
    return (rounded_max_x, rounded_max_y)

def shift_obstacles(obstacles, shift_in_x_direction, shift_in_y_direction):
    shifted_obstacles = []

    for obstacle in obstacles:
        shifted_obstacle = \
            [(vertex[0] + shift_in_x_direction, vertex[1] + shift_in_y_direction) for vertex in obstacle]
        shifted_obstacles.append(shifted_obstacle)

    return shifted_obstacles

def convert_obstacles_to_vadere_obstacle_strings(obstacles):
    list_of_vadere_obstacles_as_strings = []

    # A single obstacle is a list of vertices.
    for obstacle in obstacles:
        vadere_obstacle_as_string = create_vadere_obstacle_from_vertices(obstacle)
        list_of_vadere_obstacles_as_strings.append(vadere_obstacle_as_string)

    return list_of_vadere_obstacles_as_strings

def create_vadere_obstacle_from_vertices(vertices):
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

    points_as_string = [point_string_template.substitute(x=x, y=y) for x, y in vertices]
    points_as_string_concatenated = ",\n".join(points_as_string)

    vadere_obstacle_as_string = obstacle_string_template.substitute(points=points_as_string_concatenated)
    
    return vadere_obstacle_as_string

def create_vadere_topography_with_obstacles(obstacles, width, height):
    with open("vadere_topography_template.txt", "r") as template_file:
        vadere_topography_template = template_file.read()

    vadere_topography_string = Template(vadere_topography_template).substitute(obstacles=obstacles, width=width, height=height)

    return vadere_topography_string

def write_parsing_statistics(filename, boundary_box, all_obstacles, obstacles_inside_boundary_box, shift_obstacles_by):
    print("Filename: {}".format(filename))
    print("  Boundary box: {}".format(boundary_box))
    print("  All obstacles: {}".format(len(all_obstacles)))
    print("  Obstacles inside boundary box: {}".format(len(obstacles_inside_boundary_box)))
    print("  Shift obstacles by (x, y) for simulation: {}".format(shift_obstacles_by))

def write_vadere_topography_string_to(vadere_topography_string, output_filename):
    if output_filename == None:
        print(vadere_topography_string)
    else:
        with open(output_filename, "w") as text_file:
            print(vadere_topography_string, file=text_file)

if __name__ == "__main__":
    args = parse_command_line_arguments()
    
    vadere_topography = parse_vadere_topography_file(args.filename)
    
    obstacles_inside_boundary_box = filter_out_obstacles_outside_boundary_box(vadere_topography, args.boundary_box)
    obstacles_inside_boundary_box_as_list = convert_obstacles_to_list(obstacles_inside_boundary_box)

    shift_obstacles_by = find_minimal_coordinates(obstacles_inside_boundary_box_as_list)
    shifted_obstacles = shift_obstacles(obstacles_inside_boundary_box_as_list, shift_in_x_direction=-shift_obstacles_by[0], shift_in_y_direction=-shift_obstacles_by[1])
    
    topography_width, topography_height = find_maximum_coordinates(shifted_obstacles)
    
    vadere_obstacles_as_strings = convert_obstacles_to_vadere_obstacle_strings(shifted_obstacles)
    vadere_obstacles_as_strings_concatenated = ",\n".join(vadere_obstacles_as_strings)

    vadere_topography_string = create_vadere_topography_with_obstacles(vadere_obstacles_as_strings_concatenated, topography_width, topography_height)
    
    write_parsing_statistics(args.filename, args.boundary_box, vadere_topography["obstacles"], obstacles_inside_boundary_box, shift_obstacles_by)
    
    write_vadere_topography_string_to(vadere_topography_string, args.output)
