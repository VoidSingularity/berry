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

import hashlib, json, os, platform, random, re, shutil, subprocess, sys, urllib.request, xmlrpc.client, zipfile

def syswrap (arg):
    if isinstance (arg, str): arg = arg.split ()
    print ('$', *arg)
    subprocess.run (arg)

def fcopy (fin, fon):
    fi = open (fin, 'rb')
    fo = open (fon, 'wb')
    while len (val := fi.read (65536)): fo.write (val)
    fi.close ()
    fo.close ()

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
        for _ in range (retry):
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

def mkrecursive (name: str):
    name = os.path.expanduser (name.replace ('/', os.sep))
    if not name.endswith (os.sep): name += os.sep
    if os.path.exists (name): return
    par = name.rsplit (os.sep, 2) [0] + os.sep
    if not os.path.exists (par): mkrecursive (par)
    os.mkdir (name)

def gfg (url: str, name: str, path: str):
    # name is unused
    download_resource (url, path, root='./')

try:
    if '--offline' in sys.argv: raise FileNotFoundError ()
    dist = json.load (open ('localinfo.json'))
    if 'dist_addr' not in dist or 'dist_pswd' not in dist: raise FileNotFoundError ()
    print ("Localdist enabled")
    # https://github.com/azure-bluet/dist.py
    class ServerConf:
        def __init__ (self, addr: str, pswd: str):
            self.src = xmlrpc.client.ServerProxy (addr)
            self.pswd = pswd
        def download (self, name: str, path: str) -> None:
            f = open (path, 'wb')
            f.write (self.src.read (self.pswd, [name]) [name] .data)
            f.close ()
        def upload (self, name: str, path: str) -> None:
            f = open (path, 'rb')
            data = f.read ()
            f.close ()
            self.src.write (self.pswd, {name: xmlrpc.client.Binary (data)})
    conf = ServerConf (dist ['dist_addr'], dist ['dist_pswd'])
    def getfile (url: str, name: str, path: str):
        try:
            conf.download (name, path)
            if not os.path.exists (f'{path}.localdist'): open (f'{path}.localdist', 'w') .close ()
        except ConnectionRefusedError:
            # Remove old files
            if os.path.exists (f'{path}.localdist'):
                os.remove (path)
                os.remove (f'{path}.localdist')
            gfg (url, name, path)
except FileNotFoundError: getfile = gfg

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
    mkrecursive ('.cache/game/mods/')
    mkrecursive ('.cache/server/mods/')
    svdl = cljson ['downloads'] ['server']
    download_resource (svdl ['url'], 'server/server_une.jar', True, svdl ['sha1'])
    # Extract
    zin = zipfile.ZipFile ('.cache/server/server_une.jar', 'r')
    zin.extractall ('.cache/server/')
    # We don't even NEED net.minecraft.bundler
    # Also we can't read it anyway xd
    shutil.rmtree ('.cache/server/net')
    os.rename ('.cache/server/META-INF/libraries', '.cache/server/libraries')

# Deobfuscate Minecraft
import mapping
def deobfuscate (projectjson, properties):
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    mpdl = cljson ['downloads'] ['client_mappings']
    download_resource (mpdl ['url'], 'client.txt', True, mpdl ['sha1'])
    mapping.convert_mappings ('.cache/client.txt', '.cache/client.tsrg', True)
    syswrap ('java -jar libs/specialsource.jar -i .cache/client_official.jar -o .cache/client.jar -m .cache/client.tsrg')
    mpdl = cljson ['downloads'] ['server_mappings']
    download_resource (mpdl ['url'], 'server.txt', True, mpdl ['sha1'])
    mapping.convert_mappings ('.cache/server.txt', '.cache/server.tsrg', True)
    ver = properties ['minecraft_version']
    syswrap (f'java -jar libs/specialsource.jar -i .cache/server/META-INF/versions/{ver}/server-{ver}.jar -o .cache/server/server.jar -m .cache/server.tsrg')

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
    libroot = os.path.expanduser ('~/.berry/libraries/')
    mkrecursive (libroot)
    for lib in cljson ['libraries']:
        name = lib ['name'] .split (':')
        if len (name) == 4: # Natives
            if name [3] in ['natives-macos', 'natives-windows-arm64', 'linux-aarch_64']: continue # We do not support these platforms now; wait for future updates
            rules = lib ['rules']
            if not check_rule (rules): continue
        artifact = lib ['downloads'] ['artifact']
        pth = f'{libroot}{artifact ["path"]}'
        pd = pth.rsplit ('/', 1) [0]
        mkrecursive (pd)
        if download_resource (artifact ['url'], pth, True, artifact ['sha1'], root=''):
            print (f'Successfully downloaded {lib ["name"]}')
        else:
            print (f'{lib ["name"]} already exists. Skipping.')

