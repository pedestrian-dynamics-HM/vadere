![VADERE](vadere.png "VADERE")

---

# VADERE

VADERE is an open source framework for the simulation of microscopic pedestrian dynamics. In addition to this core domain, other systems including cars and granular flow can be introduced into the framework. VADERE provides generic model classes and visualisation and data analysis tools for two-dimensional systems. A series of simulation models are already implemented in the framework that are also the basis for scientific publications. Namely the optimal steps model, the gradient navigation model, and the social force model are available for simulation studies.

This software runs on OS X, Windows, and Linux.

VADERE has been developed by [Prof. Dr. Gerta Köster's](http://www.cs.hm.edu/die_fakultaet/ansprechpartner/professoren/koester/index.de.html)
research group at the [Munich University of Applied Sciences](https://www.hm.edu/) at the
[department for Computer Science and Mathematics](http://cs.hm.edu/).
However, we welcome contributions from external sources.

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

### Build the Application

Run `git clone https://gitlab.lrz.de/vadere/vadere`. Go to the project directory and run `mvn clean package` (or `mvn clean package -DskipTests` if you want to skip the unit tests).

### Run Application

After building the application, you can start VADERE by running `java -jar VadereGui/target/gui-0.1-SNAPSHOT-jar-with-dependencies.jar`.

## Usage Example

With the following steps, you can start a scenario of the [ExampleProject](Documentation/Examples/ExampleProject):

- start VADERE
- *Project* > *Open*
- choose `vadere.project` and click *open*
- select the scenario on the left and press *run selected scenario*


## Development Setup

Follow the installation instructions above. Go to the project directory and run `mvn clean install`. The project can now be imported *As Maven Project* in your IDE.

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

## Contribution

Please see [Contribution guidelines](CONTRIBUTING.md). The document defines guidelines for coding style and commit messages.

## Release History

 - 0.1 - initial release of the software as open source (2016-08-05)


## Contributors

People who have contributed code to the project at the Munich University of Applied Sciences (in alphabetical order):

Florian Albrecht, Benjamin Degenhart, Felix Dietrich, Benedikt Kleinmeier, Jakob Sch&ouml;ttl, Michael Seitz, Swen Stemmer, Isabella von Sivers, Mario Teixeira Parente, Benedikt Z&ouml;nnchen


## License

This software is licensed under the GNU Lesser General Public License ([LGPL](LICENSE)).
More information: http://www.gnu.org/licenses/lgpl.html



