import os
import numpy as np
from sacred.ingredient import Ingredient

ingredient = Ingredient('reader.densitymap')


@ingredient.config
def cfg():
    seperator = ","
    file_filter = lambda x: x


# loads data file (density and distributions) in a directory
# first n columns contains density values and last number_of_targets columns contains OD-distributions
# directory : file path to the input directory
# number_of_targets : number of targets used in scenario
# file_filter : todo ?
# separator : csv format using ; or ,
@ingredient.capture
def load_file(file, number_of_targets, seperator, _run, _log):
    print("loading next file:", _run.open_resource(file))
    frame = np.genfromtxt(_run.open_resource(file), delimiter=seperator)
    print(np.shape(frame))
    maps = frame[:, :-number_of_targets]
    distributions = frame[:, -number_of_targets:]

    _log.info("File {} loaded, {} samples, {} features per sample, {} targets per sample".format(file, len(maps),
                                                                                                 len(maps[0]),
                                                                                                 len(distributions[0])))

    return maps, distributions


# loads all files in a directory and concatenates the maps and distribution vectors
# first n columns contains density values and last number_of_targets columns contains OD-distributions
# directory : file path to the input directory
# number_of_targets : number of targets used in scenario
# file_filter : todo ?
# separator : csv format using ; or ,
@ingredient.capture
def load_directory(directory, number_of_targets, file_filter, seperator):
    files = list(filter(file_filter, os.listdir(directory)))

    maps = []
    distributions = []

    for file in files:
        fmaps, fdist = load_file(os.path.join(directory, file), number_of_targets, seperator=seperator)

        maps.append(fmaps)
        distributions.append(fdist)

    return np.concatenate(maps), np.concatenate(distributions)