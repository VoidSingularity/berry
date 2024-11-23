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

from xmlrpc.server import SimpleXMLRPCServer

import hashlib, os, re, sys, threading
if not os.path.exists ('dists'): os.mkdir ('dists')

# Parse the current distributions.
fmat = re.compile ('([A-Za-z0-9\\-]+)\\-([a-f0-9]{64}).jar')
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
        mto = os.stat ('dists' + old) .st_mtime_ns
        mtn = os.stat ('dists' + fn) .st_mtime_ns
        if mtn > mto: rels [mo.group (1)] = fn

# Update
def update ():
    for fn in os.listdir ('output'):
        if not fn.endswith (".jar"): continue
        jn = fn [:-4]
        # Calculate hash
        fh = open ('output/' + fn, 'rb')
        ho = hashlib.sha256 ()
        while len (val := fh.read (65536)): ho.update (val)
        # Copy file
        if jn in rels and rels [jn] == f'{jn}-{ho.hexdigest()}.jar':
            print ("Error: jar", jn, "is already distributed!", file=sys.stderr)
            continue
        if not os.path.exists (f'dists/{jn}-{ho.hexdigest()}.jar'):
            fo = open (f'dists/{jn}-{ho.hexdigest()}.jar', 'wb')
            fi = open ('output/' + fn, 'rb')
            while len (val := fi.read (65536)): fo.write (val)
            fo.close (); fi.close ()
        # Deploy
        rels [jn] = f'{jn}-{ho.hexdigest()}.jar'

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
