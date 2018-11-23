import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from vadere_analysis_tool import ScenarioOutput
from vadere_analysis_tool import VadereProject
import os


class OriginDeviation:
    """ Calculate deviation of origin after transforming it form 0 to X.
        In output_pair a array like structure with output_pair[0] (no translation)
        and output_pair[1] (with translation) is expected.
    """

    def __init__(self, output_pair_paths, name='', max_diff_ok=1e-7, max_diff_warn=1e-4):
        self.output_pair_paths = output_pair_paths
        self.max_diff_ok= max_diff_ok
        self.max_diff_warn = max_diff_warn
        self.df = pd.DataFrame()
        self.name = name
        self.get_origin_deviation_data()

    def get_origin_deviation_data(self):
        # Create ScenarioOutput object
        out_0_origin = ScenarioOutput(self.output_pair_paths[0])
        out_offset_origin = ScenarioOutput(self.output_pair_paths[1])
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

    def get_figure(self, f_max_diff_ax=None, f_start_diff_ax=None, **kwargs):
        """
        Create one figure for the current pair. On the left the maximal difference is shown. On the right,
        the maximal difference on the start position.

        :param f_max_diff_ax:       function to create max_diff axes. If not given default scatter plot is used.
                                    This function must provide consumes (ax, data, x_data_label, y_data_label, **kwargs)
        :param f_start_diff_ax:     function to create max_diff axes. If not given default scatter plot is used.
                                    This function must provide consumes (ax, data, x_data_label, y_data_label, **kwargs)
        :param kwargs:              arguments passed to axes creation. All attributes allowed for axes object are
                                    possible. If the attribute name is prefixed with 'l__' or 'r__' the attribute
                                    is only applied to the *left* or *right* plot.
        :return:                    figure object containing the axes.
        """
        if f_max_diff_ax is None:
            f_max_diff_ax = self._add_axis

        if f_start_diff_ax is None:
            f_start_diff_ax = self._add_axis

        kwargs_max, kwargs_start = self._filter_figure_kwargs("l__", "r__", **kwargs)

        result = self.get_origin_deviation_result()
        fig, axes = plt.subplots(nrows=1, ncols=2, figsize=[15, 4])

        kwargs_max = self._add_kwargs(kwargs_max,
                                      title="MaxDiff " + result['output_name'],
                                      yscale='log', linestyle='', marker='.')
        self._add_axis(axes[0], result['df_max_diff'], 'pedestrianId', 'diff' , **kwargs_max)

        kwargs_start = self._add_kwargs(kwargs_max,
                                      title="StartDiff " + result['output_name'],
                                      yscale='log', linestyle='', marker='.')
        self._add_axis(axes[1], result['df_start_diff'], 'pedestrianId', 'diff', **kwargs_start)

        # return fig

    @staticmethod
    def create_origin_deviation_pairs_from_project(project=None, project_path=None):
        """
        Create OriginDeviation objects which represent two runs one with offset (0,0) and the other with
        offset (~500000, ~500000). This function will use all output files within a vadere project.
        The function assumes that all output dir names contain the substrings 'without_offset' or
        'with_offset'. Based on the name preceding this substring the pairs will be formed. This
        function assumes each pair only exists once.

        :param project:         VadereProject object
        :param project_path:    Path to VadereProject
        :return:                List of OriginDeviation objects for the given project.
        """
        if project is not None:
            project = project
        elif project_path is not None:
            print(project_path)
            project = VadereProject(project_path)
            print(type(project))
        else:
            raise ValueError("at least one of project or project_path must be given")

        _output_pairs = []

        output_without_offset = [d for d in project.output_dirs.keys() if d.endswith("without_offset")]
        for dir_without in output_without_offset:
            dir_with_offset = dir_without.replace("without_offset", "with_offset")
            name = dir_with_offset[:-len("without_offset")]
            pair = [os.path.join(project.output_path, dir_without), os.path.join(project.output_path, dir_with_offset)]
            _output_pairs.append(OriginDeviation(pair, name))

        return _output_pairs

    @staticmethod
    def get_summary_figure(results, f_max_diff_ax=None, f_start_diff_ax=None, **kwargs):
        """
        Create one figure with one row for each pair. On the left the maximal difference is shown. On the right,
        the maximal difference on the start position.

        :param results:             list of result objects.
        :param f_max_diff_ax:       function to create max_diff axes. If not given default scatter plot is used.
                                    This function must provide consumes (ax, data, x_data_label, y_data_label, **kwargs)
        :param f_start_diff_ax:     function to create max_diff axes. If not given default scatter plot is used.
                                    This function must provide consumes (ax, data, x_data_label, y_data_label, **kwargs)
        :param kwargs:              arguments passed to axes creation. All attributes allowed for axes object are
                                    possible. If the attribute name is prefixed with 'l__' or 'r__' the attribute
                                    is only applied to the *left* or *right* plot.
        :return:                    figure object containing the axes.
        """
        if f_max_diff_ax is None:
            f_max_diff_ax = OriginDeviation._add_axis

        if f_start_diff_ax is None:
            f_start_diff_ax = OriginDeviation._add_axis

        kwargs_max, kwargs_start = OriginDeviation._filter_figure_kwargs("l__", "r__", **kwargs)

        nrows = 1 * len(results)
        ncols = 2
        plot_number = 0

        fig = plt.figure(figsize=[15, 4 * len(results)])

        for result in results:
            plot_number += 1
            ax = fig.add_subplot(nrows, ncols, plot_number)
            kwargs_max = OriginDeviation._add_kwargs(kwargs_max,
                                          title="MaxDiff " + result['output_name'],
                                          yscale='log', linestyle='', marker='.')
            f_max_diff_ax(ax, result['df_max_diff'], 'pedestrianId', 'diff', **kwargs_max)

            plot_number += 1
            kwargs_start = OriginDeviation._add_kwargs(kwargs_max,
                                            title="StartDiff " + result['output_name'],
                                            yscale='log', linestyle='', marker='.')
            ax = fig.add_subplot(nrows, ncols, plot_number)
            f_start_diff_ax(ax, result['df_start_diff'], 'pedestrianId', 'diff', **kwargs_start)

    @staticmethod
    def _add_kwargs(args_dict, **vars):
        """
        Add all key-value pairs of vars to args_dict, if the key is not present in args_dict already.

        :param args_dict: dictionary with items
        :param vars: additional items.
        :return: union of args_dict and vars. If args_dict already contains a key, the value is *NOT* overwritten.
        """
        for k, v in vars.items():
            args_dict.setdefault(k, v)
        return args_dict

    @staticmethod
    def _filter_figure_kwargs(left, right, **kwargs):
        """
        Filter kwargs based on some prefixes *left* and *right*. If none of the two prefixes is given,
        the argument is added to both kwargs outputs. The prefix will be removed in the filter process.

        :param left:    prefix to identify arguments for left plot
        :param right:   prefix to identify arguments for right plot
        :param kwargs:  all kwargs. This is filtered
        :return: list of kwargs filtered for left and right plot [left_kwargs, right_kwargs]
        """
        kwargs_max = dict()
        kwargs_start = dict()
        for k, v in kwargs.items():
            if k.startswith(left):
                kwargs_max[k[3:]] = v
            elif k.startswith(right):
                kwargs_start[k[3:]] = v
            else:
                kwargs_max[k] = v
                kwargs_start[k] = v

        return kwargs_max, kwargs_start

    @staticmethod
    def _add_axis(ax, data, x_data_label, y_data_label, **kwargs):
        """
        Create scatter plot.

        :param ax:              axes object to use for plot
        :param data:            data to plot
        :param x_data_label:    string used to select columns from data to plot
        :param y_data_label:    string used to select columns from data to plot
        :param kwargs:          any key-value pair which is first tested if it is a valid ax-attribute.
                                If yes than the value is set for this ax-attribute
        :return:                axes object which was just build.
        """
        linestyle = kwargs.setdefault('linestyle', '')
        marker = kwargs.setdefault('marker', '.')
        ax.plot(x_data_label, y_data_label, data=data, linestyle=linestyle, marker=marker)
        for k, v in kwargs.items():
            if hasattr(ax, 'set_' + k):
                f_attr = getattr(ax, 'set_' + k)
                f_attr(v)

        return ax
