#!/usr/bin/python3

import argparse
import logging
import os
import select
import signal
import socket
import subprocess
import sys
import threading
import time


"""
vadere-lauchner.py dispatches each client to a new (fresh) instance of a vadere server to ensure that no side effect
of multiple runs within one java VM will influence the simulations.

Script is based on (sumo-launchd.py)[https://github.com/sommer/veins/blob/master/sumo-launchd.py]

"""


class VadereRunner(threading.Thread):
    def __init__(self, client_socket, server_port, options):
        threading.Thread.__init__(self)
        self.client_socket: socket.socket = client_socket
        self.server_port = server_port
        self.options = options
        self.stop = False
        self.vadere = ""
        self.log = logging.getLogger(
            f"vadere-runner-{self.client_socket.getpeername()[1]}:{server_port}"
        )

    def stop_tread(self):
        self.stop = True

    def process_state(self):
        return f"subprocess: pid: {self.vadere.pid} returncode: {self.returncode}"

    def forward_connection(self, server_socket: socket.socket):
        self.client_socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        server_socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

        self.log.debug(
            f"start forwarding {self.client_socket.getpeername()[0]}:{self.client_socket.getpeername()[1]} "
            f"<----->"
            f" {server_socket.getpeername()[0]}:{server_socket.getpeername()[1]}"
        )

        while not self.stop:
            (read, write, exp) = select.select(
                [self.client_socket, server_socket],
                [],
                [self.client_socket, server_socket],
                1,
            )
            if len(exp) != 0:
                self.stop = True
            if self.client_socket in read:
                try:
                    data = self.client_socket.recv(65535)
                    if data == b"":
                        self.stop = True
                except socket.error:
                    self.stop = True
                finally:
                    self.log.debug(f"client>server: {len(data)} Bytes...")
                    server_socket.send(data)

            if server_socket in read:
                try:
                    data = server_socket.recv(65535)
                    if data == b"":
                        self.stop = True
                except socket.error:
                    self.stop = True
                finally:
                    self.log.debug(f"server>client: {len(data)} Bytes...")
                    self.client_socket.send(data)

        self.log.debug(
            f"stop forwarding {self.client_socket.getpeername()[0]}:{self.client_socket.getpeername()[1]} "
            f"<----->"
            f" {server_socket.getpeername()[0]}:{server_socket.getpeername()[1]}"
        )

    def run(self):
        cmd = [
            "java",
            "-jar",
            self.options.jar,
            "--loglevel",
            self.options.vadere_log_level,
            "--port",
            str(self.server_port),
            "--single-client",
        ]

        if self.options.gui:
            cmd.append("--gui-mode")

        try:
            if self.options.vadere_log == "":
                log_file_name = os.devnull
            else:
                log_path = os.path.abspath(
                    os.path.join(
                        self.options.vadere_log,
                        f"vader-port-{self.client_socket.getpeername()[1]}-{self.server_port}.log",
                    )
                )
                os.makedirs(os.path.dirname(log_path), exist_ok=True)
                log_file_name = log_path
                self.log.info(f"crate log at: {log_file_name}")

            log_file = open(log_file_name, "w")
            self.vadere = subprocess.Popen(
                cmd,
                cwd=os.path.curdir,
                shell=False,
                stdin=None,
                stdout=log_file,
                stderr=log_file,
            )

            connected = False
            tries = 1
            while not connected:
                try:
                    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    server_socket.connect(("127.0.0.1", self.server_port))
                    connected = True
                except socket.error as e:
                    self.log.debug(f"connection attempt {tries} : {e}")
                    if tries > 10:
                        raise
                    time.sleep(tries * 0.25)
                    tries += 1
            self.log.debug(f"connection attempt {tries} : OK")

            self.log.info(
                f"started vadere server process (PID:{self.vadere.pid}) at port {self.server_port}"
            )

            self.forward_connection(server_socket)
            self.client_socket.close()
            server_socket.close()

            try:
                self.vadere.wait(timeout=2)
            except subprocess.TimeoutExpired as e:
                self.log.info(
                    f"vadere server process (PID:{self.vadere.pid}) not terminated after timeout. Send SIGKILL"
                )

            if self.vadere.returncode is None:
                os.kill(self.vadere.pid, signal.SIGKLL)
                time.sleep(0.5)
                if self.vadere.returncode is None:
                    self.log.error(
                        f"vadere server process (PID:{self.vadere.pid}) still not dead."
                    )
                    raise

            if log_file_name == os.devnull:
                self.log.info(
                    f"vadere server process (PID:{self.vadere.pid}) exited with returncode: {self.vadere.returncode}"
                )
            else:
                self.log.info(
                    f"vadere server process (PID:{self.vadere.pid}) exited with returncode: {self.vadere.returncode}  log for details: {os.path.abspath(log_file_name)} "
                )
        finally:
            log_file.close()


def get_port(bind):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM, 0)
    sock.bind((bind, 0))
    _, port = sock.getsockname()
    sock.close()
    return port


def wait_for_client(options):

    threads = []
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((options.bind, options.port))
    s.listen(5)
    logging.info(f"vadere launcher listening on port {options.port} ...")

    try:
        while True:
            conn, addr = s.accept()
            port = get_port(options.bind)
            logging.info(f"client connected start vadere runner at port {port}...")
            thread = VadereRunner(conn, port, options)
            thread.start()
            threads.append(thread)
    except SystemExit:
        logging.info("received kill signal shutting down...")
    except KeyboardInterrupt:
        logging.info("interrupt shutting down...")
    finally:
        for t in threads:
            if t.isAlive():
                t.stop_tread()
                t.join(2)
                if t.isAlive():
                    print(f"tread not stopping. {t.process_state()}")
        s.close()


def main(args=None):
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-p",
        "--port",
        dest="port",
        default=9998,
        type=int,
        help="listen on port for incoming requests. [default= 9998]",
    )
    parser.add_argument(
        "-b",
        "--bind",
        dest="bind",
        default="127.0.0.1",
        help="bind to address. [default= 127.0.0.1]",
    )
    parser.add_argument(
        "-g",
        "--gui-mode",
        dest="gui",
        default=False,
        action="store_true",
        help="show graphical userinterface if client connects. [default= false]",
    )
    parser.add_argument(
        "--jar",
        dest="jar",
        default="",
        help="JAR file to execute. If not given use default location.",
    )
    parser.add_argument(
        "--vadere-log-level",
        dest="vadere_log_level",
        default="INFO",
        type=str,
        help="Loglevel for Vadere server. [default= False]",
    )
    parser.add_argument(
        "--vadere-log",
        dest="vadere_log",
        default="",
        required=False,
        type=str,
        help="Path log location. Filename is dynamic . [default= no logfile]",
    )
    parser.add_argument(
        "--log",
        dest="log",
        default=logging.INFO,
        choices=list(logging._nameToLevel.keys()),
        help="write log files for each vadere" " instance. [default= INFO]",
    )
    if args is None:
        options = parser.parse_args()
    else:
        options = parser.parse_args(args)

    logging.basicConfig(level=options.log, format="%(levelname)s:%(name)s> %(message)s")

    if options.jar == "":
        vadere_home = os.getenv("VADERE_HOME")
        if vadere_home is None:
            logging.error("JAR file not given and VADERE_HOME not set.")
            sys.exit(-1)
        else:
            options.jar = os.path.join(
                vadere_home, "VadereManager/target/vadere-server.jar"
            )

    wait_for_client(options)


if __name__ == "__main__":
    main()
