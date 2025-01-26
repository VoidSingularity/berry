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
