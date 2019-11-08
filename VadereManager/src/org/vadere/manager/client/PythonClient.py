import sys, os
import argparse
from IPython import embed

import org.vadere.manager.client.pyTraci as traci

def readScenario(scenPath):
    with open(scenPath, 'r') as scenFile:
        scenario = scenFile.read()
    return scenario


if __name__ == '__main__':

    # args
    parser = argparse.ArgumentParser(description="Python client to vadere via traci")
    parser.add_argument('-scenPath', type=str, help='Path to the scenario')
    parser.add_argument('-scenFName', type=str, help='Filename of the scenario. E.g. scenario002.scenario')
    args = parser.parse_args()

    # go
    scenPath = os.path.join(args.scenPath, args.scenFName)
    scenName = "Test"
    scenario = readScenario(scenPath)

    traci.init(port=9999)
    traci.sendFile(["Test", scenario])

    traci.person.add("5", (2.0, 3.0), "1", "2")
    traci.simulationStep(1)

    # interactive
    embed()