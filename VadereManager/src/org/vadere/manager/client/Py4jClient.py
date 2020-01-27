# Author: Philipp Schuegraf

import sys

from IPython import embed

from pythontraciwrapper.py4j_client import Py4jClient


if __name__ == '__main__':
    baseArgs = sys.argv[1:]
    args1 = baseArgs + ["--port", "9997", "--java-port", "10001", "--python-port", "10002"]
    args2 = baseArgs + ["--port", "9998", "--java-port", "10003", "--python-port", "10004"]

    cli1 = Py4jClient("", args1)
    cli2 = Py4jClient("", args2)

    scenarioPath = "scenario002"
    cli1.startScenario(scenarioPath)
    cli2.startScenario(scenarioPath)
    embed()
    # print(cli.poly.getDistance("1", 0., 0.))
    # print(cli.sim.createWaitingArea("6", 0.0, 3.0, 1, 0.5, 10.0,
    #                                 ["6.0", "3.0", "8.0", "3.0", "8.0", "0.5", "6.0", "0.5"]))
    # print(cli.pers.setTargetList("1", ["2", "3"]))
    # print(cli.pers.getPosition2DList())
