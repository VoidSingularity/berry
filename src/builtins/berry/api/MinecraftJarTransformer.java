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

package berry.api;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import berry.loader.BerryClassLoader;
import berry.loader.BerryLoader;
import berry.utils.Graph;

public class MinecraftJarTransformer {
    public static Graph transformers = new Graph ();
    private static void transform (ZipFile file, ZipEntry entry, ZipOutputStream out) {
        try {
            String name = entry.getName () .toLowerCase ();
            if (name.endsWith (".rsa") || name.endsWith (".sf")) return;
            var is = file.getInputStream (entry);
            var all = is.readAllBytes ();
            is.close ();
            for (var tr : transformers.sorted ()) {
                try {
                    @SuppressWarnings("unchecked")
                    var vertex = (BiFunction <String, byte[], byte[]>) transformers.getVertices () .get (tr) .getValue ();
                    all = vertex.apply (entry.getName (), all);
                } catch (ClassCastException e) {
                    throw new RuntimeException (e);
                }
            }
            out.putNextEntry (entry);
            out.write (all);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    public static void transform () {
        String orig = System.getProperty ("berry.mcjar");
        String trans = BerryLoader.getGameDirectory () + "__transformed.jar";
        try {
            ZipFile file = new ZipFile (orig);
            FileOutputStream os = new FileOutputStream (trans);
            ZipOutputStream out = new ZipOutputStream (os);
            file.entries () .asIterator () .forEachRemaining (entry -> transform (file, entry, out));
            out.close (); os.close ();
            BerryClassLoader.getInstance () .appendToClassPathForInstrumentation (trans);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
        System.out.println ("[BERRY] Minecraft jar transformed");
    }
}