# Get jars path for libraries
def get_libjars ():
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    libroot = os.path.expanduser ('~/.berry/libraries/')
    jars = []
    for lib in cljson ['libraries']:
        name = lib ['name'] .split (':')
        if len (name) == 4: # Natives
            if name [3] in ['natives-macos', 'natives-windows-arm64', 'linux-aarch_64']: continue # We do not support these platforms now; wait for future updates
            rules = lib ['rules']
            if not check_rule (rules): continue
        artifact = lib ['downloads'] ['artifact']
        pth = f'{libroot}{artifact ["path"]}'
        jars.append (pth)
    return jars

def cp_replace (projectjson, properties, ct):
    if ct == 'CPS_GAME_LIBS': return os.pathsep.join (get_libjars ())
    raise NotImplementedError

# Download assets
def download_assets (projectjson, properties):
    hb = os.path.expanduser ('~')
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    mkrecursive ('~/.berry/assets/objects/')
    mkrecursive ('~/.berry/assets/indexes/')
    ai = cljson ['assetIndex']
    download_resource (ai ['url'], f'assets/indexes/{ai["id"]}.json', True, ai ['sha1'], root=f'{hb}/.berry/')
    index = open (f'{hb}/.berry/assets/indexes/{ai["id"]}.json')
    indexjson = json.load (index)
    index.close ()
    s = '0123456789abcdef'
    for i in s:
        for j in s:
            if not os.path.exists (f'{hb}/.berry/assets/objects/{i}{j}'):
                mkrecursive (f'~/.berry/assets/objects/{i}{j}')
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

# Download bundled jars (deprecated, now you should use external libraries)
processors = {}
def download_bundled (projectjson, properties):
    li = projectjson ['bundled_libs']
    if os.path.exists ('runtime'): shutil.rmtree ('runtime')
    if os.path.exists ('.cache/runtime'): shutil.rmtree ('.cache/runtime')
    os.mkdir ('runtime'); os.mkdir ('.cache/runtime')
    for ns in li:
        proc = processors [ns]
        for i in li [ns]:
            i = re.sub ('\\$\\{([A-Za-z0-9_]+)\\}', lambda m: properties [m.group (1)], i)
            print ('Downloading from', i)
            download_resource (i, 'runtime/' + i.split ('/') [-1], False)
            proc (ns, i)
    if os.path.exists ('.cache/bundles'): os.remove ('.cache/bundles')
    f = open ('.cache/bundles', 'w')
    for i in os.listdir ('runtime'): f.write (f'bundled/{i}\n')
    f.close ()

