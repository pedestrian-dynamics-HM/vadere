![VADERE](vadere.png "VADERE")

---

# VADERE

VADERE is an open source framework for the simulation of microscopic pedestrian dynamics. In addition to this core domain, other systems including cars and granular flow can be introduced into the framework. VADERE provides generic model classes and visualisation and data analysis tools for two-dimensional systems. A series of simulation models are already implemented in the framework that are also the basis for scientific publications. Namely the optimal steps model, the gradient navigation model, and the social force model are available for simulation studies.

This software runs on OS X, Windows, and Linux.

VADERE has been developed by [Prof. Dr. Gerta Köster's](http://www.cs.hm.edu/die_fakultaet/ansprechpartner/professoren/koester/index.de.html)
research group at the [Munich University of Applied Sciences](https://www.hm.edu/) at the
[department for Computer Science and Mathematics](http://cs.hm.edu/).
However, we welcome contributions from external sources.

## Pipeline Status

| Branch  | Pipeline Status  | Coverage |
|:--------|:----------------:|:--------:| 
| master  | [![pipeline status (master)](https://gitlab.lrz.de/vadere/vadere/badges/master/pipeline.svg)](https://gitlab.lrz.de/vadere/vadere/commits/master) | [![coverage report (master)](https://gitlab.lrz.de/vadere/vadere/badges/master/coverage.svg)](https://gitlab.lrz.de/vadere/vadere/commits/master) |
| stable | [![pipeline status (stable)](https://gitlab.lrz.de/vadere/vadere/badges/stable/pipeline.svg)](https://gitlab.lrz.de/vadere/vadere/commits/stable) | [![coverage report (stable)](https://gitlab.lrz.de/vadere/vadere/badges/stable/coverage.svg)](https://gitlab.lrz.de/vadere/vadere/commits/stable) |

## Installation

### Dependencies

* Java 8
* Maven 3.0
* Git
* OpenCL

### Install OpenCL

For this project, GPGPU with OpenCL is used to speed up some computations. Therefore, the following requirements have to be met:

* the latest drivers for your OpenCL device(s)
* an OpenCL SDK

#### Operating Systems

* Windows: For further information using OpenCL on Windows [click here](https://streamcomputing.eu/blog/2015-03-16/how-to-install-opencl-on-windows/).
* OS X: OpenCL is pre-installed for OS X.
* Linux: Please refer to the installation manual of your Linux distribution. 
  * [Sources: OpenCL HowTo](https://wiki.tiker.net/OpenCLHowTo)
  * [Intel Driverpack (only driver needed)](https://software.intel.com/en-us/articles/opencl-drivers#latest_linux_driver)

### Run the Application

1. Get the Source: Run `git clone https://gitlab.lrz.de/vadere/vadere.git`.
2. Build the Application: Go to the project directory and run `mvn clean package` (or `mvn clean package -Dmaven.test.skip` if you want to skip the unit tests). This will build `vadere.jar`and `postvis.jar`. 
3. Start the Application: After building the application, you can start Vadere by running `java -jar VadereGui/target/vadere.jar`.
4. (If you only want to use the Postvisualization-Tool you can do so by running `java -jar VadereGui/target/postvis.jar`).

## Usage Example

With the following steps, you can start a scenario of one of the model test projects in [VadereModelTests](VadereModelTests):

- start Vadere 
- *Project* > *Open* 
- choose `vadere.project` of one of the projects e.g. [TestOSM](VadereModelTests/TestOSM) and click *open*
- select the scenario on the left and press *run selected scenario*


## Development Setup

Follow the **installation instructions** above i.e. install all required software and get the source. Go to the project directory and run `mvn clean install`. The project can now be imported *As Maven Project* in your IDE.

### Eclipse

- *File* > *Import* > *Maven* > *Existing Maven Projects*
- choose `pom.xml` as *Root Directory* and click *Finish*
- open *Vaderegui (gui)* > *src* > *org.vadere.gui.projectview* > `Vadereapplication`

### IntelliJ IDEA

- on the welcome-screen select *Import Project*
- select `pom.xml` > *Next* > *Next* > *Next* > *Finish*
- open *VadereGui (gui)* > *src* > *org.vadere.gui.projectview* > `VadereApplication`
- click the *run*-icon next to the `main` method

Alternatively, run `mvn eclipse:eclipse` using the [Maven Eclipse Plugin](http://maven.apache.org/plugins/maven-eclipse-plugin/usage.html) or `mvn idea:idea` using the [Maven IntelliJ Plugin](http://maven.apache.org/plugins/maven-idea-plugin/).

### Git Hooks

Since it is important to reproduce simulation results, we have the guidline that each output file has to provide its commit-hash. This commit-hash identifies
the state the software was in when the output file was generated. Therefore, git hooks save the commit-hash in the **current_commit_hash.txt** which
will be created in the [VadereSimulator/resource](vadere/VadereSimulator/resource) directory whenever a commit is made or the developer
switches to another commit. If the file is missing or there is no commit-hash in it, you will be warned in the log. 
We strongly suggest that you install these git hooks on your local machine:

1. Copy files [post-checkout](Documentation/version-control/post-checkout), [post-merge](Documentation/version-control/post-merge), 
[post-applypatch](Documentation/version-control/post-applypatch) and [post-commit](Documentation/version-control/post-commit) 
from [version-control](Documentation/version-control) to your local **.git/hooks/** directory.
These files start the script [git-hook-vadere-software](Documentation/version-control/git-hook-vadere-software).
2. Make sure that [git-hook-vadere-software](Documentation/version-control/git-hook-vadere-software) is executable.

To create the **current_commit_hash.txt** without changes to the current commit, you can choose *Switch/Checkout...* on the Repository folder or you 
switch to another branch and switch back again using the command line or any other tool you prefer.

## Contribution

Please see [Contribution guidelines](CONTRIBUTING.md). The document defines guidelines for coding style and commit messages.

## Contributors

People who have contributed code to the project at the Munich University of Applied Sciences (in alphabetical order):

Florian Albrecht, Benjamin Degenhart, Felix Dietrich, Marion Gödel, Benedikt Kleinmeier, Daniel Lehmberg, Jakob Schöttl, Stefan Schuhbäck, Michael Seitz, Swen Stemmer, Isabella von Sivers, Mario Teixeira Parente, Peter Zarnitz, Benedikt Zönnchen

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes.

## License

This software is licensed under the GNU Lesser General Public License ([LGPL](LICENSE)).

For more information: http://www.gnu.org/licenses/lgpl.html
