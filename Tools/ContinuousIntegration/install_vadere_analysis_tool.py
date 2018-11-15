import getpass
import os
import re
import subprocess


def install_package_if_needed(package_name='VadereAnalysisTool', search_path='Tools/VadereAnalysisTool'):
    try:

        make_package_cwd = os.path.abspath(search_path)

        print("Build package {}...".format(package_name))
        p_make_package = subprocess.run(
            args=["python3", "setup.py",  "bdist_wheel"],
            cwd= make_package_cwd,
            timeout=10,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE)
        print("ReturnCode: {}\nStdOut: {} \nStdErr: {}".format(p_make_package.returncode,
                                                               p_make_package.stdout.decode('utf8'),
                                                               p_make_package.stderr.decode('utf8')))
        if p_make_package.returncode == 0:
            stdout = p_make_package.stdout.decode('utf8')
            re_res = re.search("creating '(?P<name>.*?)'", stdout)
            if re_res is not None:
                dist_path = re_res.group('name')
                user = getpass.getuser()

                print("Install package {} locally for user {} ...".format(package_name, user))
                p_install_package = subprocess.run(
                    args=["python3", "-m", "pip", "install", "--user",  dist_path],
                    cwd=make_package_cwd,
                    timeout=10,
                    check=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE)

                print("ReturnCode: {}\nStdOut: {} \nStdErr: {}".format(p_install_package.returncode,
                                                                       p_install_package.stdout.decode('utf8'),
                                                                       p_install_package.stderr.decode('utf8')))
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
