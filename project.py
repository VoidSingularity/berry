import hashlib, json, os, platform, re, urllib.request

def verify_sha1 (local, sha1):
    sha = hashlib.sha1 ()
    lcl = open ('.cache/' + local, 'rb')
    while ct := lcl.read (65536):
        sha.update (ct)
    return sha.hexdigest () == sha1

def download_resource (url, local, reuse=True, sha1=None):
    if os.path.exists ('.cache/' + local) and reuse:
        if sha1 is not None and not verify_sha1 (local, sha1): return download_resource (url, local, False, sha1)
        return False
    rmt = urllib.request.urlopen (url)
    lcl = open ('.cache/' + local, 'wb')
    while ct := rmt.read (65536): lcl.write (ct)
    rmt.close (); lcl.close ()
    # sha1 verification
    if sha1 is not None and not verify_sha1 (local, sha1): raise Exception ("Found incorrect SHA-1, download failed")
    return True

def check_rule (rules): # Return true if passed
    flag = False
    for rule in rules:
        if rule ['action'] == 'allow':
            oss = rule ['os']
            if (name := oss.get ('name')) is not None:
                if {'Linux':'linux','Darwin':'osx','Windows':'windows'} [platform.system ()] == name: flag = True
        else:
            oss = rule ['os']
            if (name := oss.get ('name')) is not None:
                if {'Linux':'linux','Darwin':'osx','Windows':'windows'} [platform.system ()] == name: flag = False
    return flag

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
    download_resource (vjson ['url'], 'client_official.json', False, vjson ['sha1'])
    cl = open ('.cache/client_official.json')
    cljson = json.load (cl)
    cl.close ()
    cldl = cljson ['downloads'] ['client']
    download_resource (cldl ['url'], 'client_official.jar', False, cldl ['sha1'])
    # Install BML
    cljson ['arguments'] ['game'] .insert (0, cljson ['mainClass'])
    cljson ['arguments'] ['jvm'] .append ('-javaagent:../../output/agent.jar')
    cljson ['mainClass'] = 'bluet.berry.loader.BerryLoaderMain'
    f = open ('.cache/client.json', 'w')
    json.dump (cljson, f)
    f.close ()
    if not os.path.exists ('.cache/game'): os.mkdir ('.cache/game')
    if not os.path.exists ('.cache/game/mods'): os.mkdir ('.cache/game/mods')

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

# Download dependencies
def download_dependencies (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    if not os.path.exists ('.cache/libs'): os.mkdir ('.cache/libs')
    for lib in cljson ['libraries']:
        name = lib ['name'] .split (':')
        if len (name) == 4: # Natives
            if name [3] in ['natives-macos', 'natives-windows-arm64', 'linux-aarch_64']: continue # We do not support these platforms now; wait for future updates
            rules = lib ['rules']
            if not check_rule (rules): continue
        artifact = lib ['downloads'] ['artifact']
        if download_resource (artifact ['url'], f'libs/{artifact ["path"] .split ("/") [-1]}', True, artifact ['sha1']):
            print (f'Successfully downloaded {lib ["name"]}')
        else:
            print (f'{lib ["name"]} already exists. Skipping.')

# Download assets
def download_assets (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    if not os.path.exists ('.cache/assets'): os.mkdir ('.cache/assets')
    if not os.path.exists ('.cache/assets/objects'): os.mkdir ('.cache/assets/objects')
    if not os.path.exists ('.cache/assets/indexes'): os.mkdir ('.cache/assets/indexes')
    ai = cljson ['assetIndex']
    download_resource (ai ['url'], 'assets/indexes/index.json', False, ai ['sha1'])
    index = open ('.cache/assets/indexes/index.json')
    indexjson = json.load (index)
    index.close ()
    s = '0123456789abcdef'
    for i in s:
        for j in s:
            if not os.path.exists (f'.cache/assets/objects/{i}{j}'):
                os.mkdir (f'.cache/assets/objects/{i}{j}')
    for obji in indexjson ['objects']:
        obj = indexjson ['objects'] [obji]
        if download_resource (
            f'https://resources.download.minecraft.net/{obj ["hash"] [:2]}/{obj ["hash"]}',
            f'assets/objects/{obj ["hash"] [:2]}/{obj ["hash"]}',
            True,
            obj ['hash']
        ): print (f'Successfully downloaded file {obji}')
        else: print (f'File {obji} already exists. Skipping.')

# TODO: Setup Intellij Workspace

# Setup VSCode Workspace
def setup_vscode (projectjson, properties):
    if not os.path.exists ('.vscode'): os.mkdir ('.vscode')
    if os.path.exists ('.vscode/settings.json'):
        st = open ('.vscode/settings.json')
        stjson = json.load (st)
        st.close ()
    else: stjson = {}
    li = stjson.get ('java.project.referencedLibraries', [])
    s = set (li)
    s.add ('.cache/libs/*.jar')
    s.add ('.cache/client.jar')
    stjson ['java.project.referencedLibraries'] = list (s)
    f = open ('.vscode/settings.json', 'w')
    json.dump (stjson, f)
    f.close ()

# Run Minecraft
def run_minecraft (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    ld = os.listdir ('.cache/libs/')
    cps = os.pathsep.join ([f'../libs/{i}' for i in ld] + ['../client.jar', '../../output/asm.jar', '../../output/loader.jar'])
    if not os.path.exists ('.cache/natives'): os.mkdir ('.cache/natives')
    vars = {
        'classpath': cps,
        'natives_directory': '../natives/',
        'launcher_name': '"BML Test"',
        'launcher_version': '1.0.0',
        'auth_player_name': properties.get ('player_name', 'Dev'),
        'version_name': properties.get ('version_name', properties.get ('minecraft_version', 'unknown')),
        'game_directory': './',
        'assets_root': '../assets/',
        'assets_index_name': 'index',
        'auth_uuid': '01234567-89ab-cdef-0123-456789abcdef',
        'auth_access_token': 'aa',
        'clientid': 'berry',
        'auth_xuid': 'bb',
        'user_type': 'cc',
        'version_type': 'Berry'
    }
    args = cljson ['arguments']
    jvmargs = []
    for jvmarg in args ['jvm']:
        if isinstance (jvmarg, str):
            jvmargs.append (re.sub ('\\$\\{([A-Za-z_]+)\\}', lambda m: vars [m.group (1)], jvmarg))
        else:
            rules = jvmarg ['rules']
            if check_rule (rules): jvmargs.append (jvmarg ['value'])
    gameargs = []
    for gamearg in args ['game']:
        if isinstance (gamearg, str):
            gameargs.append (re.sub ('\\$\\{([A-Za-z_]+)\\}', lambda m: vars [m.group (1)], gamearg))
    # Windows support
    if os.path.exists ('.cache/game/mods/api.jar'): os.remove ('.cache/game/mods/api.jar')
    os.rename ('output/api.jar', '.cache/game/mods/api.jar')
    os.chdir ('.cache/game/')
    os.system (f'java {" ".join (jvmargs)} {cljson ["mainClass"]} {" ".join (gameargs)}')
    os.chdir ('../../')
