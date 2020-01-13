import argparse
import atexit
import os
import subprocess
from time import sleep, time
from typing import List

from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters

from pythontraciwrapper import ControllWrapper
from pythontraciwrapper import PersonapiWrapper, SimulationapiWrapper, PolygonapiWrapper, VadereapiWrapper


class Py4jClient:

    def __init__(self, vaderePath, args=""):

        cmd_entrypoint_process = ["java", "-jar", os.path.join(vaderePath,
                                                               "VadereManager/target/vadere-traci-entrypoint.jar")]
        if isinstance(args, List):
            self._argParser = argparse.ArgumentParser(description='Py4jClient.')
            self._addArgs()
            self._traciEntrypointArgs = self._buildTraciEntrypointArgString(args)
        else:
            raise ValueError("args must be of type List")

        cmd_entrypoint_process.extend(self._traciEntrypointArgs)
        self._setup_entrypointProcess(cmd_entrypoint_process)
        self._start_gateway()

    def startScenario(self, scenarioName):
        self.ctr.sendFile(scenarioName)

    def _start_gateway(self):

        # Gateeway
        gp = GatewayParameters(port=self.args.javaPort)
        cp = CallbackServerParameters(port=self.args.pythonPort)
        gateway = JavaGateway(gateway_parameters=gp, callback_server_parameters=cp)
        entryPoint = gateway.entry_point

        # api
        startTime = time()
        maxWaitingTime = 60.
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
            except Exception as e:
                print("TraciEntryPoint not ready after " + str(round(time() - startTime, 2)) + " seconds, wait..")
                sleep(0.1)

        # wrap api
        self.ctr = ControllWrapper(control, gateway)
        self.pers = PersonapiWrapper(personapi, gateway)
        self.sim = SimulationapiWrapper(simulationapi, gateway)
        self.va = VadereapiWrapper(vadereapi, gateway)
        self.poly = PolygonapiWrapper(polygonapi, gateway)

    def _setup_entrypointProcess(self, cmdEntrypointProcess):
        self._entrypointProcess = subprocess.Popen(cmdEntrypointProcess)
        atexit.register(self._killProcessAtExit)

    def _killProcessAtExit(self):
        self._entrypointProcess.kill()

    def _buildTraciEntrypointArgString(self, argString):

        self.args = self._argParser.parse_args(argString)
        arg_list = list()
        arg_list.extend(["--loglevel", self.args.loglevel])
        if self.args.logname is not None:
            arg_list.extend([" --logname", self.args.logname])
        arg_list.extend(["--port", str(self.args.port)])
        arg_list.extend(["--java-port", str(self.args.javaPort)])
        arg_list.extend(["--python-port", str(self.args.pythonPort)])
        arg_list.extend(["--clientNum", str(self.args.clientNum)])
        if self.args.guiMode is True:
            arg_list.append("--gui-mode")
        arg_list.extend(["--base-path", str(self.args.basePath)])
        arg_list.extend(["--default-scenario", str(self.args.defaultScenario)])
        return arg_list

    def _addArgs(self):
        self._argParser.add_argument("--loglevel",
                                     required=False,
                                     type=str,
                                     dest="loglevel",
                                     choices=["OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG",
                                              "TRACE", "ALL"],
                                     default="INFO",
                                     help="Set Log Level.")
        self._argParser.add_argument("--logname",
                                     required=False,
                                     type=str,
                                     dest="logname",
                                     help="Write log to given file.")
        self._argParser.add_argument('--port',
                                     required=False,
                                     type=int,
                                     default=9998,
                                     help="Set port number.")
        self._argParser.add_argument('--java-port',
                                     required=False,
                                     type=int,
                                     default=10001,
                                     dest="javaPort",
                                     help="Set port number of gateway server for java.")
        self._argParser.add_argument('--python-port',
                                     required=False,
                                     type=int,
                                     default=10002,
                                     dest="pythonPort",
                                     help="Set port number of gateway server for python.")
        self._argParser.add_argument("--clientNum",
                                     required=False,
                                     type=int,
                                     default=4,
                                     dest="clientNum",
                                     help="Set number of clients to manager. Important: Each client has a separate simulation." +
                                          " No communication between clients.")
        self._argParser.add_argument("--gui-mode",
                                     required=False,
                                     type=bool,
                                     dest="guiMode",
                                     help="Start server with GUI support. If a scenario is reveived show the current state of " +
                                          "the scenario")
        self._argParser.add_argument("--base-path",
                                     required=True,
                                     type=str,
                                     dest="basePath",
                                     help="Scenario directory")
        self._argParser.add_argument("--default-scenario",
                                     required=False,
                                     type=str,
                                     dest="defaultScenario",
                                     help="Supply a default scenario")
