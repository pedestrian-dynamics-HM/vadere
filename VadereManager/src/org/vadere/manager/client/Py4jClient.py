# Author: Philipp Schuegraf

import argparse
import sys

from pythontraciwrapper.py4j_client import Py4jClient

def add_arguments(parser):
    parser.add_argument("--loglevel",
                        required=False,
                        type=str,
                        dest="loglevel",
                        choices=["OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"],
                        default="INFO",
                        help="Set Log Level.")
    parser.add_argument("--logname",
                        required=False,
                        type=str,
                        dest="logname",
                        help="Write log to given file.")
    parser.add_argument('--port',
                        required=False,
                        type=int,
                        default=9998,
                        help="Set port number.")
    parser.add_argument("--clientNum",
                        required=False,
                        type=int,
                        default=4,
                        dest="clientNum",
                        help="Set number of clients to manager. Important: Each client has a separate simulation. No communication between clients.")
    parser.add_argument("--gui-mode",
                        required=False,
                        type=bool,
                        dest="guiMode",
                        help="Start server with GUI support. If a scenario is reveived show the current state of the scenario")

    return parser

if __name__ == '__main__':

    # Argparsing
    parser = argparse.ArgumentParser(description='Py4jClient.')
    parser = add_arguments(parser)
    args = parser.parse_args(sys.argv[1:])

    traciEntrypointArgString = ""
    traciEntrypointArgString += " --loglevel " + args.loglevel
    if not args.logname is None:
        traciEntrypointArgString += " --logname " + args.logname
    traciEntrypointArgString += " --port " + str(args.port)
    traciEntrypointArgString += " --clientNum " + str(args.clientNum)
    if args.guiMode is True:
        traciEntrypointArgString += " --gui-mode"

    scenarioPath = "scenario002"
    cli = Py4jClient(traciEntrypointArgString)
    cli.start(scenarioPath, interactive=True)
    #print(cli.sim.createWaitingArea("6", 0.0, 3.0, 1, 0.5, 10.0, ["6.0", "3.0", "8.0", "3.0", "8.0", "0.5", "6.0", "0.5"]))
    #print(client.pers.setTargetList("1", ["2", "3"]))
    #print(client.pers.getPosition2DList())