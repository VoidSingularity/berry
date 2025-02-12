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

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import berry.utils.Graph;

public class BerryClassTransformer implements ClassFileTransformer {
    public static interface ByteCodeTransformer {
        public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] code) throws IOException;
    }
    public final Graph all = new Graph ();
    private static BerryClassTransformer instance;
    public static BerryClassTransformer instance () {
        return instance;
    }
    private final Instrumentation inst;
    public static Instrumentation instrumentation () {
        return instance.inst;
    }
    public BerryClassTransformer (Instrumentation inst) {
        instance = this;
        this.inst = inst;
        ByteCodeTransformer rmp = (loader, name, clazz, domain, code) -> this.remap (loader, name, clazz, domain, code);
        this.all.addVertex (new Graph.Vertex ("berry::remap", rmp));
    }
    public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buffer) {
        // DO NOT TRANSFORM THESE!
        if (name.startsWith ("java") || name.startsWith ("jdk") || name.startsWith ("sun")) return null;
        // THEY SHOULD NOT BE TRANSFORMED!
        if (name.startsWith ("berry/utils")) return null;
        for (var consumer : all.sorted ()) {
            try {
                var con = (ByteCodeTransformer) (all.getVertices () .get (consumer) .getValue ());
                buffer = con.transform (loader, name, clazz, domain, buffer);
            } catch (IOException e) {
                System.err.println (String.format ("[JA] Failed to transform class %s using %s", name, consumer.getClass () .getName ()));
            }
        }
        return buffer;
    }
    // Remapper
    // Remappers should accept nullable loader, clazz and domain
    public final Graph remapper = new Graph ();
    public byte[] remap (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buffer) {
        for (var consumer : remapper.sorted ()) {
            try {
                var con = (ByteCodeTransformer) (remapper.getVertices () .get (consumer) .getValue ());
                buffer = con.transform (loader, name, clazz, domain, buffer);
            } catch (IOException e) {
                System.err.println (String.format ("[JA] Failed to remap class %s using %s", name, consumer.getClass () .getName ()));
            }
        }
        return buffer;
    }
}