# Parse external libraries
chli = [chr (i) for i in range (ord ('a'), ord ('z') + 1)]
def parse_external_libraries (projectjson, properties):
    if os.path.exists ('.cache/extlibs'): shutil.rmtree ('.cache/extlibs')
    os.mkdir ('.cache/extlibs')
    extlibs = projectjson.get ('external_libraries')
    if extlibs is None: return
    # Download them first
    urm = {}
    for url in extlibs ['urls']:
        url = re.sub ('\\$\\{([A-Za-z0-9_]+)\\}', lambda m: properties [m.group (1)], url)
        r = ''.join ([random.choice (chli) for i in range (32)])
        urm [r] = url
        print ('Downloading library from', url)
        download_resource (url, f'extlibs/{r}.jar', False)
    # Then, calculate hash
    lih = []
    for r in urm:
        with open (f'.cache/extlibs/{r}.jar', 'rb') as f:
            h = hashlib.sha1 (f.read ()) .hexdigest ()
        lih.append ((h, urm [r]))
    # Generate code
    gen = extlibs ['generate']
    with open (gen ['path'], 'w') as j:
        cls: str = gen ['class']
        pkg, cls = cls.rsplit ('.', 1)
        j.write (f'package {pkg};\n\n')
        j.write ('import java.net.URI;\n')
        j.write ('import java.net.MalformedURLException;\nimport java.net.URISyntaxException;\n\n')
        j.write ('import static berry.loader.BerryLoader.libraries;\n\n')
        j.write (f'class {cls} {{\n')
        j.write ('    static void init() throws URISyntaxException, MalformedURLException {\n')
        for entry in lih:
            j.write (f'        libraries.put("{entry [0]}", new URI("{entry [1]}").toURL());\n')
        j.write ('    }\n}\n')
    # For development: META-INF/external_libraries.json
    js = {}
    for entry in lih: js [entry [0]] = entry [1]
    with open ('.cache/external_libraries.json', 'w') as f:
        json.dump (js, f)

def getpaths ():
    return (
        ['.cache/client.jar', '.cache/server/server.jar'],
        ['.cache/bundled/', '.cache/berry/', '.cache/extramods/', 'runtime/', '.cache/extlibs/', 'extralibs/', 'libs/', os.path.expanduser ('~/.berry/libraries/')]
    )

# Setup Intellij Workspace
def setup_intellij (projectjson, properties):
    f = open (os.getcwd () .strip (os.sep) .split (os.sep) [-1] + '.iml', 'w')
    f.write (
        '''
        <?xml version="1.0" encoding="UTF-8"?>
        <module type="JAVA_MODULE" version="4">
        <component name="NewModuleRootManager" inherit-compiler-output="true">
        <exclude-output />
        <sourceFolder url="file://$MODULE_DIR$/src" isTestSource="false" />
        <content url="file://$MODULE_DIR$">
        '''
    )
    for name in projectjson ['packages']:
        f.write (f'<sourceFolder url="file://$MODULE_DIR$/src/{name}" isTestSource="false" />')
    f.write (
        '''
        </content>
        <orderEntry type="inheritedJdk" />
        <orderEntry type="sourceFolder" forTests="false" />
        '''
    )
    p = getpaths ()
    for i in p [0]:
        f.write (
            f'''
            <orderEntry type="sourceFolder" forTests="false" />
            <orderEntry type="module-library">
            <library>
            <CLASSES>
            <root url="jar://$MODULE_DIR$/{i}!/" />
            </CLASSES>
            <JAVADOC />
            <SOURCES />
            </library>
            </orderEntry>
            '''
        )
    for i in p [1]:
        i = i.rstrip ('/')
        f.write (
            f'''
            <orderEntry type="module-library">
            <library>
            <CLASSES>
            <root url="file://$MODULE_DIR$/{i}" />
            </CLASSES>
            <JAVADOC />
            <SOURCES />
            <jarDirectory url="file://$MODULE_DIR$/{i}" recursive="true" />
            </library>
            </orderEntry>
            '''
        )
    f.close ()

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
    p = getpaths ()
    for i in p [0]: s.add (i)
    for i in p [1] [:-1]: s.add (i + '*.jar')
    # hack impl
    for i in get_libjars (): s.add (i)
    stjson ['java.project.referencedLibraries'] = list (s)
    li = stjson.get ('java.project.sourcePaths', [])
    if 'srcpaths' in projectjson:
        s = projectjson ['srcpaths']
    else:
        s = []
        for name in projectjson ['packages']: s.append ('src/%s/' % name)
    stjson ['java.project.sourcePaths'] = s
    f = open ('.vscode/settings.json', 'w')
    json.dump (stjson, f)
    f.close ()

def parsemodbundle (loc):
    zipf = zipfile.ZipFile (loc)
    try:
        bf = zipf.open ('META-INF/bundled_jars', 'r')
        for i in bf:
            i = i.strip () .decode ()
            zipf.extract (i, '.cache/')
    except Exception: pass

