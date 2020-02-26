import argparse
import os
from time import sleep, time
from typing import List

from py4j.java_gateway import (CallbackServerParameters, GatewayParameters,
                               JavaGateway)
from pythontraciwrapper import (ControlWrapper, PersonapiWrapper,
                                PolygonapiWrapper, SimulationapiWrapper,
                                VadereapiWrapper)
from pythontraciwrapper._runner import Runner


class Py4jClient:
    @classmethod
    def create(
        cls,
        project_path,
        vadere_base_path="",
        gui_mode=False,
        start_server=True,
        server_log=None,
        entry_point_log=None,
        debug=False,
    ):
        return cls(
            vadere_base_path=vadere_base_path,
            args=["--base-path", project_path, "--gui-mode", gui_mode],
            start_server=start_server,
            server_log=server_log,
            entry_point_log=entry_point_log,
            debug=debug,
        )

    def __init__(
        self,
        vadere_base_path="",
        args=None,
        start_server=True,
        server_log=None,
        entry_point_log=None,
        debug=False,
    ):
        self.vadere_base_path = vadere_base_path
        self.start_server = start_server
        self.server_log = server_log
        self.entrypoint_log = entry_point_log
        self.debug = debug
        self.entrypoint_jar = ''

        self._build_argparser()
        if isinstance(args, List):
            self.args = self._parser.parse_args(args)
        else:
            self._parser.parse_args(["--help"])

    def connect(self):
        self._start_entrypoint()
        self._connect_gateway()

    def startScenario(self, scenarioName):
        if scenarioName.endswith(".scenario"):
            self.ctr.sendFile(scenarioName[: -len(".scenario")])
        else:
            self.ctr.sendFile(scenarioName)

    def _connect_gateway(self):

        # Gateeway
        gp = GatewayParameters(port=self.args.javaPort)
        cp = CallbackServerParameters(port=self.args.pythonPort)
        gateway = JavaGateway(gateway_parameters=gp, callback_server_parameters=cp)
        entryPoint = gateway.entry_point

        # api
        startTime = time()
        maxWaitingTime = 5.0
        connected = False
        while not connected and ((time() - startTime) < maxWaitingTime) is True:
            try:
                personapi = entryPoint.getPersonapi()
                simulationapi = entryPoint.getSimulationapi()
                polygonapi = entryPoint.getPolygonapi()
                vadereapi = entryPoint.getVadereapi()
                control = entryPoint.getTraciControl()
                connected = True
                print("Python client connected to java via py4j")
            except Exception:

                print(
                    "TraciEntryPoint not ready after "
                    + str(round(time() - startTime, 2))
                    + " seconds, wait.."
                )
                sleep(1.0)
        if not connected:
            print("Cannot connect to TraciEntryPoint/TraCIServer")
            exit(-1)

        # wrap api
        self.ctr = ControlWrapper(control, gateway, self)
        self.pers = PersonapiWrapper(personapi, gateway, self)
        self.sim = SimulationapiWrapper(simulationapi, gateway, self)
        self.va = VadereapiWrapper(vadereapi, gateway, self)
        self.poly = PolygonapiWrapper(polygonapi, gateway, self)

    def close_threads(self):
        if self.server_thread is not None:
            self.server_thread.stop()

        if self.entrypoint_thread is not None:
            self.entrypoint_thread.stop()

    def _start_entrypoint(self):
        if self.start_server:
            if self.entrypoint_jar == '':
                jar_path = os.path.join(
                    self.vadere_base_path,
                    "VadereManager/target/vadere-server.jar",
                ),
            else:
                jar_path = self.entrypoint_jar

            vadere_server_cmd = [
                "java",
                "-jar",
                jar_path,
            ]
            vadere_server_cmd.extend(self._server_args())
            self.server_thread = Runner(
                command=vadere_server_cmd,
                thread_name="Server",
                log_location=self.server_log,
                use_stdout=self.debug,
            )
            print("Start Server Thread...")
            self.server_thread.start()
            sleep(0.8)
        else:
            self.server_thread = None
            print("Connecting to existing Server...")

        if self.entrypoint_jar == '':
            jar_path = os.path.join(
                self.vadere_base_path,
                "VadereManager/target/vadere-traci-entrypoint.jar",
            ),
        else:
            jar_path = self.entrypoint_jar

        entrypoint_cmd = [
            "java",
            "-jar",
            jar_path,
        ]
        entrypoint_cmd.extend(self._entrypoint_args())
        self.entrypoint_thread = Runner(
            command=entrypoint_cmd,
            thread_name="Entrypoint",
            log_location=self.entrypoint_log,
            use_stdout=self.debug,
        )
        print("Client> Start Entrypoint Thread...")
        self.entrypoint_thread.start()
        sleep(0.8)

    def _server_args(self):
        arg_list = list()
        arg_list.extend(["--loglevel", self.args.loglevel])
        if self.args.logname is not None:
            arg_list.extend([" --logname", self.args.logname])
        arg_list.extend(["--bind", self.args.bind])
        arg_list.extend(["--port", str(self.args.server_port)])
        arg_list.extend(["--clientNum", str(self.args.clientNum)])
        if self.args.guiMode is True:
            arg_list.append("--gui-mode")
        return arg_list

    def _entrypoint_args(self):
        arg_list = list()
        arg_list.extend(["--loglevel", self.args.loglevel])
        if self.args.logname is not None:
            arg_list.extend([" --logname", self.args.logname])
        arg_list.extend(["--bind", self.args.bind])
        arg_list.extend(["--server-port", str(self.args.server_port)])
        arg_list.extend(["--java-port", str(self.args.javaPort)])
        arg_list.extend(["--python-port", str(self.args.pythonPort)])
        arg_list.extend(["--base-path", str(self.args.basePath)])
        arg_list.extend(["--default-scenario", str(self.args.defaultScenario)])
        return arg_list

    def _build_argparser(self):
        self._parser = argparse.ArgumentParser(description="Py4jClient.")
        self._parser.add_argument(
            "--loglevel",
            required=False,
            type=str,
            dest="loglevel",
            choices=["OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"],
            default="INFO",
            help="Set Log Level.",
        )
        self._parser.add_argument(
            "--logname",
            required=False,
            type=str,
            # default="log",
            dest="logname",
            help="Write log to given file.",
        )
        self._parser.add_argument(
            "--bind", required=False, type=str, default="127.0.0.1",
        )
        self._parser.add_argument(
            "--server-port",
            required=False,
            type=int,
            default=9998,
            help="Set port number.",
        )
        self._parser.add_argument(
            "--java-port",
            required=False,
            type=int,
            default=10001,
            dest="javaPort",
            help="Set port number of gateway server for java.",
        )
        self._parser.add_argument(
            "--python-port",
            required=False,
            type=int,
            default=10002,
            dest="pythonPort",
            help="Set port number of gateway server for python.",
        )
        self._parser.add_argument(
            "--clientNum",
            required=False,
            type=int,
            default=4,
            dest="clientNum",
            help="Set number of clients to manager. Important: Each client has a separate simulation."
            + " No communication between clients.",
        )
        self._parser.add_argument(
            "--gui-mode",
            required=False,
            type=bool,
            dest="guiMode",
            help="Start server with GUI support. If a scenario is reveived show the current state of "
            + "the scenario",
        )
        self._parser.add_argument(
            "--base-path",
            required=True,
            type=str,
            dest="basePath",
            help="Scenario directory",
        )
        self._parser.add_argument(
            "--default-scenario",
            required=False,
            type=str,
            dest="defaultScenario",
            help="Supply a default scenario",
        )
