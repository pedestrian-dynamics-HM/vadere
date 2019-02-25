import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

with open('requirements.txt', "r") as f:
    requirements = f.read().splitlines()

setuptools.setup(
    name="vadereanalysistool",
    version="0.1.0",
    author="Stefan Schuhbäck",
    author_email="stefan.schuhbaeck@hm.edu",
    description="Import VadereProject to ease analysis",
    long_description=long_description,
    packages=setuptools.find_packages(),
    install_requires=requirements,
    classifiers=[
        "Programming Language :: Python :: 3",
        "Operating System :: OS Independent",
    ],
)
