import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="vadere_analysis_tool",
    version="0.0.1",
    author="Stefan Schuhbäck",
    author_email="stefan.schuhbaeck@hm.edu",
    description="Import VadereProject to ease analysis",
    long_description=long_description,
    long_description_content_type="text/markdown",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "Operating System :: OS Independent",
    ],
)