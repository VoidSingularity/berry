package berry.loader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class JarContainer {
    private final JarFile file;
    public JarContainer (File jarfile) throws IOException {
        this.file = new JarFile (jarfile);
    }
    public JarFile file () {
        return this.file;
    }
    public static final Map <String, JarContainer> containers = new HashMap <> ();
}
