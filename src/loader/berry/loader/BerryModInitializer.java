package berry.loader;

import berry.utils.Graph;

public interface BerryModInitializer {
    public void initialize (String[] argv);
    default void preinit (Graph graph, JarContainer jar, String name) {
        graph.addVertex (new Graph.Vertex (name));
    };
}
