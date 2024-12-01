package berry.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import berry.api.mixins.MixinInitialize;
import berry.loader.BerryLoader;
import berry.loader.BerryModInitializer;
import berry.loader.JarContainer;

public class BuiltinAPIBootstrap implements BerryModInitializer {
    public void initialize (String[] argv) {
        // Load bundled
        for (String jarname : JarContainer.containers.keySet ()) {
            JarContainer jar = JarContainer.containers.get (jarname);
            var file = jar.file ();
            var entry = file.getEntry ("META-INF/bundled_jars");
            if (entry != null) {
                try (InputStream is = file.getInputStream (entry); Scanner scanner = new Scanner (is)) {
                    while (scanner.hasNextLine ()) {
                        String line = scanner.nextLine () .strip ();
                        if (line.isEmpty ()) continue;
                        if (line.startsWith ("#")) continue;
                        BundledJar.addBundled (BerryLoader.getGameDirectory (), jar, line);
                    }
                } catch (IOException e) {
                    System.err.println ("[BERRY/BUILTIN] Unexpected failure while reading bundle info!");
                    e.printStackTrace ();
                }
            }
        }
        // Mixin bootstrap
        MixinInitialize.initialize ();
    }
}
