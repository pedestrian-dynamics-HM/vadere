import numpy as np


def write_matrix_to_file(matrix, distribution, file):
    number_of_elements = np.size(matrix)
    row = np.reshape(matrix, (1, number_of_elements)).flatten('C')  # row-wise
    row = np.concatenate((row, distribution))
    row_string = ';'.join(map(lambda r: "{%.10f}",row)) + '\n'  # TODO limit to 10 decimals
    file.write(row_string)
    file.flush()


def get_output_file_name(distribution, name='density'):
    return "{}_{}_{}_{}".format(name, int(distribution[0] * 100), int(distribution[1] * 100), int(distribution[2] * 100))

