from py4j.java_gateway import JavaGateway, java_import
from IPython import embed
from pythontraciwrapper import PersonapiWrapper
from pythontraciwrapper import ControllWrapper

# if __name__ == '__main__':

# start
gateway = JavaGateway()
entryPoint = gateway.entry_point

# api
personapi = entryPoint.getPersonapi()
simulationapi = entryPoint.getSimulationapi()
polygonapi = entryPoint.getPolygonapi()
controll = entryPoint.getTraciControll()

# controll command
ctr = ControllWrapper(controll, gateway)
ctr.sendFile()

# personapi commands
pers = PersonapiWrapper(personapi, gateway)
print(pers.createNew("5", "5.7", "3.2", ["3"]))
print(pers.getIDList())

# continue interactive
embed()