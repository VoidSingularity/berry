#!/usr/bin/python3

#  Copyright (C) 2025 VoidSingularity

#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.

#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.

#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <https://www.gnu.org/licenses/>.

import json, os, shutil, sys, zipfile

pkgf = open ('project.json')
jsonfile = json.load (pkgf)
packages = jsonfile ['packages']
runs = jsonfile ['runs']
pkgf.close ()
prof = open ('properties.json')
properties = json.load (prof)
prof.close ()

if not os.path.exists ('build'): os.mkdir ('build')
if not os.path.exists ('output'): os.mkdir ('output')
if not os.path.exists ('.cache'): os.mkdir ('.cache')

def syswrap (cmd):
    print ('$', cmd)
    os.system (cmd)

def javac (dn, opt):
    for fn in os.listdir (dn):
        if os.path.isfile (dn + fn):
            syswrap (f'javac {opt} -d build {dn}{fn}')
        else: javac (dn + fn + os.sep, opt)

def clean_build ():
    if not os.path.isdir ('build'): return
    for fn in os.listdir ('build/'):
        if os.path.isfile ('build/' + fn):
            os.remove ('build/' + fn)
        else:
            shutil.rmtree ('build/' + fn)

built = set ()

def lsrecursive (root, d = ''):
    ret = []
    for f in os.listdir (root + d):
        if os.path.isdir (root + d + f):
            ret += lsrecursive (root, d + f + os.sep)
        else: ret.append (d + f)
    return ret

def build (name, pkg):
    if name in built: return
    deps = pkg.get ("dep")
    opt = ' --class-path=./'
    if deps is not None:
        for dep in deps:
            if handle (dep): opt += f'{os.pathsep}output/{dep}.jar'
    cps = pkg.get ("cps")
    if cps is not None:
        for cp in cps:
            cp = os.path.expanduser (cp)
            if os.path.isdir (cp):
                for l in lsrecursive (cp):
                    opt += f'{os.pathsep}{cp}{l}'
            else: opt += f'{os.pathsep}{cp}'
    srcpth = f'src/{pkg ["source"]}/'
    lr = lsrecursive (srcpth)
    lo = []
    for l in lr:
        if l.endswith ('.java'):
            lo.append (srcpth + l)
    shutil.rmtree ('build')
    syswrap (f'javac {opt} -s {srcpth} -d build {" ".join (lo)}')
    # Some sort of issue related to the jar command
    zip = zipfile.ZipFile (f'output/{name}.jar', 'w')
    for c in lsrecursive ('build/'):
        f = open (f'build/{c}', 'rb')
        g = zip.open (c, 'w')
        g.write (f.read ())
        f.close ()
        g.close ()
        print (f"Added file {c} to jar file {name}.jar")
    mf = pkg.get ("manifest")
    if mf is not None:
        f = open (f'manifest/{mf}.mf', 'rb')
        g = zip.open ('META-INF/MANIFEST.MF', 'w')
        g.write (f.read ())
        f.close ()
        g.close ()
        print (f"Added manifest {mf}.mf to jar file {name}.jar")
    extras = pkg.get ('extras')
    if extras is not None:
        for extra in extras:
            src, dst = extra ['source'], extra ['destination']
            if os.path.isdir (src):
                for i in os.listdir (src):
                    f = open (f'{src}{i}', 'rb')
                    g = zip.open (f'{dst}{i}', 'w')
                    g.write (f.read ())
                    f.close ()
                    g.close ()
                    print (f'Added extra file {dst}{i} to jar file {name}.jar')
            else:
                f = open (f'{src}', 'rb')
                g = zip.open (f'{dst}', 'w')
                g.write (f.read ())
                f.close ()
                g.close ()
                print (f'Added extra file {dst} to jar file {name}.jar')
    zip.close ()
    built.add (name)

import project
try:
    stf = open ('.cache/status.json')
    status = set (json.load (stf))
    stf.close ()
except FileNotFoundError:
    status = set ()
def run (name, task, force=False):
    re = task.get ('repeat_everytime')
    if re is not True and not force and name in status: return
    deps = task.get ('dep')
    if deps is not None:
        for dep in deps:
            handle (dep)
    fn = task.get ('function')
    if fn is not None:
        func = getattr (project, fn)
        func (jsonfile, properties)
    status.add (name)

def handle (name):
    flag = False
    if name in packages:
        flag = True
        build (name, packages [name])
    elif name in runs: run (name, runs [name])
    print (f'Task {name} completed by handle()')
    return flag

def main ():
    for a in sys.argv [1:]:
        if a in packages:
            build (a, packages [a])
        elif a in runs:
            run (a, runs [a], True)
        print (f'Task {a} completed by main()')
    f = open ('.cache/status.json', 'w')
    json.dump (list (status), f)
    f.close ()

if __name__ == '__main__': main ()
