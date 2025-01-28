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

processors ['builtins'] = lambda ns, i: os.rename ('.cache/runtime/' + i.split ('/') [-1], f'runtime/{ns}-{i.split("/")[-1]}')
