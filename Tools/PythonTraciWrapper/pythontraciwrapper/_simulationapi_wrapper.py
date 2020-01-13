from ._api_wrapper import ApiWrapper


class SimulationapiWrapper(ApiWrapper):

    def getSimTime(self):
        response = self._apiObject.getTime()
        result = response.getResponseData()
        return result
