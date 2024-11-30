package berry.api;

import java.io.IOException;
import java.util.List;

import berry.api.mixins.MixinInitialize;
import berry.loader.BerryModInitializer;
import berry.loader.JarContainer;
import berry.utils.Graph;

public class BuiltinAPIBootstrap implements BerryModInitializer {
    public static final List <String> bundles = List.of (
        "asm-9.7.1.jar",
        "asm-analysis-9.7.1.jar",
        "asm-commons-9.7.1.jar",
        "asm-tree-9.7.1.jar",
        "asm-util-9.7.1.jar",
        "sponge-mixin-0.15.4+mixin.0.8.7.jar",
        "javax.annotation-api-1.3.2.jar",
        "annotations-26.0.1.jar",
        "mixinextras-common-0.4.1.jar",
        "org.sat4j.core-2.3.1.jar",
        "org.sat4j.pb-2.3.1.jar"
    );
    private JarContainer container = null;
    public void preinit (Graph G, JarContainer jar, String name) {
        G.addVertex (new Graph.Vertex (name));
        this.container = jar;
    }
    public void initialize (String[] argv) {
        // Load bundled
        // Find the game root dir
        String dir = null;
        int i;
        for (i=0; i<argv.length-1; i++)
        if (argv [i] .equals ("--gameDir"))
        dir = argv [i+1];
        // load
        for (String bundle : bundles)
        try { BundledJar.addBundled (dir, container, "bundled/" + bundle); }
        catch (IOException e) { throw new RuntimeException (e); }
        // Mixin bootstrap
        MixinInitialize.initialize ();
    }
}
