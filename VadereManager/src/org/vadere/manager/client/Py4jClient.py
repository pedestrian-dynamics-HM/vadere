from py4j.java_gateway import JavaGateway, java_import
from IPython import embed
from pythontraciwrapper import PersonapiWrapper, SimulationapiWrapper, PolygonapiWrapper
from pythontraciwrapper import ControllWrapper

class Py4jClient():

    def __init__(self):

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

    def start(self, scenarioPath, interactive=False):
        self.ctr.sendFile(scenarioPath)
        if interactive is True:
            embed()


scenarioPath = "scenario002"
client = Py4jClient()
client.start(scenarioPath, interactive=True)
#print(client.pers.getPosition2DList())