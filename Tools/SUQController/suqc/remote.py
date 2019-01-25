#!/usr/bin/env python3 

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

import os
import subprocess
import pickle
import abc

from fabric import Connection

from suqc.configuration import SuqcConfig
from suqc.environment import  EnvironmentManager, VadereConsoleWrapper
from suqc.utils.general import create_folder, parent_folder_clean, str_timestamp

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------


class ServerConnection(object):

    READ_VERSION = "python3 -c 'import suqc; print(suqc.__version__)'"

    def __init__(self):
        self._con = None

    @property
    def con(self):
        if self._con is None:
            raise RuntimeError("Server not initialized.")
        return self._con

    def __enter__(self):
        self._connect_server()
        return self

    def __exit__(self, type, value, traceback):
        self.con.close()
        print("INFO: Server connection closed.")

    def get_server_config(self):

        server_cfg = SuqcConfig.load_cfg_file()["server"]

        if not server_cfg["host"] or not server_cfg["user"] or server_cfg["port"] <= 0:
            host = input("Enter host name:")
            user = input("Enter user name:")
            port = int(input("Enter port number (int):"))
            SuqcConfig.store_server_config(host, user, port)
            server_cfg = SuqcConfig.load_cfg_file()["server"]
        else:
            host, user, port = server_cfg["host"], server_cfg["user"], server_cfg["port"]
        print(f"INFO: Trying to connect to ssh -p {port} {user}@{host} . "
              f"Configure at {SuqcConfig.path_suq_config_file()}")
        return server_cfg

    def _connect_server(self):
        config = None
        server_cfg = self.get_server_config()

        self._con: Connection = Connection(server_cfg["host"],
                                           user=server_cfg["user"],
                                           port=server_cfg["port"],
                                           config=config)

        version = self.read_terminal_stdout(ServerConnection.READ_VERSION)
        print(f"INFO: Connection established. Detected suqc version {version} on server side.")

    def read_terminal_stdout(self, s: str) -> str:
        r = self._con.run(s)
        return r.stdout.rstrip()  # rstrip -> remove trailing whitespaces and new lines


class ServerRequest(object):

    def __init__(self):
        self.server = None
        self.remote_env_name = None
        self.remote_folder_path = None
        self.is_setup = False

    def setup_connection(self, server_connection):
        self.server = server_connection

        # NOTE: do NOT use os.path because the remote is linux, but if local is windows wrong slashes are used
        # NOTE: DO NOT EXPAND TILES "~" AS THIS IS MOSTLY NOT SUPPORTED!

        self.remote_env_name = "_".join(["output", str_timestamp()])

        self.remote_folder_path = self._join_linux_path(["suqc_envs", self.remote_env_name], True)
        self._generate_remote_folder()
        self.is_setup = True

    @classmethod
    def open_arg_pickle(cls, remote_pickle_arg_path):
        with open(remote_pickle_arg_path, "rb") as file:
            kwargs = pickle.load(file)
        return kwargs

    @classmethod
    def dump_result_pickle(cls, res, remote_pickle_res_path):
        with open(os.path.abspath(remote_pickle_res_path), "wb") as file:
            pickle.dump(res, file)

    def _generate_remote_folder(self):
        self.server.con.run(f"mkdir {self.remote_folder_path}")

    def _correct_folderpath(self, p):
        if not p.endswith("/"):
            p = "".join([p, "/"])
        return p

    def _join_linux_path(self, p: list, is_folder: bool):
        lpath = "/".join(p)

        if is_folder:
            lpath = self._correct_folderpath(lpath)

        return lpath

    def _default_result_pickle_path_remote(self):
        return self._join_linux_path([self.remote_folder_path, "result.p"], is_folder=False)

    def _default_result_pickle_path_local(self, suitable_path):
        return os.path.join(suitable_path, "results.p")

    def _remote_input_folder(self):
        # input data is directly written to the remote folder
        return self.remote_folder_path

    def remote_output_folder(self):
        # TODO: not so clean to reference to EnvironmentManager
        return self._join_linux_path([self.remote_folder_path, EnvironmentManager.output_folder], True)

    def _transfer_local2remote(self, local_filepath):
        remote_target_path = self._join_linux_path([self.remote_folder_path, os.path.basename(local_filepath)], False)
        self.server.con.put(local_filepath, self.remote_folder_path)
        return remote_target_path

    def _compress_output(self):

        compressed_filepath = self._join_linux_path([self.remote_folder_path, "vadere_output.tar.gz"], is_folder=False)

        # from https://stackoverflow.com/questions/939982/how-do-i-tar-a-directory-of-files-and-folders-without-including-the-directory-it
        s = f"""cd {self.remote_output_folder()} && tar -zcvf ../{os.path.basename(compressed_filepath)} . && cd -"""
        self.server.con.run(s)
        return compressed_filepath

    def _transfer_remote2local(self, remote_path, local_path):
        self.server.con.get(remote_path, local_path)

    def _transfer_model_local2remote(self, model):
        model = VadereConsoleWrapper.infer_model(model)
        return self._transfer_local2remote(local_filepath=model.jar_path)

    def _transfer_compressed_output_remote2local(self, local_path):
        target_file = self._compress_output()

        zip_path = parent_folder_clean(local_path)

        filename = os.path.basename(target_file)
        local_filepath = os.path.join(zip_path, filename)
        self._transfer_remote2local(remote_path=target_file, local_path=local_filepath)

        return local_filepath

    def _transfer_pickle_local2remote(self, **kwargs):
        local_pickle_filepath = os.path.join(SuqcConfig.path_container_folder(), "arguments.p")  # TODO: maybe better to add the Timstamp, to not have any clashes!
        with open(local_pickle_filepath, "wb") as file:
            pickle.dump(kwargs, file)

        remote_pickle_path = self._transfer_local2remote(local_pickle_filepath)
        os.remove(local_pickle_filepath)

        return remote_pickle_path

    def _transfer_pickle_remote2local(self, remote_pickle, local_pickle):
        self.server.con.get(remote_pickle, local_pickle)

        with open(local_pickle, "rb") as file:
            res = pickle.load(file)

        os.remove(local_pickle)

        return res

    def _uncompress_file2target(self, local_compressed_file, path_output):

        if not os.path.exists(path_output):
            create_folder(path_output)

        subprocess.call(["tar", "xvzf", local_compressed_file, "-C", path_output])
        subprocess.call(["rm", local_compressed_file])

    def _remove_remote_folder(self):
        s = f"""rm -r {self.remote_folder_path}"""
        self.server.con.run(s)

    @classmethod
    @abc.abstractmethod
    def _remote_run(cls, **kwargs):
        raise NotImplementedError("Base class")

    @abc.abstractmethod
    def remote(self, **kwargs):
        raise NotImplementedError("Base class")