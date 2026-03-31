 # Setup for python functionality

## Installation of conda

To bootstrap a minimal distribution, use a minimal installer such as [Miniconda](https://docs.anaconda.com/free/miniconda/) or [Miniforge](https://conda-forge.org/download/).

Conda is also included in the [Anaconda Distribution](https://repo.anaconda.com).

## Create conda environment

Create a new conda environment with python and activate it: 

```bash
conda create -n CONDA_ENV python=3.11
conda activate CONDA_ENV
```

Install required packages: 

```bash
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
```

## Usage of conda environment for airflow calculation

Specify [AttributesAirFlowModel](../../../../../../../../VadereState/src/org/vadere/state/attributes/models/airflow/AttributesAirFlowModel.java) with the right installation path of conda and the name of the created environment.


