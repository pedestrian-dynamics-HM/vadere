import getpass
import os
import re
import subprocess
import shutil


def install_package_if_needed(package_name='VadereAnalysisTool', search_path='Tools/VadereAnalysisTool'):
    try:

        make_package_cwd = os.path.abspath(search_path)

        if os.path.exists(os.path.join(search_path, "dist")):
            shutil.rmtree(os.path.join(search_path, "dist"))

        print("Build package {}...".format(package_name))
        p_make_package = subprocess.run(
            args=["python3", "setup.py",  "bdist_wheel"],
            cwd= make_package_cwd,
            timeout=10,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE)
        if p_make_package.stdout:
            print("StdOut: {}".format(p_make_package.stdout.decode('utf8')))
        if p_make_package.stderr:
            print("StdErr: {}".format(p_make_package.stderr.decode('utf8')))
        print("ReturnCode: {}".format(p_make_package.returncode))

        if p_make_package.returncode == 0:
            stdout = p_make_package.stdout.decode('utf8')
            dist_dir = os.path.join(search_path, "dist")
            wheel_files = [f for f in os.listdir(dist_dir) if f.endswith(".whl")]
            if len(wheel_files) > 0:
                dist_path = os.path.join("dist", wheel_files[0])
                user = getpass.getuser()

                print("\nInstall package {} locally for user {} ...".format(dist_path, user))
                p_install_package = subprocess.run(
                    args=["python3", "-m", "pip", "install", "--user",  dist_path],
                    cwd=make_package_cwd,
                    timeout=10,
                    check=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE)

                if p_install_package.stdout:
                    print("StdOut: {}".format(p_install_package.stdout.decode('utf8')))
                if p_install_package.stderr:
                    print("StdErr: {}".format(p_install_package.stderr.decode('utf8')))
                print("ReturnCode: {}".format(p_install_package.returncode))

            else:
                exit(1)
        else:
            exit(1)


    except subprocess.TimeoutExpired as exception:
        print("Timeout installing {} from path {}".format(package_name, search_path))

    except subprocess.CalledProcessError as exception:
        print("Error installing {} from path {}\n  err:{}".format(package_name,
                                                                  search_path,
                                                                  exception.stderr))


if __name__ == '__main__':
    install_package_if_needed()
