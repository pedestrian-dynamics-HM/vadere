from py4j.java_gateway import JavaGateway, java_import
from IPython import embed
import subprocess

from pythontraciwrapper import PersonapiWrapper, SimulationapiWrapper, PolygonapiWrapper
from pythontraciwrapper import ControllWrapper

class Py4jClient():

    def __init__(self, traciEntrypointArgString=""):

        cmdEntrypoint = "java -jar C:/Users/Philipp/Repos/vadere/VadereManager/target/vadere-traci-entrypoint.jar"
        cmdEntrypoint += traciEntrypointArgString
        self._entrypoint = subprocess.Popen(cmdEntrypoint)

        # start
        gateway = JavaGateway()
        entryPoint = gateway.entry_point

        # api
        personapi = entryPoint.getPersonapi()
        simulationapi = entryPoint.getSimulationapi()
        polygonapi = entryPoint.getPolygonapi()
        controll = entryPoint.getTraciControll()

        # wrap apis
        self.ctr = ControllWrapper(controll, gateway)
        self.pers = PersonapiWrapper(personapi, gateway)
        self.sim = SimulationapiWrapper(simulationapi, gateway)
        self.poly = PolygonapiWrapper(polygonapi, gateway)

    def __del__(self):
        self._entrypoint.kill()

    def start(self, scenarioPath, interactive=False):
        self.ctr.sendFile(scenarioPath)
        if interactive is True:
            embed()

    # def start(self, scenarioPath, scenFName, interactive=False):
    #     self.ctr.sendFile(scenarioPath, scenFName)
    #     if interactive is True:
    #         embed()