import subprocess

for i in [200,300]:
    myCommand = 'python scripts/main.py with scripts/t_junction/sbb.json "number_of_trees={0}" "number_of_cores=6" --force --filestorage ../runs/sbb'.format(i)
    print(myCommand)
    p = subprocess.Popen(myCommand,shell=True)
    p.wait()
