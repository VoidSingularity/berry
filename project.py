from project_template import *

# Build Installer
# Just merge three jars together!
def installer (projectjson, properties):
    zinst = zipfile.ZipFile ('output/_installer.jar', 'r')
    zjson = zipfile.ZipFile ('libs/json-20240303.jar', 'r')
    zdobf = zipfile.ZipFile ('libs/specialsource.jar', 'r')
    zout = zipfile.ZipFile ('output/installer.jar', 'w')
    # entries
    ent = set ()
    for i in zinst.filelist: ent.add (i.filename)
    for i in zjson.filelist: ent.add (i.filename)
    for i in zdobf.filelist: ent.add (i.filename)
    for i in ent:
        try: zin = zinst.open (i)
        except Exception:
            try: zin = zjson.open (i)
            except Exception: zin = zdobf.open (i)
        zou = zout.open (i, 'w')
        zou.write (zin.read ())
        zou.close ()
    zinst.close (); zjson.close (); zout.close ()
