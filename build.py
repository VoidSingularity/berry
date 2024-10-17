#!/usr/bin/python3

import json, sys, os

pkgf = open ('packages.json')
packages = json.load (pkgf)
pkgf.close ()

def javac (dn, opt):
    for fn in os.listdir (dn):
        if os.path.isfile (dn + fn):
            os.system (f'javac {opt} -d build {dn}{fn}')
        else: javac (dn + fn + os.sep, opt)

def build (pkg):
    deps = pkg.get ("dep")
    opt = ' --class-path=./'
    if deps is not None:
        for dep in deps:
            build (packages [dep])
            opt += f':output/{dep}.jar'
    cps = pkg.get ("cps")
    if cps is not None:
        for cp in cps:
            opt += f':libs/{cp}.jar'
    os.system ("rm -rf build/*")
    os.system (f"rm output/{pkg ['output']}")
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

if __name__ == '__main__':
    for a in sys.argv:
        if a in packages:
            build (packages [a])
