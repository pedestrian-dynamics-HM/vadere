from ._api_wrapper import ApiWrapper


class VadereapiWrapper(ApiWrapper):

    def addStimulusInfos(self, jsonFilePath):
        response = self._apiObject.addStimulusInfos(jsonFilePath)
        result = response.toString()
        return result

    def getAllStimulusInfos(self):
        response = self._apiObject.getAllStimulusInfos()
        result = response.getResponseData()
        return result

    def createTargetChanger(self, jsonFilePath):
        response = self._apiObject.createTargetChanger(jsonFilePath)
        result = response.toString()
        return result

    def removeTargetChanger(self, elementID: str):
        response = self._apiObject.removeTargetChanger(elementID)
        result = response.toString()
        return result
