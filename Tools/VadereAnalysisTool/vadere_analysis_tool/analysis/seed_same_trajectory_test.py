
from vadere_analysis_tool import ScenarioOutput
from vadere_analysis_tool import VadereProject



class SameSeedTrajectory:
    """ Compare postvis.trajectory files from different runs but with same seed."""

    def __init__(self, project_dir):
        self.project = VadereProject(project_dir)

    def _find_same_scenarios(self):
        
        for k , v in self.project.output_dirs.items():
            v.scenario