# Setup Berry
# Either download (for mods) or just setup (for loader dev)
def setup_berry (projectjson, properties):
    bv = properties.get ('berry_version')
    mkrecursive ('.cache/berry')
    if bv is not None:
        li = ['agent', 'loader', 'builtins']
        for l in li:
            name = l + '.jar'
            url = properties ['berry_repo'] .format (version=bv, jarname=l)
            getfile (url, name, f'.cache/berry/{name}')
        if not os.path.exists ('.cache/bundled'): os.mkdir ('.cache/bundled')
        for i in os.listdir ('.cache/bundled'): os.remove ('.cache/bundled/' + i)
        parsemodbundle ('.cache/berry/builtins.jar')
        if os.path.exists ('.cache/game/mods/builtins.jar'): os.remove ('.cache/game/mods/builtins.jar')
        mkrecursive ('.cache/game/mods')
        mkrecursive ('.cache/extramods')
        fcopy ('.cache/berry/builtins.jar', '.cache/extramods/builtins.jar')
        jar_extlibs ('builtins.jar')
    else:
        if os.path.exists ('.cache/berry/loader.jar'): os.remove ('.cache/berry/loader.jar')
        if os.path.exists ('.cache/berry/agent.jar'): os.remove ('.cache/berry/agent.jar')
        os.rename ('output/loader.jar', '.cache/berry/loader.jar')
        os.rename ('output/agent.jar', '.cache/berry/agent.jar')

# Parse external libraries from jar mods
def jar_extlibs (mod: str):
    zf = zipfile.ZipFile (f'.cache/extramods/{mod}')
    try:
        ef = zf.open('META-INF/external_libraries.json', 'r')
        js = json.load (ef)
        ef.close()
        if not os.path.isdir ('extralibs'): os.mkdir ('extralibs')
        for h in js: download_resource (js [h], h + '.jar', False, h, root='extralibs/')
    except KeyError: pass

# Parse extra mods
def extramods (projectjson, properties):
    if not os.path.exists ('.cache/extramods'): os.mkdir ('.cache/extramods')
    emods = projectjson.get ('extramods')
    if emods is None: return
    for mod in emods:
        url = emods [mod] .format (**properties)
        getfile (url, mod, f'.cache/extramods/{mod}')
        jar_extlibs (mod)

# Run Minecraft Client
def run_client (projectjson, properties):
    if os.path.exists ('.cache/game/mods'): shutil.rmtree ('.cache/game/mods')
    mkrecursive ('.cache/game/mods')
    if os.path.exists ('.cache/extramods'):
        for fn in os.listdir ('.cache/extramods'):
            fcopy (f'.cache/extramods/{fn}', f'.cache/game/mods/{fn}')
    mn = projectjson ['output_mod']
    fcopy (f'output/{mn}.jar', f'.cache/game/mods/{mn}.jar')
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    cps = []
    libroot = os.path.expanduser ('~/.berry/libraries/')
    for lib in cljson ['libraries']:
        name = lib ['name'] .split (':')
        if len (name) == 4: # Natives
            if name [3] in ['natives-macos', 'natives-windows-arm64', 'linux-aarch_64']: continue # We do not support these platforms now; wait for future updates
            rules = lib ['rules']
            if not check_rule (rules): continue
        artifact = lib ['downloads'] ['artifact']
        pth = f'{libroot}{artifact ["path"]}'
        cps.append (pth)
    cps.append ('../client.jar')
    cps = os.pathsep.join (cps)
    mkrecursive ('.cache/natives')
    vars = {
        'classpath': '../berry/loader.jar',
        'natives_directory': '../natives/',
        'launcher_name': '"BML Test"',
        'launcher_version': '1.0.0',
        'auth_player_name': properties.get ('player_name', 'Dev'),
        'version_name': properties.get ('version_name', properties.get ('minecraft_version', 'unknown')),
        'game_directory': './',
        'assets_root': os.path.expanduser ('~/.berry/assets/'),
        'assets_index_name': cljson ['assetIndex'] ['id'],
        'auth_uuid': '0123456789abcdef0123456789abcdef',
        'auth_access_token': 'aa',
        'clientid': 'berry',
        'auth_xuid': 'bb',
        'user_type': 'msa',
        'version_type': 'Berry'
    }
    # Is that only for skin?
    if os.path.isfile ('localinfo.json'):
        auth = json.load (open ('localinfo.json'))
        try:
            vars ['auth_player_name'] = auth ['auth_name']
            vars ['auth_uuid'] = auth ['auth_uuid']
        except KeyError: pass
    args = cljson ['arguments']
    jvmargs = ['-javaagent:../berry/agent.jar', '-Dberry.indev=true', '-Djava.system.class.loader=berry.loader.BerryClassLoader', '-Dberry.cps='+cps]
    for jvmarg in args ['jvm']:
        if isinstance (jvmarg, str):
            jvmargs.append (re.sub ('\\$\\{([A-Za-z0-9_]+)\\}', lambda m: vars [m.group (1)], jvmarg))
        else:
            rules = jvmarg ['rules']
            if check_rule (rules): jvmargs.append (jvmarg ['value'])
    gameargs = [cljson ['mainClass']]
    for gamearg in args ['game']:
        if isinstance (gamearg, str):
            gameargs.append (re.sub ('\\$\\{([A-Za-z0-9_]+)\\}', lambda m: vars [m.group (1)], gamearg))
    os.chdir ('.cache/game/')
    syswrap (['java'] + jvmargs + ['berry.loader.BerryLoader'] + gameargs)
    os.chdir ('../../')

