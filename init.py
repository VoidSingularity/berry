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

# Change the source if you need
source = 'https://azfs.pages.dev/berry-dist/0.0.0+2025012701/'

li = [
    'build.py',
    'mapping.py',
    'project_template.py'
]

from urllib.request import urlopen, Request

for i in li:
    req = Request (source + i)
    req.add_header ('User-Agent', 'BerryInit')
    u = urlopen (req)
    f = open (i, 'wb')
    f.write (u.read ())
    u.close (); f.close ()
