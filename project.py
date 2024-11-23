import hashlib, json, os, platform, re, urllib.request

def syswrap (cmd):
    print ('$', cmd)
    os.system (cmd)

def verify_sha1 (local, sha1, root):
    sha = hashlib.sha1 ()
    lcl = open (root + local, 'rb')
    while ct := lcl.read (65536):
        sha.update (ct)
    return sha.hexdigest () == sha1

def download_resource (url, local, reuse=True, sha1=None, retry=10, root='.cache/'):
    if os.path.exists (root + local) and reuse:
        if sha1 is not None and not verify_sha1 (local, sha1, root): return download_resource (url, local, False, sha1)
        return False
    try:
        for i in range (retry):
            req = urllib.request.Request (url)
            req.add_header ('User-Agent', 'BerryBuild')
            rmt = urllib.request.urlopen (req)
            lcl = open (root + local, 'wb')
            while ct := rmt.read (65536): lcl.write (ct)
            rmt.close (); lcl.close ()
            # sha1 verification
            if sha1 is not None and not verify_sha1 (local, sha1, root): raise Exception ("Found incorrect SHA-1, download failed")
            return True
    except Exception as e: print (e)

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
    download_resource (vjson ['url'], 'client.json', True, vjson ['sha1'])
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    cldl = cljson ['downloads'] ['client']
    download_resource (cldl ['url'], 'client_official.jar', True, cldl ['sha1'])
    if not os.path.exists ('.cache/game'): os.mkdir ('.cache/game')
    if not os.path.exists ('.cache/game/mods'): os.mkdir ('.cache/game/mods')

# Deobfuscate Minecraft
import mapping
def deobfuscate (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    mpdl = cljson ['downloads'] ['client_mappings']
    download_resource (mpdl ['url'], 'client.txt', True, mpdl ['sha1'])
    mapping.convert_mappings ('.cache/client.txt', '.cache/client.tsrg', True)
    syswrap ('java -jar libs/specialsource.jar -q -i .cache/client_official.jar -o .cache/client.jar -m .cache/client.tsrg')

# Download dependencies
def download_dependencies (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    # Mojang ships ASM 9.3, but we need higher versions.
    libs = cljson ['libraries']
    for i in range (len (libs)):
        if libs [i] ['name'] .startswith ('org.ow2.asm'):
            libs.pop (i)
            break
    cl = open ('.cache/client.json', 'w')
    json.dump (cljson, cl)
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
    hb = os.path.expanduser ('~')
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    if not os.path.exists (f'{hb}/.berry'): os.mkdir (f'{hb}/.berry')
    if not os.path.exists (f'{hb}/.berry/assets'): os.mkdir (f'{hb}/.berry/assets')
    if not os.path.exists (f'{hb}/.berry/assets/objects'): os.mkdir (f'{hb}/.berry/assets/objects')
    if not os.path.exists (f'{hb}/.berry/assets/indexes'): os.mkdir (f'{hb}/.berry/assets/indexes')
    ai = cljson ['assetIndex']
    download_resource (ai ['url'], f'assets/indexes/{ai["id"]}.json', True, ai ['sha1'], root=f'{hb}/.berry/')
    index = open (f'{hb}/.berry/assets/indexes/{ai["id"]}.json')
    indexjson = json.load (index)
    index.close ()
    s = '0123456789abcdef'
    for i in s:
        for j in s:
            if not os.path.exists (f'{hb}/.berry/assets/objects/{i}{j}'):
                os.mkdir (f'{hb}/.berry/assets/objects/{i}{j}')
    for obji in indexjson ['objects']:
        obj = indexjson ['objects'] [obji]
        if download_resource (
            f'https://resources.download.minecraft.net/{obj ["hash"] [:2]}/{obj ["hash"]}',
            f'assets/objects/{obj ["hash"] [:2]}/{obj ["hash"]}',
            True,
            obj ['hash'],
            root=f'{hb}/.berry/'
        ): print (f'Successfully downloaded file {obji}')
        else: print (f'File {obji} already exists. Skipping.')

# Download bundled jars for asm and mixin
def download_bundled (projectjson, properties):
    li = [
        "https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/sponge-mixin-0.15.4+mixin.0.8.7.jar",
        "https://maven.fabricmc.net/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar",
        "https://maven.fabricmc.net/org/ow2/asm/asm-analysis/9.7.1/asm-analysis-9.7.1.jar",
        "https://maven.fabricmc.net/org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar",
        "https://maven.fabricmc.net/org/ow2/asm/asm-commons/9.7.1/asm-commons-9.7.1.jar",
        "https://maven.fabricmc.net/org/ow2/asm/asm-util/9.7.1/asm-util-9.7.1.jar"
    ]
    if not os.path.exists ('runtime'): os.mkdir ('runtime')
    for i in li: download_resource (i, '../runtime/' + i.split ('/') [-1], False)

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
    s.add ('runtime/*.jar')
    stjson ['java.project.referencedLibraries'] = list (s)
    li = stjson.get ('java.project.sourcePaths', [])
    s = set (li)
    s.add ('src/agent/')
    s.add ('src/loader/')
    s.add ('src/builtins/')
    stjson ['java.project.sourcePaths'] = list (s)
    f = open ('.vscode/settings.json', 'w')
    json.dump (stjson, f)
    f.close ()

# Run Minecraft
def run_minecraft (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    ld = os.listdir ('.cache/libs/')
    cps = os.pathsep.join ([f'../libs/{i}' for i in ld] + ['../client.jar', '../../output/loader.jar', '../../output/utils.jar'])
    if not os.path.exists ('.cache/natives'): os.mkdir ('.cache/natives')
    vars = {
        'classpath': cps,
        'natives_directory': '../natives/',
        'launcher_name': '"BML Test"',
        'launcher_version': '1.0.0',
        'auth_player_name': properties.get ('player_name', 'Dev'),
        'version_name': properties.get ('version_name', properties.get ('minecraft_version', 'unknown')),
        'game_directory': './',
        'assets_root': os.path.expanduser ('~/.berry/assets/'),
        'assets_index_name': cljson ['assetIndex'] ['id'],
        'auth_uuid': '01234567-89ab-cdef-0123-456789abcdef',
        'auth_access_token': 'aa',
        'clientid': 'berry',
        'auth_xuid': 'bb',
        'user_type': 'cc',
        'version_type': 'Berry'
    }
    args = cljson ['arguments']
    jvmargs = ['-javaagent:../../output/agent.jar']
    for jvmarg in args ['jvm']:
        if isinstance (jvmarg, str):
            jvmargs.append (re.sub ('\\$\\{([A-Za-z_]+)\\}', lambda m: vars [m.group (1)], jvmarg))
        else:
            rules = jvmarg ['rules']
            if check_rule (rules): jvmargs.append (jvmarg ['value'])
    gameargs = [cljson ['mainClass']]
    for gamearg in args ['game']:
        if isinstance (gamearg, str):
            gameargs.append (re.sub ('\\$\\{([A-Za-z_]+)\\}', lambda m: vars [m.group (1)], gamearg))
    if os.path.exists ('.cache/game/mods/builtins.jar'): os.remove ('.cache/game/mods/builtins.jar')
    if not os.path.exists ('.cache/game'): os.mkdir ('.cache/game')
    if not os.path.exists ('.cache/game/mods'): os.mkdir ('.cache/game/mods')
    os.rename ('output/builtins.jar', '.cache/game/mods/builtins.jar')
    os.chdir ('.cache/game/')
    syswrap (f'java {" ".join (jvmargs)} berry.loader.BerryLoader {" ".join (gameargs)}')
    os.chdir ('../../')