# Run Minecraft Server
# TODO: fx this
def run_server (projectjson, properties):
    if os.path.exists ('.cache/server/mods'): shutil.rmtree ('.cache/server/mods')
    mkrecursive ('.cache/server/mods')
    if os.path.exists ('.cache/extramods'):
        for fn in os.listdir ('.cache/extramods'):
            fcopy (f'.cache/extramods/{fn}', f'.cache/server/mods/{fn}')
    mn = projectjson ['output_mod']
    fcopy (f'output/{mn}.jar', f'.cache/server/mods/{mn}.jar')
    cps = open ('.cache/server/META-INF/classpath-joined') .read () .strip () .split (';')
    for i in range (len (cps)):
        if cps [i] .split ('/') [-1] .startswith ('asm'):
            cps.pop (i)
            break
    cps += ['server.jar']
    mc = open ('.cache/server/META-INF/main-class') .read () .strip ()
    os.chdir ('.cache/server/')
    syswrap ([
        'java', '-javaagent:../berry/agent.jar', '-Dberry.side=SERVER', '-Dberry.indev=true', '-Djava.system.class.loader=berry.loader.BerryClassLoader',
        f'-Dberry.cps={os.pathsep.join (cps)}',
        '-cp', '../berry/loader.jar',
        'berry.loader.BerryLoader', mc, '--nogui'
    ])
    os.chdir ('../../')

def build_resources (projectjson, properties):
    if not os.path.exists ('output'): os.mkdir ('output')
    # Scan packs
    packs = os.listdir ('.cache/game/resourcepacks')
    # For each pack
    for pack in packs:
        if pack.endswith ('.zip'): pass
        zip = zipfile.ZipFile (f'output/resourcepack_{pack}.zip', 'w')
        # For each file in the pack
        rd = f'.cache/game/resourcepacks/{pack}'
        for root, dirs, files in os.walk (rd):
            for file in files:
                path = os.path.join (root, file)
                zip.write (path, os.path.relpath (path, rd))
        zip.close ()

def build_datapacks (projectjson, properties):
    if not os.path.exists ('output'): os.mkdir ('output')
    # Scan packs. The world name only supports 'world' for now
    packs = os.listdir ('.cache/server/world/datapacks')
    # For each pack
    for pack in packs:
        if pack.endswith ('.zip'): pass
        zip = zipfile.ZipFile (f'output/datapack_{pack}.zip', 'w')
        # For each file in the pack
        rd = f'.cache/server/world/datapacks/{pack}'
        for root, dirs, files in os.walk (rd):
            for file in files:
                path = os.path.join (root, file)
                zip.write (path, os.path.relpath (path, rd))
        zip.close ()

def localdist (projectjson, properties):
    lcd = projectjson.get ('localdist')
    for fn in lcd:
        conf.upload (lcd [fn], fn)
