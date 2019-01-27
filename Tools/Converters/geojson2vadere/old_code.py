def build_vadere_topography_input_with_obstacles(obstacles, width, height):
    with open("vadere_topography_default.txt", "r") as myfile:
        vadere_topography_input = myfile.read().replace('\n', '')

    vadere_topography_output = Template(vadere_topography_input).substitute(width=width, height=height, obstacles=obstacles)

    return vadere_topography_output

def print_output(output_filename, output):
    if output_filename == None:
        print(output)
    else:
        with open(output_filename, "w") as text_file:
            print(output, file=text_file)
