![Vadere](vadere.png "Vadere")

---

# Vadere

Vadere is an open source framework for the simulation of microscopic pedestrian dynamics. In addition to this core domain, other systems including cars and granular flow can be introduced into the framework. Vadere provides generic model classes and visualisation and data analysis tools for two-dimensional systems. A series of simulation models are already implemented in the framework that are also the basis for scientific publications. Namely the optimal steps model, the gradient navigation model, and the social force model are available for simulation studies.

This software runs on OS X, Windows, and Linux. 

Vadere has been developed at the Munich University of Applied Sciences at the department for Computer Science and Mathematics. However, we welcome contributions from external sources.

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

### Run the Application

1. Get the Source: Run `git clone https://gitlab.lrz.de/vadere/vadere.git`.
2. Build the Application: Go to the project directory and run `mvn clean package` (or `mvn clean package -DskipTests` if you want to skip the unit tests).
3. Start the Application: After building the application, you can start Vadere by running `java -jar VadereGui/target/gui-0.1-SNAPSHOT-jar-with-dependencies.jar`.

## Usage Example 

With the following steps, you can start a scenario of the [ExampleProject](Documentation/Examples/ExampleProject):

- start Vadere 
- *Project* > *Open* 
- choose `vadere.project` and click *open*
- select the scenario on the left and press *run selected scenario*


## Development Setup

Follow the **installation instructions** above i.e. install all required software and get the source. Go to the project directory and run `mvn clean install`. The project can now be imported *As Maven Project* in your IDE.

### Eclipse

- *File* > *Import* > *Maven* > *Existing Maven Projects*
- choose `pom.xml` as *Root Directory* and click *Finish*
- open *VadereGui* > *src* > *projectview* > `VadereApplication`

### IntelliJ IDEA

- on the welcome-screen select *Import Project* 
- select `pom.xml` > *Next* > *Next* > *Next* > *Finish*
- open *VadereGui* > *src* > *projectview* > `VadereApplication`
- click the *run*-icon next to the `main` method

Alternatively, run `mvn eclipse:eclipse` using the [Maven Eclipse Plugin](http://maven.apache.org/plugins/maven-eclipse-plugin/usage.html) or `mvn idea:idea` using the [Maven IntelliJ Plugin](http://maven.apache.org/plugins/maven-idea-plugin/).

### Git Hooks

Follow the instructions in [Version Control HowTo](Documentation/version-control/HOWTO.txt).

## Contribution

Please see [Contribution guidelines](CONTRIBUTION.md). The document defines guidelines for coding style and commit messages.

## Release History

0.1	initial release of the software as open source


## Contributors

People who have contributed code to the project at the Munich University of Applied Sciences (in alphabetical order):

Florian Albrecht, Benjamin Degenhart, Felix Dietrich, Benedikt Kleinmeier, Jakob Sch&ouml;ttl, Michael Seitz, Swen Stemmer, Isabella von Sivers, Mario Teixeira Parente, Benedikt Z&ouml;nnchen


## License

This software is licensed under the GNU Lesser General Public License ([LGPL](LICENSE)).
For more information: http://www.gnu.org/licenses/lgpl.html



