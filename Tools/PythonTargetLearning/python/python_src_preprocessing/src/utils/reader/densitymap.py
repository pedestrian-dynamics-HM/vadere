import os
import numpy as np
import pandas as pd
from tqdm import tqdm
from sacred.ingredient import Ingredient

ingredient = Ingredient('reader.densitymap')


@ingredient.config
def cfg():
    seperator = ";"
    file_filter = lambda x: x


@ingredient.capture
def load_file(file, number_of_targets, seperator, _run, _log):
    frame = np.genfromtxt(_run.open_resource(file), delimiter=seperator)

    maps = frame[:, :-number_of_targets]
    distributions = frame[:, -number_of_targets:]

    _log.info("File {} loaded, {} samples, {} features per sample, {} targets per sample".format(file, len(maps), len(maps[0]), len(distributions[0])))

    return maps, distributions


@ingredient.capture
def load_directory(directory, number_of_targets, file_filter, seperator):
    files = list(filter(file_filter, os.listdir(directory)))

    maps = []
    distributions = []

    # for file in tqdm(files, total=len(files), desc="Loading directory {}".format(directory)):
    for file in files:
        fmaps, fdist = load_file(os.path.join(directory, file), number_of_targets, seperator=seperator)

        maps.append(fmaps)
        distributions.append(fdist)


    return np.concatenate(maps), np.concatenate(distributions)
        