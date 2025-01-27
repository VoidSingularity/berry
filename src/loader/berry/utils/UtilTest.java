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

public class UtilTest {
    // Test
    public static void main (String[] args) {
        testgraph ();
    }
    public static void testgraph () {
        Graph G = new Graph ();
        Graph.Vertex v1, v2, v3, v4;
        v1 = new Graph.Vertex ("v1"); v2 = new Graph.Vertex ("v2");
        v3 = new Graph.Vertex ("v3"); v4 = new Graph.Vertex ("v4");
        G.addVertex (v1); G.addVertex (v2);
        G.addVertex (v3); G.addVertex (v4);
        G.addEdge (null, v2, v1, null);
        G.addEdge (null, v2, v3, null);
        G.addEdge (null, v3, v1, null);
        G.addEdge (null, v4, v3, null);
        var output = G.sorted ();
        for (var s : output) System.out.print (((String) s) + " "); System.out.println (";");
        output = G.sorted ();
        for (var s : output) System.out.print (((String) s) + " "); System.out.println (";");
    }
}
