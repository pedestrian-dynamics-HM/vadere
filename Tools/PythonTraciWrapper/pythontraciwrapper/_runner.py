import logging
import os
import signal
import subprocess
import threading
import time


class Runner(threading.Thread):
    def __init__(self, command, thread_name, log_location=None, use_stdout=False):
        threading.Thread.__init__(self)
        self.command = command
        self.log_location = log_location
        self.use_stdout = use_stdout
        self.thread_name = thread_name
        self.log = logging.getLogger()

    def stop(self):
        self._cleanup()

    def run(self):
        try:
            if self.log_location is None:
                self.log_location = os.devnull

            log_file = open(self.log_location, "w")
            self.process = subprocess.Popen(
                self.command,
                cwd=os.path.curdir,
                shell=False,
                stdin=None,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
            )
            for line in self.process.stdout:
                if self.use_stdout:
                    print(f"{self.thread_name}> {line.decode('utf-8')}", end="")
                log_file.write(line.decode("utf-8"))

            if self.process.returncode is not None:
                self._cleanup()

        finally:
            log_file.close()
            print(
                f"{self.thread_name}> subprocess returncode={self.process.returncode}"
            )

    def _cleanup(self):
        try:
            os.kill(self.process.pid, signal.SIGTERM)
            self.process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            self.log.error("subprocess not stopping. Send SIGKILL")

        if self.process.returncode is None:
            os.kill(self.process.pid, signal.SIGKILL)
            time.sleep(0.5)
            if self.process.returncode is None:
                self.log.error("subprocess still not dead")
                raise
