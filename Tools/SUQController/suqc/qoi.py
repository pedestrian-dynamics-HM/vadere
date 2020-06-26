#!/usr/bin/env python3

import os
import re
import time
from typing import *

import pandas as pd
from suqc.environment import VadereEnvironmentManager
from suqc.utils.dict_utils import deep_dict_lookup


class FileDataInfo(object):

    # Implemented in Vadere merge request !38, this is only a fallback mode and requires
    # manual updating if there are changes in Vadere. See also Vadere issue #199 and #201.

    # all output types are from Vadere BUT "GeneralOutputFile"
    map_outputtype2index = {
        "IdOutputFile": 1,
        "LogEventOutputFile": 1,
        "NoDataKeyOutputFile": 0,
        "PedestrianIdOutputFile": 1,
        "TimestepOutputFile": 1,
        "TimestepPedestrianIdOutputFile": 2,
        "TimestepPedestrianIdOverlapOutputFile": 3,
        "TimestepPositionOutputFile": 3,
        "TimestepRowOutputFile": 2,
        "GeneralOutputFile": 1,
    }

    printFallbackMsg = False

    def __init__(
        self, process_file, processors=None, outputkey=None,
    ):
        self.filename = process_file["filename"]
        if outputkey is None:
            self.output_key = process_file["type"].split(".")[-1]
        else:
            self.output_key = outputkey
        self.processors = processors  # not really needed yet, but maybe in future.

        try:
            self.nr_row_indices = self.map_outputtype2index[self.output_key]
        except KeyError:
            if not self.printFallbackMsg:
                self.printFallbackMsg = True
                print(
                    f"WARNING: file type {self.output_key} was not found in list, this may require an update. Setting "
                    f"number of index columns to 1."
                )
            self.nr_row_indices = 1  # use simply first column as index


class QuantityOfInterest(object):
    def __init__(self, requested_files: Union[List[str], str]):

        assert isinstance(requested_files, (list, str))

        if isinstance(requested_files, str):
            requested_files = [requested_files]

        self.req_qois = self._requested_qoi(requested_files)

    def _requested_qoi(self, requested_files):
        req_qois = list()

        for rf in requested_files:
            pf = dict()
            pf["filename"] = rf
            pf["type"] = rf
            # TODO: This has to exactly match, maybe make more robust to allow without the file-ending
            req_qois.append(
                FileDataInfo(process_file=pf, outputkey="GeneralOutputFile")
            )

        return req_qois

    def _read_csv(self, req_qoi: FileDataInfo, filepath):

        # make sure that Vadere writes
        with open(filepath) as f:
            first_line = f.readline()

        try:
            # Tries to use the meta-data line, extract the number of rows
            nr_row_indices = re.search(r"ROW=(\d+)", first_line).group(1)
            nr_row_indices = int(nr_row_indices)
        except AttributeError or ValueError:  # AttributeError -> regex failed | ValueError -> converting to int failed
            # Fallback mode, infer index from the hard-coded list.
            nr_row_indices = req_qoi.nr_row_indices

        df = pd.read_csv(filepath, delimiter=" ", header=[0], comment="#")

        if req_qoi.nr_row_indices != 0:
            idx_keys = df.columns[:nr_row_indices]
            return df.set_index(idx_keys.tolist())
        else:
            return df

    def _add_parid2idx(self, df, par_id, run_id):
        # from https://stackoverflow.com/questions/14744068/prepend-a-level-to-a-pandas-multiindex

        original_column_order = df.index.names
        df["id"] = par_id
        df["run_id"] = run_id
        df.set_index(["id", "run_id"], append=True, inplace=True)

        df = df.reorder_levels(["id", "run_id"] + original_column_order)
        return df

    def read_and_extract_qois(self, par_id, run_id, output_path):

        read_data = dict()

        for k in self.req_qois:
            filepath = os.path.join(output_path, k.filename)
            df_data = self._read_csv(k, filepath)
            read_data[k.filename] = self._add_parid2idx(
                df_data, par_id, run_id
            )  # filename is identifier for QoI

        return read_data


class VadereQuantityOfInterest(QuantityOfInterest):
    def __init__(self, basis_scenario: dict, requested_files: Union[List[str], str]):

        assert isinstance(requested_files, (list, str))

        if isinstance(requested_files, str):
            requested_files = [requested_files]

        user_set_writers, _ = deep_dict_lookup(basis_scenario, "processWriters")
        self.process_files = user_set_writers["files"]
        self.processsors = user_set_writers["processors"]

        super().__init__(requested_files)

    def get_process_files(self):
        return self.process_files

    def get_processors(self):
        return self.processsors

    def _requested_qoi(self, requested_files):

        req_qois = list()
        process_files = self.get_process_files()
        processsors = self.get_processors()

        for pf in process_files:

            # TODO: This has to exactly match, maybe make more robust to allow without the file-ending
            filename = pf["filename"]  # TODO: see issue #33

            if filename in requested_files:
                sel_procs = self._select_corresp_processors(pf, processsors)
                req_qois.append(FileDataInfo(process_file=pf, processors=sel_procs))

                requested_files.remove(
                    filename
                )  # -> processed, list should be empty when leaving function

        if requested_files:  # has to be empty
            raise ValueError(
                f"The requested files {requested_files} are not set in the Vadere scenario: \n "
                f"{process_files}"
            )

        return req_qois

    def _select_corresp_processors(self, process_file, processors):
        proc_ids = process_file["processors"]

        selected_procs = list()

        # TODO: see issue #33
        for pid in proc_ids:
            found = False
            for p in processors:
                if pid == p["id"]:
                    selected_procs.append(p)

                    if not found:
                        found = True
                    else:
                        raise ValueError(
                            "The Vadere scenario is not correctly set up! There are two processors with "
                            f"the id={pid}."
                        )

            if not found:
                raise ValueError(
                    f"The Vadere scenario is not correctly set up! Processor id {pid} could not be found "
                    "in 'processors'."
                )

        return selected_procs


if __name__ == "__main__":
    a = VadereQuantityOfInterest(
        "evacuationTimes.txt", VadereEnvironmentManager("corner")
    )

    print(a.req_qois)
