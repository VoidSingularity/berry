package berry.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import berry.utils.Graph.Vertex;

public class StringSorter {
    private List <String> sorted = null;
    public static record Rule (String first, String second) {}
    private final Set <Rule> rules = new HashSet <> ();
    private final Set <String> vals = new HashSet <> ();
    public void addRule (Rule rule) {
        sorted = null;
        rules.add (rule);
    }
    public void addRule (String first, String second) {
        sorted = null;
        rules.add (new Rule (first, second));
    }
    public void addValue (String value) {
        sorted = null;
        vals.add (value);
    }
    public List <String> sort () {
        if (sorted != null) return sorted;
        Graph G = new Graph ();
        for (var value : vals) G.addVertex (new Graph.Vertex (value));
        for (var rule : rules) {
            var vs = G.getVertices ();
            Vertex v1 = vs.get (rule.first),
                   v2 = vs.get (rule.second);
            if (v1 != null && v2 != null) G.addEdge (null, v1, v2, null);
        }
        return this.sorted = G.sorted ();
    }
}
