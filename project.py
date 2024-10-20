import hashlib, json, os, urllib.request

def verify_sha1 (local, sha1):
    sha = hashlib.sha1 ()
    lcl = open ('.cache/' + local, 'rb')
    while ct := lcl.read (65536):
        sha.update (ct)
    return sha.hexdigest () == sha1

def download_resource (url, local, reuse=True, sha1=None):
    if os.path.exists ('.cache/' + local) and reuse:
        if sha1 is not None and not verify_sha1 (local, sha1): download_resource (url, local, False, sha1)
        return False
    rmt = urllib.request.urlopen (url)
    lcl = open ('.cache/' + local, 'wb')
    while ct := rmt.read (65536): lcl.write (ct)
    rmt.close (); lcl.close ()
    # sha1 verification
    if sha1 is not None and not verify_sha1 (local, sha1): raise Exception ("Found incorrect SHA-1, download failed")
    return True

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
    download_resource (cldl ['url'], 'client_official.jar', False, cldl ['sha1'])

# Deobfuscate Minecraft
# Deobfuscation is possible with bluet.berry.asm.ClassFile, but I'm just being lazy here
import mapping
def deobfuscate (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    mpdl = cljson ['downloads'] ['client_mappings']
    download_resource (mpdl ['url'], 'client.txt', False, mpdl ['sha1'])
    mapping.convert_mappings ('.cache/client.txt', '.cache/client.tsrg', True)
    os.system ('java -jar libs/specialsource.jar -q -i .cache/client_official.jar -o .cache/client.jar -m .cache/client.tsrg')

# TODO: Download dependencies

# Download assets
def download_assets (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    ai = cljson ['assetIndex']
    download_resource (ai ['url'], 'index.json', False, ai ['sha1'])
    index = open ('.cache/index.json')
    indexjson = json.load (index)
    index.close ()
    if not os.path.exists ('.cache/assets'): os.mkdir ('.cache/assets')
    s = '0123456789abcdef'
    for i in s:
        for j in s:
            if not os.path.exists (f'.cache/assets/{i}{j}'):
                os.mkdir (f'.cache/assets/{i}{j}')
    for obji in indexjson ['objects']:
        obj = indexjson ['objects'] [obji]
        if download_resource (
            f'https://resources.download.minecraft.net/{obj ["hash"] [:2]}/{obj ["hash"]}',
            f'assets/{obj ["hash"] [:2]}/{obj ["hash"]}',
            True,
            obj ['hash']
        ): print (f'Successfully downloaded file {obji}')
        else: print (f'File {obji} already exists. Skipping.')

# TODO: Setup Intellij Workspace
# TODO: Setup VSCode Workspace
# TODO: Run Minecraft
