from __future__ import print_function
import pandas as pd
import numpy as np
import os
from ipywidgets import interact, interactive, fixed, interact_manual
import ipywidgets as widgets
import ipysheet
import colorsys
import xlsxwriter
import csv
from tqdm import tqdm_notebook as tqdm
from datetime import datetime
import matplotlib.pyplot as plt


# groups pedestrian data into time intervalls with a given frequency
# skip_groups : 	the distance between intervalls
# off_set : 		the start of the first intervall
def to_groups(peds, interval, freq, skip_groups, off_set):
    grouped = peds.groupby(pd.Grouper(key='datetime', freq=str(interval) + freq))
    grouped = list(map(lambda x: x[1], list(grouped)))
    #grouped = grouped[off_set::skip_groups]
    return grouped


def to_pedestrians(peds, mapping, group):
    df = peds.loc[peds['timestamp'].isin(group.timestamp.values)]
    pedestrians = mapping.loc[mapping['pedestrianId'].isin(df.pedestrianId.unique())]
    
    return pedestrians

# creates an od matrix of the given pedestrians data (a pandas dataframe)
# axis: 	size of OD-Matrix = number of origins/destinations
# cap : 	optional freshhold value under which values are set to zero 
def create_od_matrix(pedestrians, axis, cap=None):
    total = len(pedestrians.pedestrianId.values)
    
    grouped = pedestrians.groupby(['source','target']).size().reset_index().rename(columns={0:'count'})
    
    od = np.zeros((len(axis), len(axis)))

    for i, s in enumerate(axis):
        for j, t in enumerate(axis):
            row = grouped.loc[(grouped['source'] == s) & (grouped['target'] == t)]
            value = 0
            
            if not row.empty:
                value = row['count'].values[0]
            
            if cap is not None and value < cap:
                value = 0
            
            od[i, j] = value
            
    return od, total

# generates gradient function to be used for the xml output of od-matrices
# note: 	gradient color scale is not good, should be changed to blue,grenn, yellow
def generate_gradient_function(max_val):
    return """function (value) {
            if (value == 0) {
                return {}
            }
            var color1 = {red: 255, green: 0, blue: 0};
            var color2 = {red: 255, green: 255, blue: 0};
            var color3 = {red: 0, green: 255, blue: 0};
        
            var percent = value/100;

            if (percent <= 0.5) {
                start = color1;
                end = color2;
                percent = percent / 0.5
            } else {
                start = color2;
                end = color3;
                percent = (percent - 0.5) / 0.5
            }
        
            start = color1;
            end = color3;
        
            var diffRed = end.red - start.red;
            var diffGreen = end.green - start.green;
            var diffBlue = end.blue - start.blue;

            var g = {
              r: start.red + (diffRed * percent),
              g: start.green + (diffGreen * percent),
              b: start.blue + (diffBlue * percent),
            };
      
            return {
                backgroundColor: 'rgba('+g.r+','+g.g+','+g.b+', 0.2)'
            };
        }
        """

