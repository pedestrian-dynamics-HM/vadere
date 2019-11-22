from py4j.java_gateway import JavaGateway
from IPython import embed

if __name__ == '__main__':

    gateway = JavaGateway()
    entryPoint = gateway.entry_point

    pers = entryPoint.getPersonapi()
    sim = entryPoint.getSimulationapi()
    poly = entryPoint.getPolygonapi()

    embed()