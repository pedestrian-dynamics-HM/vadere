import os
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
from tqdm import tqdm
from multiprocessing import Pool

def processRow(args):
  i, row, path, size = args
  n_targets = 2

  row = np.array(row)
  targets = row[-n_targets:]
  data = row[:-n_targets]
  name = 'density-{0}-{1:.2f}-{2:.2f}.png'.format(i, targets[0], targets[1])
  filepath = os.path.join(path, name)
  
  if os.path.exists(filepath):
      #print("Skipping", filepath, 'since it already exits!')
      return False
      
  image = data.reshape(size)
  fig, ax = plt.subplots()
  ax.imshow(image, interpolation='Nearest')
  ax.axis('off')
  fig.gca().set_axis_off()
  fig.subplots_adjust(top = 1, bottom = 0, right = 1, left = 0, hspace = 0, wspace = 0)
  ax.margins(0,0)
  fig.gca().xaxis.set_major_locator(plt.NullLocator())
  fig.gca().yaxis.set_major_locator(plt.NullLocator())

  fig.savefig(filepath, bbox_inches='tight', pad_inches=0)

  return True


def processFile(file, base_directory, image_directory, size, number_of_cores=1, _filter=lambda x: x):
  path = os.path.join(image_directory, file[:-4])
  try:
    os.mkdir(path)
  except Exception:
    print("Path", path, "already exists")

  for chunk in pd.read_csv(open(os.path.join(base_directory, file), 'r'), sep=';', header=None, chunksize=500):
    chunk = _filter(chunk)
    with Pool(processes=number_of_cores) as p:
        with tqdm(total=len(chunk), desc='process chunk', leave=False) as pbar:
            rows = list(map(lambda r: (*r, path, size), list(chunk.iterrows())))
            for i, result in enumerate(p.imap_unordered(processRow, rows)):
              pbar.update()

  print('Done', file)