# display od-matrix with interactive widget
# freq : 	seconds (S), minutes (T), hours (H)
# group : 	currently selected od-matrix
# interval: 	time interval over which the od-matrix is generated
# skip group: 	skip a given number of od-matrices
# off_set: 	start generating od-matrices from off set given in frequency (seconds, min, hours)
# cap : 	optional freshhold value under which values are set to zero 
def run(peds, mapping, cap=None, prefix=''):
    
    to_string = np.vectorize(lambda x: str(x))
    
    freq = 'S'
    interval = 10
    group = 0
    skip_groups = 1 # minimum values is 1 = no groups are skipped!
    off_set = 0 
    
    if 'datetime' not in peds.columns:
        peds.insert(1, 'datetime', pd.to_datetime(peds['timestamp'], unit='ms'))
    
    wFreq = widgets.Dropdown(options=[('hour', 'H'), ('min', 'T'), ('seconds', 'S')], value=freq, description='freq')
    wInterval = widgets.IntSlider(min=1, max=60, step=1, value=interval, description='interval', continuous_update=False)
    wGroup = widgets.IntSlider(min=0, max=0, step=1, value=group, description='group', continuous_update=False)
    wTime = widgets.Label(value="text")
    wSaveXML = widgets.Button(description='Save as XML')
    wSaveCSV = widgets.Button(description='Save as csv')
    
    source_target = np.union1d(mapping.source.unique(), mapping.target.unique())
    dim = len(source_target)
    axis = list(to_string(source_target)) + ['sum']
    sheet = ipysheet.sheet(rows=len(axis), columns=len(axis), column_headers=axis, row_headers=axis)
    
    groups = to_groups(peds, interval, freq, skip_groups, off_set)
    wGroup.max = len(groups) - 1
    
    start = groups[group].datetime.iloc[0]
    end = groups[group].datetime.iloc[-1]
    group_count = len(groups[group].pedestrianId.values)
    wTime.value = "{:%d, %b %Y} from {:%I:%M:%S %p} to {:%I:%M:%S %p}".format(start, start, end)
    
    pedestrians = to_pedestrians(peds, mapping, groups[group])
    od, frame_count = create_od_matrix(pedestrians, source_target, cap=cap)
    
    # adds info labels
    wInfo1 = widgets.Label("Number of OD-Matrices with in current hour: {0} ".format(len(groups)))
    wInfo2 = widgets.Label("Current number of pedestrians within OD-Matrix: {0}".format(group_count))
    wInfo3 = widgets.Label("Number of pedestrians within current hour: {0}".format(frame_count))
    
    info_lines = [wInfo1,wInfo2,wInfo3]
    info_box = widgets.VBox(info_lines)
    
    origin = od.sum(axis=0) # sum of every origin
    destination = od.sum(axis=1) # sum of every target
    
    ipysheet.renderer(code=generate_gradient_function(od.max()), name='gradient');
    ipysheet.renderer(code=generate_gradient_function(origin.max()), name='origin');
    ipysheet.renderer(code=generate_gradient_function(destination.max()), name='destination');
    
    ipysheet.cell_range(od, row_start=0, column_start=0, renderer='gradient')
    ipysheet.row(dim, origin, column_end=dim-1, renderer='origin')
    ipysheet.column(dim, destination, row_end=dim-1, renderer='destination')
    
    def f(change):
        nonlocal peds
        nonlocal mapping
        nonlocal freq
        nonlocal interval
        nonlocal group
        nonlocal groups
        nonlocal wGroup
        
        name = change['owner'].description
        value = change['new']
        
        regroup = False
        if name is 'group':
            group = value
        elif name is 'freq':
            freq = value
            regroup = True
        elif name is 'interval':
            interval = value
            regroup = True
        if regroup:
            groups = to_groups(peds, interval, freq, skip_groups, off_set)
            wGroup.max = len(groups) - 1
        
        
        pedestrians = to_pedestrians(peds, mapping, groups[group])
        group_count = len(groups[group].pedestrianId.values)
        od, frame_count = create_od_matrix(pedestrians, source_target, cap=cap)
        
        origin = od.sum(axis=0)
        destination = od.sum(axis=1)
    
        ipysheet.renderer(code=generate_gradient_function(od.max()), name='gradient');
        ipysheet.renderer(code=generate_gradient_function(origin.max()), name='origin');
        ipysheet.renderer(code=generate_gradient_function(destination.max()), name='destination');
    
        ipysheet.cell_range(od, row_start=0, column_start=0, renderer='gradient')
        ipysheet.row(dim, origin, column_end=dim-1, renderer='origin')
        ipysheet.column(dim, destination, row_end=dim-1, renderer='destination')
        
        start = groups[group].datetime.iloc[0]
        end = groups[group].datetime.iloc[-1]
        wTime.value = "{:%d, %b %Y} from {:%I:%M:%S %p} to {:%I:%M:%S %p} test".format(start, start, end)
        # adds info label
        info_box.children[0].value = "Number of OD-Matrices with in current hour: {0}".format(len(groups))
        info_box.children[1].value = "Current number of pedestrians within OD-Matrix: {0}".format(group_count)
        info_box.children[2].value = "Number of pedestrians within current hour: {0}".format(frame_count)
        
        
    wFreq.observe(f, names='value')
    wInterval.observe(f, names='value')
    wGroup.observe(f, names='value')
    
    filename="{0}-od-matrices".format(prefix)
    
    def save_xml(button):
        data = get_od_matrices(groups, peds, mapping, filename, source_target,cap=cap)
        write_xlsx(data, to_string(source_target), filename)
            
    def save_csv(button):
        data = get_od_matrices(groups, peds, mapping, filename, source_target,cap=cap)
        write_csv(data, to_string(source_target), filename)
        
    wSaveXML.on_click(save_xml)
    wSaveCSV.on_click(save_csv)

    return widgets.VBox([wFreq, wInterval, wGroup, wTime, info_box, wSaveXML, wSaveCSV, sheet])

