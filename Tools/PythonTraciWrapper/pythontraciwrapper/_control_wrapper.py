from ._api_wrapper import ApiWrapper


class ControlWrapper(ApiWrapper):
    def sendFile(self, scenarioPath):
        return self._apiObject.sendFile(scenarioPath)

    def getVersion(self):
        return self._apiObject.getVersion()

    def nextStep(self, simTimeStep):
        return self._apiObject.nextSimTimeStep(str(simTimeStep))

    def close(self):
        """
        Clean closing of server and entry-point thread
        :return: TraCI Return Code
        """
        ret = self._apiObject.close()
        self._client.close_threads()
        return ret
