# OpenCL Installation (optional but recommended)

Vadere uses computer's video card to speed up some computations. Therefore, following OpenCL components should be installed:

* the OpenCL Installable Client Driver loader also called ICD loader (Opencl.dll for Windows and libOpenCL.so for Linux)
* drivers for your device(s) 

Both should be offered by the vendor of your device (often there are also open-source solutions). The device can be a CPU as well as a GPU (recommanded). For example, if you have a NVIDIA GPU instaling your drivers should be enough to install both components. 
Vadere will search for the best suiable device which is supported. On a desktop workstation this should be your video card (GPU). If there is no device supporting OpenCL Vadere will use a plain and slower Java-implementation instead. 

Please, use following instructions to set up the OpenCL components for your operating system:

* Windows: For further information using OpenCL on Windows read the paragraph Running an OpenCL application [click here](https://streamcomputing.eu/blog/2015-03-16/how-to-install-opencl-on-windows/).
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
 
