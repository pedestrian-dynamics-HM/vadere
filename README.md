![Vadere Logo](vadere.png "Vadere Logo")

---

# Vadere

Vadere is an open source framework for the simulation of microscopic pedestrian dynamics. Vadere provides generic model classes and visualisation and data analysis tools for two-dimensional systems. A series of pedestrian locomotion models are already implemented in the framework that are also the basis for scientific publications. Namely the optimal steps model, the gradient navigation model, and the social force model are available for simulation studies. In addition to pedestrians, other systems including cars and granular flow can be introduced into the framework.

This software runs on Windows, OS X and Linux.

Vadere has been developed by [Prof. Dr. Gerta Köster's](http://www.cs.hm.edu/die_fakultaet/ansprechpartner/professoren/koester/index.de.html)
research group at the [Munich University of Applied Sciences](https://www.hm.edu/) at the
[department for Computer Science and Mathematics](http://cs.hm.edu/).
However, we welcome contributions from external sources. 

The Vadere framework includes a mesh generator for unstructured high-quality 2D meshes called **EikMesh** which is described [here](https://gitlab.lrz.de/vadere/vadere/wikis/eikmesh). 

## Pipeline Status

| Branch  | Pipeline Status  | Coverage |
|:--------|:----------------:|:--------:| 
| master  | [![pipeline status (master)](https://gitlab.lrz.de/vadere/vadere/badges/master/pipeline.svg)](https://gitlab.lrz.de/vadere/vadere/commits/master) | [![coverage report (master)](https://gitlab.lrz.de/vadere/vadere/badges/master/coverage.svg)](https://gitlab.lrz.de/vadere/vadere/commits/master) |
| stable | [![pipeline status (stable)](https://gitlab.lrz.de/vadere/vadere/badges/stable/pipeline.svg)](https://gitlab.lrz.de/vadere/vadere/commits/stable) | [![coverage report (stable)](https://gitlab.lrz.de/vadere/vadere/badges/stable/coverage.svg)](https://gitlab.lrz.de/vadere/vadere/commits/stable) |

## Installation

### Download Releases

Latest build of master:
* [Windows](http://www.vadere.org/builds/master/vadere.master.windows.zip)
* [Linux](http://www.vadere.org/builds/master/vadere.master.linux.zip)

Stable releases and selected branch-builds are located on [www.vadere.org/releases/](http://www.vadere.org/releases/)

The ZIP file contains:
* **README.md** - this README file. 
* **vadere.jar** - provides Vadere with a GUI. 
* **vadere_console.jar** - provides the command line version of Vadere and allows easy integration into other applications. 
* **VadereModelTests** - contains test scenarios for pedestrian locomotion models. Note: The tests are also useful for a "getting started" (see below "Run Built-In Examples" for details).

To execute the `.jar` files it is highly recommended to use Oracle Java 8.  

### Build from Source

#### Dependencies

* Java 8
* Maven 3.0
* Git
* OpenCL (optional but recommended)

**Note:** Please, ensure that the Git executable can be found in the `PATH` variable of your operating system.

#### Install OpenCL (optional but recommended)

Vadere uses computer's video card to speed up some computations. Therefore, following OpenCL components should be installed:

* the OpenCL Installable Client Driver loader also called ICD loader (Opencl.dll for Windows and libOpenCL.so for Linux)
* OpenCL Drivers for your device 

Both should be offered by the vendor of your device. The device can be a CPU as well as a GPU (recommanded). For example if you have a NVIDIA GPU updating your drivers should be enough to install both components. 
Vadere will search for the best device which is supported. On a desktop workstation this should be your video card (GPU). If there is no device Vadere will use a plain and slower Java-Implementation instead. 
Please, use following instructions to set up the OpenCL components for your operating system:

* Windows: For further information using OpenCL on Windows read the paragraph 
>  Running an OpenCL application
[click here](https://streamcomputing.eu/blog/2015-03-16/how-to-install-opencl-on-windows/).
* OS X: OpenCL is pre-installed for OS X.
* Linux: Please refer to the installation manual of your Linux distribution. 
  * [Sources: OpenCL HowTo](https://wiki.tiker.net/OpenCLHowTo)
  * Tips and official packages (Ubuntu): 
    
    <details>

    * Use the console tool `clinfo` (`sudo apt-get install clinfo`) to see the current status in terminal
    * Drivers commonly have the prefix `opencl-icd` (to look at most opencl related packages run `apt search opencl`). Some that may be helpful:
         * `beignet-opencl-icd` (OpenCL library for Intel GPUs)
         * `mesa-opencl-icd` (free and open source implementation of the OpenCL API)
         * `nvidia-opencl-icd`
         * `ocl-icd-opencl-dev` (installs opencl development files and can be required for compiling)
         * `ocl-icd-libopencl1` (Generic OpenCL ICD Loader)
    
    </details>
  * [Intel Driverpack (only driver needed)](https://software.intel.com/en-us/articles/opencl-drivers#latest_linux_driver)

#### Run the Application

1. Get the Source: Run `git clone https://gitlab.lrz.de/vadere/vadere.git`.
2. Build the Application: Go to the project directory and run `mvn clean package` (or `mvn clean package -Dmaven.test.skip` if you want to skip the unit tests). This will build `vadere.jar`, `vadere-console.jar` and `postvis.jar`.
3. Start the Application: After building the application, you can start Vadere by running `java -jar VadereGui/target/vadere.jar`.
4. (If you only want to use the Postvisualization-Tool you can do so by running `java -jar VadereGui/target/postvis.jar`).

## Run Built-In Examples

With the following steps, you can run a simulation with one of the built-in examples from [VadereModelTests](VadereModelTests):

- start Vadere 
- *Project* > *Open* 
- choose `vadere.project` of one of the projects e.g. [TestOSM](https://gitlab.lrz.de/vadere/vadere/tree/master/VadereModelTests/TestOSM) and click *open*
- select the scenario on the left and press *run selected scenario*

## Changelog

See [CHANGELOG.md](https://gitlab.lrz.de/vadere/vadere/blob/master/CHANGELOG.md) for a list of changes.

## JavaDoc

- [state](http://www.vadere.org/javadoc/state/index.html)

## Contribution

See [CONTRIBUTING.md](https://gitlab.lrz.de/vadere/vadere/blob/master/CONTRIBUTING.md) for how to set up the development environment and the coding guidelines.

## License

This software is licensed under the GNU Lesser General Public License ([LGPL](https://gitlab.lrz.de/vadere/vadere/blob/master/LICENSE)).

For more information: http://www.gnu.org/licenses/lgpl.html
