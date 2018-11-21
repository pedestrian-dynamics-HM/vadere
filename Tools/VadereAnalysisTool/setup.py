import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="VadereAnalysisTool",
    version="0.0.5",
    author="Stefan Schuhbäck",
    author_email="stefan.schuhbaeck@hm.edu",
    description="Import VadereProject to ease analysis",
    long_description=long_description,
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "Operating System :: OS Independent",
    ],
)
