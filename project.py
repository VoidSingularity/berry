import hashlib, json, os, urllib.request

def download_resource (url, local, reuse=True, sha1=None):
    if os.path.exists ('.cache/' + local) and reuse: return
    rmt = urllib.request.urlopen (url)
    lcl = open ('.cache/' + local, 'wb')
    while ct := rmt.read (65536): lcl.write (ct)
    rmt.close (); lcl.close ()
    # sha1 verification
    if sha1 is not None:
        sha = hashlib.sha1 ()
        lcl = open ('.cache/' + local, 'rb')
        while ct := lcl.read (65536): sha.update (ct)
        lcl.close ()
        if sha.hexdigest () != sha1: raise Exception ("Found incorrect SHA-1, download failed")

# Download Minecraft version manifest

MANIFEST_LOCATION = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

def download_manifest (projectjson, properties):
    download_resource (MANIFEST_LOCATION, 'version_manifest.json', False)

# Download Minecraft

def download_minecraft (projectjson, properties):
    mf = open ('.cache/version_manifest.json')
    mfjson = json.load (mf)
    mf.close ()
    version = properties.get ('minecraft_version')
    if version is None: version = mfjson ['latest'] ['release']
    for vjson in mfjson ['versions']:
        if vjson ['id'] == version:
            break
    else:
        raise Exception (f"Cannot find version {version} in version manifest. Try run build.py download_manifest again.")
    download_resource (vjson ['url'], 'client.json', False, vjson ['sha1'])
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    cldl = cljson ['downloads'] ['client']
    download_resource (cldl ['url'], 'client.jar', False, cldl ['sha1'])

# TODO: Deobfuscate Minecraft
# TODO: Download dependencies
# TODO: Download assets
# TODO: Setup Intellij Workspace
# TODO: Setup VSCode Workspace
# TODO: Run Minecraft