# method to generate od-matrices with from a given set of groups
# groups : 	each group contains all pedestrians within one time interval 
# cap : 	optional freshhold value under which values are set to zero 
def get_od_matrices(groups, peds, mapping, name, source_target, cap=None):
    data = []
    for g in groups:
        start = g.datetime.iloc[0]
        end = g.datetime.iloc[-1]
        # select mapping for pedestrians contained in given hour
        pedestrians = to_pedestrians(peds, mapping, g)
        # generate od matrix
        od, frame_count = create_od_matrix(pedestrians, source_target, cap=cap)
        
        time = "{:%I:%M:%S }_{:%I:%M:%S }".format(start, end)
        name="{0}".format(time)
        data.append((name, len(pedestrians), od))

    return data

# collect groups of pedestrians with in given time interval, freq, skip_groups and off_set
# peds : 	list of pedestrians in the form timeStamp, pedId, x, y, 
# mapping : 	list of pedID, origin, destination
# parameters : 	(freq, interval, skip_groups, off_set)
# write2csv : 	save od-matrices to csv files or not
# cap : 	optional freshhold value under which values are set to zero 
# prefix : 	prefix for the file name of csv file the od-matrices will be saved in 
def collect_groups(peds, mapping, parameters, write2csv=False, cap=None, prefix=''):
    
    to_string = np.vectorize(lambda x: str(x))
    
    freq = parameters[0] # 'H', 'T', 'S'
    interval = parameters[1]
    skip_groups = parameters[2] # min value is 1
    off_set = parameters[3]
    
    if 'datetime' not in peds.columns:
        peds.insert(1, 'datetime', pd.to_datetime(peds['timestamp'], unit='ms'))
    
    # step 1: group pedestrians into freq intervals
    groups = to_groups(peds, interval, freq, skip_groups, off_set)
    origins = mapping.source.unique()
    destinations = mapping.target.unique()
    source_target = np.union1d(origins, destinations)
    name="{0}-od-matrices".format(prefix)
    
    # step 2: get corresponding od-matrix for freq intervals
    od_matrices = get_od_matrices(groups, peds, mapping, name, source_target,cap=cap)

    if write2csv:
        write_csv(data, to_string(source_target), name)
        print("finished saving file ", name)
    
    return groups, od_matrices, to_string(origins), to_string(destinations)
       
# extracts start and end time from string with format %H:%M:%S_%H:%M:%S
def str2datetime_obj(time_str):
    time = time_str.split('_')
    start = datetime.strptime(time[0][:-2], '%H:%M:%S').time()
    end = datetime.strptime(time[1][:-2], '%H:%M:%S').time()
    
    return start,end

# stores od-matrices in data inside csv file 
def write_csv(data, axis, name='od-matrices'):
    
    with open('{0}.csv'.format(name), 'w', newline='\n') as csvfile:
        csvwriter = csv.writer(csvfile, delimiter=',')
        
        for g in data:
            for row in g[2]:
                csvwriter.writerow(row)
            
            csvwriter.writerow('')
    
    print("saved {0}".format(name))
        
# write out displayed od-matrix to xlsx file
def write_xlsx(groups, axis, name='od-matrices'):
    chars = 'ABCDEFGHIJKLMNOP'
    workbook = xlsxwriter.Workbook('{}.xlsx'.format(name))
    number_of_persons = workbook.add_worksheet('# of persons')
    od_matrices = workbook.add_worksheet('od matrices')
    
    format_center = workbook.add_format()
    format_center.set_align('center')
    
    format_col_sum = workbook.add_format()
    format_col_sum.set_align('center')
    format_col_sum.set_left(6)
    
    format_row_sum = workbook.add_format()
    format_row_sum.set_align('center')
    format_row_sum.set_top(6)
    
    format_row_index = workbook.add_format()
    format_row_index.set_align('center')
    format_row_index.set_right(6)
    
    number_of_persons.write_row(2, 2, ['Time', '# pedestrians'], format_center)
    
    # write header and freeze row
    od_matrices.write_row(0, 1, axis, format_center)
    od_matrices.freeze_panes(1, 0)
    
    n = len(axis)
    
    current = 1

    for j, group in tqdm(enumerate(groups)):
        time = group[0]
        persons = group[1]
        od = group[2]
        
        number_of_persons.write_row(j+3, 2, [time, persons], format_center)
        
        # write time            row     col 
        od_matrices.merge_range(current, 0, current, n + 1, time, format_center)
        
        # write row index
        current = current + 1
        od_matrices.write_column(current, 0, axis, format_row_index)
      
        for row in range(n):
            od_matrices.write_row(current, 1, od[row], format_center)
            od_matrices.write_formula(current, n+1, '=SUM({}{}:{}{})'.format(chars[1], current+1, chars[n], current+1), format_col_sum)
            
            current = current + 1

            
        for col in range(n):
            i = col+1
            od_matrices.write_formula(current, i, '=SUM({}{}:{}{})'.format(chars[i], current-n+1, chars[i], current), format_row_sum)
        conditional_format = {
            'type': '3_color_scale',
            'min_type': 'num',
            'min_value': 0,
            'mid_type': 'num',
            'mid_value': 20,
            'max_type': 'num',
            'max_value': 40
        }
        
        # destination sum coloring
        od_matrices.conditional_format('{}{}:{}{}'.format(chars[1], current+1, chars[n], current+1), conditional_format)
        
        # origin sum coloring
        od_matrices.conditional_format('{}{}:{}{}'.format(chars[n+1], current-n + 1, chars[n+1], current), conditional_format)
        
        # distribution coloring
        od_matrices.conditional_format('{}{}:{}{}'.format(chars[1], current-n + 1, chars[n], current), conditional_format)
        
        current = current + 2
        
    workbook.close()
    print('done')
 
