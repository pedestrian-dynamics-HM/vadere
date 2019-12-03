# Author: Philipp Schuegraf

from pythontraciwrapper.py4j_client import Py4jClient

scenarioPath = "scenario002"
cli = Py4jClient()
cli.start(scenarioPath, interactive=True)
#print(cli.sim.createWaitingArea("6", 0.0, 3.0, 1, 0.5, 10.0, ["6.0", "3.0", "8.0", "3.0", "8.0", "0.5", "6.0", "0.5"]))
#print(client.pers.setTargetList("1", ["2", "3"]))
#print(client.pers.getPosition2DList())