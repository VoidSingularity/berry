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

package berry.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// G = {V, E}
public class Graph {
    public static class Vertex {
        final Set <Object> in, out;
        final String key; final Object value;
        public Vertex (String key) {
            this.key = key; this.value = null;
            in = new HashSet <> ();
            out = new HashSet <> ();
        }
        public Vertex (String key, Object value) {
            this.key = key; this.value = value;
            in = new HashSet <> ();
            out = new HashSet <> ();
        }
        public String getKey () { return this.key; }
        public Object getValue () { return this.value; }
    }
    public static record Edge (Vertex source, Vertex destination, Object weight) {}
    private Map <String, Vertex> vertices = new HashMap <> ();
    private Map <Object, Edge> edges = new HashMap <> ();
    private boolean cached = false;
    private List <String> cache;
    public void addVertex (Vertex v) {
        vertices.put (v.key, v);
        cached = false;
    }
    public void addEdge (Object key, Vertex src, Vertex dst, Object weight) {
        // As edge identifiers are not so commonly used, we should allow key to be null
        if (key == null) key = UUID.randomUUID ();
        Edge n = new Edge (src, dst, weight);
        edges.put (key, n);
        src.out.add (key);
        dst.in.add (key);
        cached = false;
    }
    public Map <String, Vertex> getVertices () { return this.vertices; }
    public Map <Object, Edge> getEdges () { return this.edges; }
    public List <String> sorted () {
        if (cached) return cache;
        List <String> ret = new ArrayList <> ();
        Map <String, Integer> ins = new HashMap <> ();
        Set <String> vs = new HashSet <> ();
        for (String o : this.vertices.keySet ()) {
            ins.put (o, this.vertices.get (o) .in.size ());
            if (ins.get (o) == 0) vs.add (o);
        }
        while (! vs.isEmpty ()) {
            String v = vs.iterator () .next ();
            ret.add (v); vs.remove (v);
            for (Object t : this.vertices.get (v) .out) {
                Edge e = this.edges.get (t);
                String s = e.destination.key;
                ins.put (s, ins.get (s) - 1);
                if (ins.get (s) == 0) vs.add (s);
            }
        }
        if (ret.size () != ins.size ()) cache = null;
        else cache = List.copyOf (ret);
        cached = true;
        return cache;
    }
}
