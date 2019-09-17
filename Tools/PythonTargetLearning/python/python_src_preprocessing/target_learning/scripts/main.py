'''
Main script to run target learning:
usage (run from target_learning directory)
    python scripts\main.py with <path_to_config.json> <additional configuration> --force --filestorage <path_for_output>

    example:
        python scripts\main.py with scripts\t_junction\hybrid.json "number_of_cores=4" --force --filestorage ../runs/hybrid

    Note: 
        1. A config file path is needed
        2. Additional configurations can be omitted if no value overloading of the config.json is needed
        3. The '--force' CANNOT be omitted, the script will not run without it!!
        4. --filestorage is technically not necessary, but without it no results what so ever will be saved!!
'''

import logging
import os
import sys
import random

from sklearn.model_selection import train_test_split
from sklearn.utils import shuffle

module_path = os.path.abspath(os.path.join('.'))

if module_path not in sys.path:
    sys.path.append(module_path)

from utils.reader.densitymap import ingredient as density_ingredient, load_directory, load_file
from rf.regression import ingredient as rf_ingredient, multiple_forests as multiple

from sacred.experiment import Experiment
from utils.sacred.file_storage import FileStorageObserver, filestorage_cli

# create experiment
ex = Experiment('target_learning', ingredients=[density_ingredient, rf_ingredient], additional_cli_options=[filestorage_cli])


logger = logging.getLogger('t_junction.hybrid')

# add stream handler to logger
ch = logging.StreamHandler()
ch.setFormatter(logging.Formatter('%(levelname)s - %(name)s - %(message)s'))
logger.addHandler(ch)
logger.setLevel(logging.INFO)

ex.logger = logger

@ex.automain
def main(_run, _log, _config):
    
    number_of_targets = _config.get('number_of_targets', None)
    dataset = _config.get('dataset', None)

    if dataset is None:
        # TODO: add error message
        exit(1)

    if number_of_targets is None:
        # TODO: add error message
        exit(1)

    # load dataset depending on input
    path = dataset.get('path', None)
    shuffle_data = dataset.get('shuffle', True)
    maps, distributions = (None, None)
    if path is not None:
        if os.path.isfile(path):
            maps, distributions = load_file(path, number_of_targets)
        else:
            maps, distributions = load_directory(path, number_of_targets)

        testsize = _config.get('testsize', 0.2)
        train_samples, test_samples, train_targets, test_targets = train_test_split(maps, distributions, shuffle=shuffle_data, test_size=testsize)
    else:
        training_path = dataset.get('training', None)
        testing_path = dataset.get('testing', None)

        if training_path is None or testing_path is None:
            exit(1)

        train_samples, train_targets = load_directory(training_path, number_of_targets)
        test_samples, test_targets = load_directory(testing_path, number_of_targets)

        if shuffle_data:
            train_samples, train_targets = shuffle(train_samples, train_targets)
            test_samples, test_targets = shuffle(test_samples, test_targets)
  
    # run experiment
    regressors, \
    normed_prediction, \
    (score_training, score_test, score_oob), \
    errors = multiple( \
        {'samples': train_samples, 'targets': train_targets}, \
        {'samples': test_samples, 'targets': test_targets}, \
    number_of_targets=number_of_targets, \
    number_of_trees=_config.get('number_of_trees', 20), \
    number_of_cores=_config.get('number_of_cores', 4), \
    save=_config.get('save', True))