import sys
import org.vadere.manager.client.pyTraci as traci
from IPython import embed

scenPath = r"C:\Users\Philipp\Repos\vadere\Scenarios\Demos\roVer\scenarios\scenario002.scenario"
scenFile = open(scenPath, 'r')
scenario = scenFile.read()

con = traci.connect(port=9999)
con.sendFile(["Test", scenario])
embed()