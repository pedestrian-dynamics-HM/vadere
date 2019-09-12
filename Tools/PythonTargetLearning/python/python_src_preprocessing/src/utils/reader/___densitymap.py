import os


import pandas as pd
import numpy as np

from tqdm import tqdm


def load_file(file, number_of_targets=3, seperator=';'):
    # read in data
    frame = pd.read_csv(file, header=None, sep=seperator)
    
    targets = frame[frame.columns[-number_of_targets:]]
    map = frame.iloc[:, 0:-number_of_targets]

    return map, targets


def load_directory(directory, number_of_targets=3, seperator=';', file_filter=lambda x: x):
    files = list(filter(file_filter, os.listdir(directory)))

    map = pd.DataFrame()
    distribution = pd.DataFrame()
    for file in tqdm(files, desc='loading directory ' + directory, total=len(files)):
        fmap, fdistribution = load_file(os.path.join(directory, file), number_of_targets, seperator=seperator)
        map = map.append(fmap)
        distribution = distribution.append(fdistribution)

    return map, distribution