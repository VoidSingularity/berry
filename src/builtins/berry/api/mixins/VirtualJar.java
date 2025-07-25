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

package berry.api.mixins;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import berry.loader.BerryLoader;

public class VirtualJar extends JarFile {
    static {
        try {
            File ef = new File (BerryLoader.getGameDirectory () + "empty.jar");
            if (! ef.exists ()) {
                OutputStream os = new FileOutputStream (ef);
                ZipOutputStream zos = new ZipOutputStream (os);
                zos.putNextEntry (new ZipEntry (MANIFEST_NAME));
                zos.write ("Manifest-Version: 1.0\n".getBytes ());
                zos.close ();
                os.close ();
            }
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    public static interface VirtualJarProvider {
        // The names might be confusing but I can't think of better ones LOL
        public boolean provides (String name);
        public byte[] provide (String name) throws IOException;
        public Enumeration <JarEntry> entries ();
    }
    private final VirtualJarProvider provider;
    public VirtualJar (VirtualJarProvider provider) throws IOException {
        super (new File (BerryLoader.getGameDirectory () + "empty.jar"));
        this.provider = provider;
    }
    @Override
    public ZipEntry getEntry (String name) {
        if (this.provider.provides (name)) return new ZipEntry (name);
        else return null;
    }
    @Override
    public Enumeration <JarEntry> entries () {
        var e = this.provider.entries ();
        if (e != null) return e;
        return super.entries ();
    }
    @Override
    public InputStream getInputStream (ZipEntry ze) throws IOException {
        String name = ze.getName ();
        if (this.provider.provides (name)) {
            return new ByteArrayInputStream (this.provider.provide (name));
        }
        else return null;
    }
}
