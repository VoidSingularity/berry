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
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import berry.utils.Graph;

public class BerryClassTransformer implements ClassFileTransformer {
    public interface ByteCodeTransformer {
        byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] code) throws IOException;
    }
    public static class GraphTransformer implements ByteCodeTransformer {
        public final Graph graph = new Graph ();
        public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buffer) {
            for (var consumer : graph.sorted ()) {
                try {
                    var con = (ByteCodeTransformer) (graph.getVertices () .get (consumer) .getValue ());
                    byte[] next = con.transform (loader, name, clazz, domain, buffer);
                    if (next != null) buffer = next;
                } catch (IOException e) {
                    System.err.printf ("[JA] Failed to remap class %s using %s%n", name, consumer.getClass () .getName ());
                }
            }
            return buffer;
        }
    }
    // All in this
    public final GraphTransformer all = new GraphTransformer ();
    // Remapper
    // Remappers should accept nullable loader, clazz and domain
    public final GraphTransformer remapper = new GraphTransformer ();
    // Monitor
    // Monitors must be read-only
    public final List <ByteCodeTransformer> monitor = new ArrayList <> ();
    // Instance
    private static BerryClassTransformer instance;
    public static BerryClassTransformer instance () {
        return instance;
    }
    public BerryClassTransformer () {
        instance = this;
        var vremap = new Graph.Vertex ("berry::remap", this.remapper);
        this.all.graph.addVertex (vremap);
        ByteCodeTransformer moni = (loader, name, clazz, domain, buffer) -> {
            for (ByteCodeTransformer trans : monitor) trans.transform (loader, name, clazz, domain, buffer);
            return buffer;
        };
        var vmonitor = new Graph.Vertex ("berry::monitor", moni);
        this.all.graph.addVertex (vmonitor);
        this.all.graph.addEdge (null, vremap, vmonitor, null);
    }
    public final Set <String> trans_blacklist = new HashSet <> ();
    public static final Set <String> blacklist_prefix = new HashSet <> ();
    static {
        blacklist_prefix.addAll (Set.of ("java", "jdk", "sun", "berry/utils/"));
    }
    public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buffer) {
        // DO NOT TRANSFORM THESE!
        if (name.startsWith ("java") || name.startsWith ("jdk") || name.startsWith ("sun")) return buffer;
        // THEY SHOULD NOT BE TRANSFORMED!
        for (var prefix : blacklist_prefix)
            if (name.startsWith (prefix))
                return buffer;
        // Blacklist. Do not transform them.
        if (trans_blacklist.contains (name)) {
            trans_blacklist.remove (name);
            return buffer;
        }
        return all.transform (loader, name, clazz, domain, buffer);
    }
}
