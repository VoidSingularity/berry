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

import hashlib, json, os, platform, re, shutil, urllib.request, xmlrpc.client, zipfile

def syswrap (cmd):
    print ('$', cmd)
    os.system (cmd)

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
    if not os.path.exists ('.cache/server'): os.mkdir ('.cache/server')
    if not os.path.exists ('.cache/server/mods'): os.mkdir ('.cache/server/mods')
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
processors = {}
def download_bundled (projectjson, properties):
    li = projectjson ['bundled_libs']
    if os.path.exists ('runtime'): shutil.rmtree ('runtime')
    if os.path.exists ('.cache/runtime'): shutil.rmtree ('.cache/runtime')
    os.mkdir ('runtime'); os.mkdir ('.cache/runtime')
    for ns in li:
        proc = processors [ns]
        for i in li [ns]:
            download_resource (i, 'runtime/' + i.split ('/') [-1], False)
            proc (ns, i)
    if os.path.exists ('.cache/bundles'): os.remove ('.cache/bundles')
    f = open ('.cache/bundles', 'w')
    for i in os.listdir ('runtime'): f.write (f'bundled/{i}\n')
    f.close ()

def getpaths ():
    return (
        ['.cache/client.jar', '.cache/server/server.jar'],
        ['.cache/libs/', '.cache/bundled/', '.cache/berry/', '.cache/extramods/', 'runtime/', 'libs/']
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
            <jarDirectory url="file://$MODULE_DIR$/{i}" recursive="false" />
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
    for i in p [1]: s.add (i + '*.jar')
    stjson ['java.project.referencedLibraries'] = list (s)
    li = stjson.get ('java.project.sourcePaths', [])
    s = set (li)
    for name in projectjson ['packages']: s.add ('src/%s/' % name)
    stjson ['java.project.sourcePaths'] = list (s)
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
    if not os.path.exists ('.cache'): os.mkdir ('.cache')
    if not os.path.exists ('.cache/berry'): os.mkdir ('.cache/berry')
    if bv is not None:
        li = ['agent', 'loader', 'builtins']
        srv = xmlrpc.client.ServerProxy ('http://localhost:19922')
        for l in li:
            try:
                bn = srv.fquery (l)
                if isinstance (bn, xmlrpc.client.Binary): bn = bn.data
                f = open (f'.cache/berry/{l}.jar', 'wb')
                f.write (bn)
                f.close ()
                if not os.path.exists (f'.cache/berry/{l}.localdist'): open (f'.cache/berry/{l}.localdist', 'w') .close ()
            except ConnectionRefusedError:
                # Download from releases
                if os.path.exists (f'.cache/berry/{l}.localdist'):
                    os.remove (f'.cache/berry/{l}.jar')
                    os.remove (f'.cache/berry/{l}.localdist')
                if os.path.exists (f'.cache/berry/{l}.jar'): continue
                url = properties ['berry_repo'] .format (version=bv, jarname=l)
                download_resource (url, f'berry/{l}.jar')
        if not os.path.exists ('.cache/bundled'): os.mkdir ('.cache/bundled')
        for i in os.listdir ('.cache/bundled'): os.remove ('.cache/bundled/' + i)
        parsemodbundle ('.cache/berry/builtins.jar')
        if os.path.exists ('.cache/game/mods/builtins.jar'): os.remove ('.cache/game/mods/builtins.jar')
        if not os.path.exists ('.cache/game'): os.mkdir ('.cache/game')
        if not os.path.exists ('.cache/game/mods'): os.mkdir ('.cache/game/mods')
        if not os.path.exists ('.cache/extramods'): os.mkdir ('.cache/extramods')
        fcopy ('.cache/berry/builtins.jar', '.cache/extramods/builtins.jar')
    else:
        if os.path.exists ('.cache/berry/loader.jar'): os.remove ('.cache/berry/loader.jar')
        if os.path.exists ('.cache/berry/agent.jar'): os.remove ('.cache/berry/agent.jar')
        os.rename ('output/loader.jar', '.cache/berry/loader.jar')
        os.rename ('output/agent.jar', '.cache/berry/agent.jar')

# Run Minecraft Client
def run_client (projectjson, properties):
    if os.path.exists ('.cache/game/mods'): shutil.rmtree ('.cache/game/mods')
    if not os.path.exists ('.cache/game/mods'): os.mkdir ('.cache/game/mods')
    if os.path.exists ('.cache/extramods'):
        for fn in os.listdir ('.cache/extramods'):
            fcopy (f'.cache/extramods/{fn}', f'.cache/game/mods/{fn}')
    mn = projectjson ['output_mod']
    fcopy (f'output/{mn}.jar', f'.cache/game/mods/{mn}.jar')
    cl = open ('.cache/client.json')
    cljson = json.load (cl)
    cl.close ()
    ld = os.listdir ('.cache/libs/')
    cps = os.pathsep.join ([f'../libs/{i}' for i in ld] + ['../client.jar', '../berry/loader.jar'])
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
    jvmargs = ['-javaagent:../berry/agent.jar', '-Dberry.indev=true']
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
    os.chdir ('.cache/game/')
    syswrap (f'java {" ".join (jvmargs)} berry.loader.BerryLoader {" ".join (gameargs)}')
    os.chdir ('../../')

# Run Minecraft Server
def run_server (projectjson, properties):
    if not os.path.exists ('.cache/server'): os.mkdir ('.cache/server')
    if os.path.exists ('.cache/server/mods'): shutil.rmtree ('.cache/server/mods')
    if not os.path.exists ('.cache/server/mods'): os.mkdir ('.cache/server/mods')
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
    cps += ['../berry/loader.jar', 'server.jar']
    mc = open ('.cache/server/META-INF/main-class') .read () .strip ()
    os.chdir ('.cache/server/')
    syswrap (
        'java -javaagent:../berry/agent.jar -Dberry.side=SERVER -Dberry.indev=true'
        f' -cp {os.pathsep.join (cps)}'
        f' berry.loader.BerryLoader {mc} --nogui'
    )
    os.chdir ('../../')
