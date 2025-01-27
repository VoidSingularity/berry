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

# This program creates a local server that allows
# BML developers to conveniently test dev-builds
# of BML in other mods. Buildscript in mod work-
# spaces will firstly check whether the local
# server exist. If so, the buildscript will choo-
# se to download and update BML from the local
# server. If not, the buildscript will delete the
# files previously downloaded from local servers.
# The local files will be stored at /dists/ and
# every time BML is built, you can manually choo-
# se to put the jars in the server. The server
# will automatically update the version info.

# TODO: FIX LOCALDIST!

from xmlrpc.server import SimpleXMLRPCServer

import hashlib, os, re, sys, threading
if not os.path.exists ('dists'): os.mkdir ('dists')

# Parse the current distributions.
fmat = re.compile ('([A-Za-z0-9\\-]+)\\.(jar|py)\\-([a-f0-9]{64})')
rels = {}
for fn in os.listdir ('dists'):
    mo = re.match (fmat, fn)
    if mo is None: continue
    # Try to verify the hash
    fh = open ('dists/' + fn, 'rb')
    ho = hashlib.sha256 ()
    while len (val := fh.read (65536)): ho.update (val)
    if mo.group (2) != ho.hexdigest ():
        print ("Error: file", fn, "does not match its hash!", file=sys.stderr)
        continue
    if mo.group (1) not in rels: rels [mo.group (1)] = fn
    else:
        # Compare mtime between two files
        old = rels [mo.group (1)]
        mto = os.stat ('dists/' + old) .st_mtime_ns
        mtn = os.stat ('dists/' + fn) .st_mtime_ns
        if mtn > mto: rels [mo.group (1)] = fn

# Update
def update ():
    fo = open ('output/project_template.py', 'wb')
    fi = open ('project_template.py', 'rb')
    while len (val := fi.read (65536)): fo.write (val)
    fi.close (); fo.close ()
    fo = open ('output/build.py', 'wb')
    fi = open ('build.py', 'rb')
    while len (val := fi.read (65536)): fo.write (val)
    fi.close (); fo.close ()
    for fn in os.listdir ('output'):
        # Calculate hash
        fh = open ('output/' + fn, 'rb')
        ho = hashlib.sha256 ()
        while len (val := fh.read (65536)): ho.update (val)
        # Copy file
        if fn in rels and rels [fn] == f'{fn}-{ho.hexdigest()}':
            print ("Error: file", fn, "is already distributed!", file=sys.stderr)
            continue
        if not os.path.exists (f'dists/{fn}-{ho.hexdigest()}'):
            fo = open (f'dists/{fn}-{ho.hexdigest()}', 'wb')
            fi = open ('output/' + fn, 'rb')
            while len (val := fi.read (65536)): fo.write (val)
            fo.close (); fi.close ()
        # Deploy
        rels [fn] = f'{fn}-{ho.hexdigest()}'

# Query
def fexist (jarname: str): return jarname in rels
def fquery (jarname: str):
    f = open ('dists/' + rels [jarname], 'rb')
    bi = f.read ()
    f.close ()
    return bi

# Run
srv = SimpleXMLRPCServer (('', 19922))
srv.register_function (fexist)
srv.register_function (fquery)
thr = threading.Thread (target=srv.serve_forever, daemon=True)
thr.start ()
while True:
    if input ('> '):
        update ()
