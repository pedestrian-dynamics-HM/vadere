### PASTE THIS INTO IPYTHON-CONSOLE
### DOES NEITHER WORK IN PYTHON-SCRIPT NOR IN JUPYTER-NOTEBOOK
import sys

# append path to tools of sumo
sys.path.append('/Users/Philipp/sumo-1.3.1/tools')

import traci
import struct

def go(data):
    con = traci.connect(port=9999)
    con.sendFile(data)

scenPath = r"/Users/Philipp/Repos/vadere/Scenarios/Demos/roVer/scenarios/scenario002.scenario"
scenFile = open(scenPath, 'r')
scenario = scenFile.read()

data = ["Test", scenario]

# connect and sendFile
go(data)