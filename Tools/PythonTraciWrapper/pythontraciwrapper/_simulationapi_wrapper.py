from typing import List

from py4j.java_gateway import java_import

from ._api_wrapper import ApiWrapper

class SimulationapiWrapper(ApiWrapper):

    def __init__(self, apiObject, gateway):
        super(SimulationapiWrapper, self).__init__(apiObject, gateway)

        # imports
        java_import(self._gateway.jvm, "org.vadere.manager.traci.compoundobjects.*")
        java_import(self._gateway.jvm, "java.util.*")

    def createWaitingArea(self, elementID: str, startTime: float, endTime: float, repeat: int,
                          waitTimeBetweenRepetition: float, time: float, polyCorners: List[str]):
        polyCornersJavaArrayList = self._gateway.jvm.ArrayList()
        for x in polyCorners:
            polyCornersJavaArrayList.add(x)
        compoundObjectBuilder = self._gateway.jvm.CompoundObjectBuilder()
        waitingAreaData = compoundObjectBuilder.createWaitingArea(elementID, startTime, endTime, repeat,
                                                                  waitTimeBetweenRepetition, time, polyCornersJavaArrayList)
        response = self._apiObject.createWaitingArea(elementID, waitingAreaData)
        result = response.toString()
        return result

    def createTargetChanger(self, elementID: str, reachDist: float, nextTargetIsPedestrian: int, nextTarget: str,
                            prob: float, polyCorners: List[str]):
        polyCornersJavaArrayList = self._gateway.jvm.ArrayList()
        for x in polyCorners:
            polyCornersJavaArrayList.add(x)
        compoundObjectBuilder = self._gateway.jvm.CompoundObjectBuilder()
        targetChangerData = compoundObjectBuilder.createTargetChanger(elementID, polyCornersJavaArrayList, reachDist,
                                                                      nextTargetIsPedestrian, nextTarget, prob)
        response = self._apiObject.createTargetChanger(elementID, targetChangerData)
        result = response.toString()
        return result