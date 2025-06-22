// Copyright (C) 2025 VoidSingularity

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or (at
// your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package berry.loader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class JarContainer {
    private final JarFile file;
    private final String fp;
    public JarContainer (File jarfile) throws IOException {
        this.fp = jarfile.getAbsolutePath ();
        this.file = new JarFile (jarfile);
    }
    public String filepath () {
        return this.fp;
    }
    public JarFile file () {
        return this.file;
    }
    public static final Map <String, JarContainer> containers = new HashMap <> ();
}