# plot od-matrix as pixmap
def plot_od_pixmap(matrix, fig_title, val_precision = 2, color_type = 'viridis', show_values=True, file_name=''):
    fig, ax = plt.subplots()
    ax.set_xticks(np.arange(10))
    ax.set_yticks(np.arange(10))
    od_labels = ['-1','0','1','2','3','4','5','6','7','8']
    ax.set_xticklabels(od_labels)
    ax.set_yticklabels(od_labels)
    
    if show_values:
        for i in range(0,10):
            for j in range(0,10):
                text = ax.text(j, i, np.round(matrix[i,j],val_precision), ha="center", va="center", color="w")

    plt.title(fig_title)
    plt.imshow(matrix, cmap=color_type)
    plt.colorbar()
    
    if file_name:
        plt.savefig('{}.png'.format(file_name),dpi=400)
        print("saved image",file_name)
        
    plt.show()



to_second = lambda x : x.hour*60*60 + x.minute*60 + x.second
# compute matrix norm of given od-matrices (data)
def compute_matrix_norms(data):
    
    norm_vec = []
    for g in data:
        time_stamp = g[0]
        num_ped = g[1]
        od = g[2]
         # L2 norm, forb norm, max (inf) norm
        norm = np.linalg.norm(od,2)
        start,end = str2datetime_obj(g[0])
        start_sec = to_second(start)
        end_sec = to_second(end)
        #norms.append([time_stamp,num_ped,,np.linalg.norm(od,'fro'),np.linalg.norm(od,np.inf)])
        norm_vec.append([end_sec,norm])
    
    return np.array(norm_vec)

# generate bar plot for the given cell_values (cell_values are one cell within od-matrix over a given timeperiod)
def generate_bar_plot(h, time_vec, cell_values, info, show_av = False):
    
    if show_av:
        average = np.average(cell_values)
        plt.hlines(average,0,len(cell_values),linewidth=2,color='r')
        
    bar_x = np.arange(len(cell_values))
    plt.bar(bar_x,cell_values, width=0.8) # 
    plt.title("{0} for hour {1}".format(info[0],h))
    plt.xlabel("time intervalls [1h/10s]")
    plt.ylabel(info[1])
    plt.ylim([0,100])
    plt.xlim([-10, len(cell_values)+10])
    plt.grid()
    #plt.savefig("{0}.png".format(info[2]))
    plt.show()

# generate histogram plot for the given cell_values (cell_values are one cell within od-matrix a given timeperiod)
def generate_hist_plot(h, cell_values, x_max, y_max, info):
    
    non_zero_cell_values = cell_values[cell_values != 0]
    plt.hist(x=non_zero_cell_values,bins=25,alpha=0.7,rwidth=0.85)
    plt.grid(axis='y',alpha=0.75)
    plt.xlabel(info[1])
    plt.ylabel('frequency')
    plt.title("{0} : {1}h".format(info[0],h))
    plt.xlim([0,45])
    plt.ylim([0,100])
    #plt.savefig("{0}.png".format(info[2]))
    plt.show()
    
# convert origin and destination ids to indexes
def od2indexes(origin,target):
    
    i = origin + 1
    j = target + 1
    
    return i,j

# collect the matrix entries of a single cell (origin, destination) from the given od-matrices in data
def collect_matrix_entries(data,origin,destination):
    cell_values = np.zeros((len(data),3))
    i,j = od2indexes(origin, destination)
    k = 0
    for g in data:
        time = str2datetime_obj(g[0])
        start = to_second(time[0])
        end = to_second(time[1])
        cell_values[k,0] = start
        cell_values[k,1] = end
        cell_values[k,2] = g[2][i,j]
        k+=1
        
    return cell_values


    


