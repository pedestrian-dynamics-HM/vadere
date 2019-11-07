### PASTE THIS INTO IPYTHON-CONSOLE
### DOES NEITHER WORK IN PYTHON-SCRIPT NOR IN JUPYTER-NOTEBOOK
import sys

# append path to tools of sumo
sys.path.append('/Users/Philipp/sumo-1.3.1/tools')

import traci
import struct

def go(data):
    con = traci.connect(port=9999)
    con._queue.append(1)
    con._string = data

scenPath = r"/Users/Philipp/Repos/vadere/Scenarios/Demos/roVer/scenarios/scenario002.scenario"
scenFile = open(scenPath, 'r')
scenario = scenFile.read()

data = struct.pack("BBBBBBBBBB",0,0,0,34,13,117,0,0,0,4)\
    + "Test".encode('us-ascii')\
    + struct.pack('BBBB',0,0,33,251)\
    + scenario.encode('us-ascii')

# connect and sendFile
go(data)