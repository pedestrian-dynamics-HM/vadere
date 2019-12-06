from py4j.java_gateway import java_import

from ._api_wrapper import ApiWrapper


class ControllWrapper(ApiWrapper):

    def sendFile(self, scenarioPath):
        print(self._apiObject.sendFile(scenarioPath))

    def getVersion(self):
        print(self._apiObject.getVersion())

    def nextStep(self, *args):
        print(self._apiObject.nextSimTimeStep(*args))

    def close(self):
        print(self._apiObject.close())