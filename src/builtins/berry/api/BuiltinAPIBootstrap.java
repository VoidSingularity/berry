package berry.api;

import java.io.IOException;
import java.util.List;

import berry.api.mixins.MixinInitialize;
import berry.loader.JarContainer;

public class BuiltinAPIBootstrap {
    public static final List <String> bundles = List.of (
        "asm-9.7.1.jar",
        "asm-analysis-9.7.1.jar",
        "asm-commons-9.7.1.jar",
        "asm-tree-9.7.1.jar",
        "asm-util-9.7.1.jar",
        "sponge-mixin-0.15.4+mixin.0.8.7.jar"
    );
    public static void initialize (JarContainer container, String[] argv) throws IOException {
        // Load bundled
        // Find the game root dir
        String dir = null;
        int i;
        for (i=0; i<argv.length-1; i++)
        if (argv [i] .equals ("--gameDir"))
        dir = argv [i+1];
        // load
        for (String bundle : bundles) BundledJar.addBundled (dir, container, "bundled/" + bundle);
        // Mixin bootstrap
        MixinInitialize.initialize ();
    }
}
