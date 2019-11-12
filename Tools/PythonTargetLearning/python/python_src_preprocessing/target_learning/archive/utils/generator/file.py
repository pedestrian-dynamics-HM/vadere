# script for generation the attributes file which contains all attributes for the generated data

# fields

header = "# This file contains all information regarding the generated output data"
section_header = ["# Datatype", "# script version tag","# OBSERVATION_AREA",
                  "# TIME_STEP_BOUNDS", "# RESOLUTION ", "# SIGMA", "# GAUSS_DENSITY_BOUNDS","# FRAMERATE" ,"# scenarios used"]


def generate_attributes_file(output_directory, section_fields):
    with open(output_directory + "\\" + "attributes.txt", mode='w', newline='\n') as file:
        file.writelines(header)
        file.writelines('\n')
        file.writelines('\n')
        for i in range(0, len(section_fields)):
            file.writelines(section_header[i])
            file.writelines('\n')
            file.writelines(section_fields[i])
            file.writelines('\n')
            file.writelines('\n')

        file.flush()
