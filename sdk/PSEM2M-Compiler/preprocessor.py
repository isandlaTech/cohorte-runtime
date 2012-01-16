#!/usr/bin/python
#-- Content-Encoding: utf-8 --
'''
Created on 8 déc. 2011

@author: Thomas Calmant
'''

from optparse import OptionParser, OptionGroup
from ant_generator import EclipseAntGenerator
import eclipse_reader
import glob
import logging
import os
import subprocess
import sys

class InvalidParameterException(Exception):
    """
    Exception thrown when an invalid parameter is found
    """
    pass


def read_parameters():
    """
    Reads the arguments from sys.argv
    
    Returns a dictionary with the given keys :
    
    * name : The compilation run name
    * clean : If True, only remove build.xml files
    * clean_after_build : If True, remove build.xml after compilation
    
    * root : The root projects directory
    * extra_paths : An array of extra projects roots directories
    * target : The target platform path
    
    * ipojo.ant : The iPOJO Ant task definition file
    * ipojo.annotations : The iPOJO Annotations library
    
    @return: A dictionary
    @raise InvalidParameterException: The user entered an invalid parameter
    """
    parser = OptionParser()

    # Standard options
    opt_help = "Sets the name of the compilation (optional)"
    parser.add_option("-n", "--name", dest="name", default="compilation", \
                      metavar="NAME", help=opt_help)

    opt_help = "Only remove generated build.xml files."
    parser.add_option("--clean", dest="clean", action="store_true", \
                      default=False, help=opt_help)

    opt_help = "Remove generated build.xml files after compilation."
    parser.add_option("--clean-after-build", dest="clean_after_build", \
                      action="store_true", default=False, help=opt_help)

    # Project paths
    group = OptionGroup(parser, "Projects paths", \
                        "Paths to look for projects and libraries")

    opt_help = "Sets the path of the main root directory to recursively " \
                + "search for Eclipse projects and to place the root " \
                + "build.xml file. Uses the current directory by default."
    group.add_option("-R", "--root-directory", dest="root_dir", \
                      metavar="DIR", help=opt_help)

    opt_help = "Extra paths to recursively look for Eclipse project, " \
                + "separated with " + os.pathsep
    group.add_option("-E", "--extra-directories", dest="extra_dirs", \
                      metavar="DIR:DIR:...", help=opt_help)

    opt_help = "Sets the path of the product target platform " \
                + "(required if not cleaning...)"
    group.add_option("-T", "--target-platform", dest="target_platform",
                      metavar="DIR", help=opt_help)

    parser.add_option_group(group)

    # iPOJO Options
    group = OptionGroup(parser, "iPOJO Options", "iPOJO files paths")

    opt_help = "Sets the path to the iPOJO Ant task definition file"
    group.add_option("--ipojo-ant-file", dest="ipojo_ant_file", \
                      metavar="FILE", help=opt_help)

    opt_help = "Sets the path to the iPOJO Annotations library JAR file"
    group.add_option("--ipojo-annotations", dest="ipojo_annotations_lib", \
                      metavar="FILE", help=opt_help)

    parser.add_option_group(group)

    # Parse the sys.args, ignore extra parameters
    options = parser.parse_args()[0]

    # Normalize parameters
    only_clean = options.clean

    # ... root directory
    if not options.root_dir:
        root_dir = os.getcwd()
    else:
        root_dir = os.path.abspath(options.root_dir)

    if not os.path.isdir(root_dir):
        raise InvalidParameterException(\
                        "The given root directory is not found : %s" % root_dir)

    # ... target platform, relative to the root directory
    target_platform = options.target_platform
    if not target_platform:
        if not only_clean:
            raise InvalidParameterException(\
                        "A target platform directory must be given")

    elif os.path.isdir(target_platform):
        # The given target platform path is a valid
        target_platform = os.path.abspath(target_platform)

    else:
        # Try to use it as a relative path
        target_platform_path = os.path.normpath(root_dir + os.sep \
                                                + target_platform)

        if os.path.isdir(target_platform_path):
            target_platform = os.path.abspath(target_platform_path)

        else:
            raise InvalidParameterException(\
                        "The given target platform directory is invalid : %s" \
                        % target_platform)

    # ... Extra directories
    extra_dirs = []
    if options.extra_dirs != None:
        extra_paths = options.extra_dirs.split(os.pathsep)

        for path in extra_paths:
            # Convert to real world paths
            path = os.path.expanduser(path)
            path = os.path.expandvars(path)

            if os.path.isdir(path):
                extra_dirs.append(os.path.abspath(path))


    # ... iPOJO Ant file
    ipojo_ant_file = None
    if options.ipojo_ant_file != None \
        and (os.path.isfile(options.ipojo_ant_file) \
             or os.path.islink(options.ipojo_ant_file)):

        ipojo_ant_file = os.path.abspath(options.ipojo_ant_file)

    ipojo_annotations_lib = None
    if options.ipojo_annotations_lib != None \
        and (os.path.isfile(options.ipojo_annotations_lib) \
             or os.path.islink(options.ipojo_annotations_lib)):

        ipojo_annotations_lib = os.path.abspath(options.ipojo_annotations_lib)


    return {"name": options.name, "clean": only_clean, \
            "clean_after_build": options.clean_after_build, \
            "root": root_dir, "extra_paths": extra_dirs, \
            "target":target_platform, \
            "ipojo.ant": ipojo_ant_file, \
            "ipojo.annotations": ipojo_annotations_lib}


def get_projects(projects_roots):
    """
    Retrieves all Eclipse projects recursively
    
    @param projects_roots: Roots directories for recursive search
    """
    projects = []

    for root in projects_roots:
        pattern = os.path.normpath(root + os.sep + "*" \
                                   + os.sep + ".project")
        project_files = glob.glob(pattern)

        for project_file in project_files:
            try:
                projects.append(eclipse_reader.read_project(project_file))

            except:
                logging.warn("Error reading file.", exc_info=True)

    return projects


def main():
    """
    Entry point
    """
    # Parse program parameters
    try:
        params = read_parameters()

    except InvalidParameterException as ex:
        print >> sys.stderr, "Error reading parameters :", ex
        return 1

    result = 0

    # Compute root Ant file name
    root_ant_file = os.path.normpath(params["root"] + os.sep + "build.xml")

    try:
        print "--> Get Projects..."
        projects_roots = [params["root"]]
        projects_roots.extend(params["extra_paths"])
        projects = get_projects(projects_roots)

        if not params["clean"]:

            print "--> Resolve links..."
            for project in projects:
                project.resolve_links(projects)

            print "--> Generate Ant files..."
            ant_generator = EclipseAntGenerator(projects, params["name"], \
                                                params["ipojo.annotations"], \
                                                params["ipojo.ant"])

            ant_generator.prepare_ant_files(params["root"], [params["target"]])

            print "--> Compile time !"
            result = subprocess.call(["ant", "-f", root_ant_file, "package"])

    except:
        logging.error("Error during treatment.", exc_info=True)
        result = 1

    try:
        print "--> Clean up the mess"
        clean_build_xml = params["clean"] or params["clean_after_build"]

        for project in projects:
            project.clean(clean_build_xml)

        if clean_build_xml:
            os.remove(root_ant_file)

    except:
        logging.warn("Error during cleanup.", exc_info=True)

    return result


if __name__ == '__main__':
    logging.basicConfig()
    sys.exit(main())
