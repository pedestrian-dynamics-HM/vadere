import json


class VadereScenario:
    def __init__(self, path):
        with open(path, "r") as f:
            data = json.load(f)
        self.data = data
        self.path = path

    @classmethod
    def load(cls, path):
        s = cls(path)
        return s

    @property
    def name(self):
        return self.data["name"]

    @name.setter
    def name(self, val: str):
        self.data["name"] = str(val)

    @property
    def description(self):
        return self.data["description"]

    @description.setter
    def description(self, val: str):
        self.data["description"] = str(val)

    @property
    def release(self):
        return self.data["release"]

    @release.setter
    def release(self, val: str):
        self.data["release"] = str(val)

    @property
    def process_writers(self):
        return self.data["processWriters"]

    @process_writers.setter
    def process_writers(self, val: dict):
        if type(val) is not dict:
            raise ValueError("process_writer must be dict")
        self.data["processWriters"] = val

    @property
    def scenario(self):
        return self.data["scenario"]

    @scenario.setter
    def scenario(self, val: dict):
        if type(val) is not dict:
            raise ValueError("scenario must be dict")
        self.data["scenario"] = val

    @property
    def topography(self):
        return self.data["scenario"]["topography"]

    @topography.setter
    def topography(self, val: dict):
        if type(val) is not dict:
            raise ValueError("topography must be dict")
        self.data["scenario"]["topography"] = val

    @property
    def obstacles(self):
        return self.data["scenario"]["topography"]["obstacles"]

    @obstacles.setter
    def obstacles(self, val: list):
        if type(val) is not list:
            raise ValueError("obstacles must be list of dicts")
        self.data["scenario"]["topography"]["obstacles"] = val

    @property
    def measurement_areas(self):
        return self.data["scenario"]["topography"]["measurementAreas"]

    @measurement_areas.setter
    def measurement_areas(self, val: list):
        if type(val) is not list:
            raise ValueError("measurementAreas must be list of dicts")
        self.data["scenario"]["topography"]["measurementAreas"] = val

    @property
    def stairs(self):
        return self.data["scenario"]["topography"]["stairs"]

    @stairs.setter
    def stairs(self, val: list):
        if type(val) is not list:
            raise ValueError("stairs must be list of dicts")
        self.data["scenario"]["topography"]["stairs"] = val

    @property
    def targets(self):
        return self.data["scenario"]["topography"]["targets"]

    @targets.setter
    def targets(self, val: list):
        if type(val) is not list:
            raise ValueError("targets must be list of dicts")
        self.data["scenario"]["topography"]["targets"] = val

    @property
    def absorbing_areas(self):
        return self.data["scenario"]["topography"]["absorbingAreas"]

    @absorbing_areas.setter
    def absorbing_areas(self, val: list):
        if type(val) is not list:
            raise ValueError("absorbingAreas must be list of dicts")
        self.data["scenario"]["topography"]["absorbingAreas"] = val

    @property
    def sources(self):
        return self.data["scenario"]["topography"]["sources"]

    @sources.setter
    def sources(self, val: list):
        if type(val) is not list:
            raise ValueError("sources must be list of dicts")
        self.data["scenario"]["topography"]["sources"] = val

    @property
    def raw(self):
        return self.data

    @property
    def scenario(self):
        return self.data["scenario"]

    @property
    def topography(self):
        return self.scenario["topography"]


if __name__ == "__main__":
    s = VadereScenario.load("./mf_small_2.scenario")
