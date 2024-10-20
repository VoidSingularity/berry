#!/usr/bin/python3

import json, os, shutil, sys, time

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

def javac (dn, opt):
    for fn in os.listdir (dn):
        if os.path.isfile (dn + fn):
            os.system (f'javac {opt} -d build {dn}{fn}')
        else: javac (dn + fn + os.sep, opt)

def clean_build ():
    if not os.path.isdir ('build'): return
    for fn in os.listdir ('build/'):
        if os.path.isfile ('build/' + fn):
            os.remove ('build/' + fn)
        else:
            shutil.rmtree ('build/' + fn)

built = set ()
def build (name, pkg):
    if name in built: return
    deps = pkg.get ("dep")
    opt = ' --class-path=./'
    if deps is not None:
        for dep in deps:
            build (dep, packages [dep])
            opt += f':output/{dep}.jar'
    cps = pkg.get ("cps")
    if cps is not None:
        for cp in cps:
            opt += f':libs/{cp}.jar'
    clean_build ()
    if os.path.exists (f'output/{pkg ["output"]}'): os.remove (f'output/{pkg ["output"]}')
    pkgpth = pkg ['pkg']
    if os.path.isfile (pkgpth): os.system (f'javac {opt} -d build {pkgpth}')
    else: javac (pkgpth, opt)
    cmd = 'jar'
    if pkg.get ('manifest') is None: cmd += ' -Mcvf'
    else: cmd += f" -mcvf manifest/{pkg ['manifest']}"
    cmd += f" output/{pkg ['output']} -C build "
    if os.path.isfile (pkgpth): cmd += '/'.join (pkgpth.split ('/') [:-1]) + '/'
    else: cmd += pkgpth
    os.system (cmd)
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
    if name in packages: build (name, packages [name])
    elif name in runs: run (name, runs [name])
    print (f'Task {name} completed by handle()')

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
