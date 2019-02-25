import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from vadereanalysistool import ScenarioOutput


class OriginDeviation:
    """ Calculate deviation of origin after transforming it form 0 to X.
        In output_pair a array like structure with output_pair[0] (no translation)
        and output_pair[1] (with translation) is expected.
    """

    def __init__(self, output_pair, name='', max_diff_ok=1e-7, max_diff_warn=1e-4):
        self.output_pair = output_pair
        self.max_diff_ok= max_diff_ok
        self.max_diff_warn = max_diff_warn
        self.df = pd.DataFrame()
        self.name = name
        self.get_origin_deviation_data()

    def get_origin_deviation_data(self):
        # Create ScenarioOutput object
        out_0_origin = ScenarioOutput(self.output_pair[0])
        out_offset_origin = ScenarioOutput(self.output_pair[1])
        if self.name == '':
            self.name = out_0_origin.output_dir_name + "---" + out_offset_origin.output_dir_name

        # Load trajectories and offset
        trajectory_0_orign = out_0_origin.files['postvis.trajectories']()
        trajectory_offset_orign = out_offset_origin.files['postvis.trajectories']()
        offset = out_offset_origin.get_bound_offset()

        # Merge data frames and calculate differences after reverting offset transformation
        df = pd.merge(trajectory_0_orign, trajectory_offset_orign, on=['timeStep', 'pedestrianId'],
                      suffixes=['_0', '_offset'])
        df['x_trans'] = np.abs(df['x_offset'] - offset[0])
        df['y_trans'] = np.abs(df['y_offset'] - offset[1])

        df['x_diff'] = np.abs(df['x_0'] - (df['x_offset'] - offset[0]))
        df['y_diff'] = np.abs(df['y_0'] - (df['y_offset'] - offset[1]))

        df['diff'] = np.abs(np.sqrt(df.x_0 ** 2 + df.y_0 ** 2) - (np.sqrt(df.x_trans ** 2 + df.y_trans ** 2)))

        # find maximal difference for each pedestrian in postvis.trajectories.
        # df_max_diff_per_pedId contains one row per pedestrian
        df_max_diff = df.loc[df.groupby('pedestrianId')['diff'].idxmax(),]

        df_start_diff = df.loc[df.groupby('pedestrianId')['timeStep'].idxmin(),]

        return df, df_max_diff, df_start_diff

    def get_origin_deviation_result(self):

        _, df_max_diff, df_start_diff = self.get_origin_deviation_data()

        ped_id_warn = df_max_diff[(df_max_diff['diff'] > self.max_diff_ok) & (df_max_diff['diff'] <= self.max_diff_warn)].index.values
        ped_id_err = df_max_diff[(df_max_diff['diff'] > self.max_diff_warn)].index.values
        ped_id_ok_ = df_max_diff[(df_max_diff['diff'] <= self.max_diff_ok)].index.values

        ret = {'err_count': len(ped_id_err), 'warn_count': len(ped_id_warn), 'ok_count': len(ped_id_ok_),
               'err_dict': df_max_diff.loc[ped_id_err].to_dict('index'),
               'warn_dict': df_max_diff.loc[ped_id_warn].to_dict('index'),
               'stats_at_max_diff': df_max_diff['diff'].describe(), 'stats_at_start': df_start_diff['diff'].describe(),
               'output_name': self.name, 'df_max_diff': df_max_diff, 'df_start_diff': df_start_diff
               }

        return ret

    def get_figure(self, results):
        width = 15
        height = 4 * len(results)
        fig, axes = plt.subplots(ncols=2, nrows=len(results), figsize=[width, height])

        i = 0
        for result in results:
            self._add_axis(axes[i, 0], result['df_max_diff'], 'pedestrianId', 'diff',
                     title="MaxDiff " + result['output_name'],
                     yscale='log')
            self._add_axis(axes[i, 1], result['df_start_diff'], 'pedestrianId', 'diff',
                     title="StartDiff " + result['output_name'],
                     yscale='log')
            i += 1

        return fig

    @staticmethod
    def _add_axis(ax, data, x_data_label, y_data_label, yscale='linear', title='', x_text_label='', y_text_label=''):
        ax.plot(x_data_label, y_data_label, data=data, linestyle='', marker='.')
        ax.set_yscale(yscale)
        ax.set_title(title)
        ax.set_xlabel(x_text_label)
        ax.set_ylabel(y_text_label)
        return ax
