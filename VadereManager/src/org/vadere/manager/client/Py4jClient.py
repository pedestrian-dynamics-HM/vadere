from py4j.java_gateway import JavaGateway

if __name__ == '__main__':

    gateway = JavaGateway()
    entryPoint = gateway.entry_point

    personApi = entryPoint.getPersonapi()
    simulationApi = entryPoint.getSimulationapi()
    polygonApi = entryPoint.getPolygonapi()