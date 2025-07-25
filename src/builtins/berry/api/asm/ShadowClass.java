package berry.api.asm;

import java.io.IOException;
import java.util.Scanner;

import berry.loader.JarProcessor;
public class ShadowClass implements JarProcessor, ClassFileHandler {
    private final String oPrefix, nPrefix;
    private final String soPrefix, snPrefix;
    // Use a.b.C instead of a/b/C
    public ShadowClass (String oPrefix, String nPrefix) {
        this.oPrefix = oPrefix;
        this.nPrefix = nPrefix;
        soPrefix = oPrefix.replace ('.', '/');
        snPrefix = nPrefix.replace ('.', '/');
    }
    public void handle (ClassFile cf) {
        String name = cf.cls_name (cf.thisClass);
        if (!name.startsWith (snPrefix)) {
            for (var con : cf.constants) {
                if (con != null && con.type == 1) {
                    String str = new String (con.data);
                    str = str.replace (oPrefix, nPrefix) .replace (soPrefix, snPrefix);
                    con.data = str.getBytes ();
                }
            }
        }
    }
    public byte[] transform (String name, byte[] buf) throws IOException {
        ClassFile cf = new ClassFile (buf);
        handle (cf);
        return cf.get ();
    }
    public EntryInfo process (EntryInfo orig) throws IOException {
        String name = orig.name ();
        byte[] data = orig.data ();
        if (name.endsWith (".class")) {
            data = transform (name.split ("\\.") [0], data);
        }
        if (name.startsWith ("META-INF/services/")) {
            Scanner scanner = new Scanner (new String (data));
            StringBuilder builder = new StringBuilder ();
            while (scanner.hasNextLine ()) {
                String line = scanner.nextLine ();
                if (line.startsWith (oPrefix)) line = nPrefix + line.substring (oPrefix.length (), line.length ());
                builder.append (line); builder.append ('\n');
            }
            scanner.close ();
            data = builder.toString () .getBytes ();
        }
        if (name.startsWith (soPrefix)) name = name.replace (soPrefix, snPrefix);
        if (name.startsWith ("META-INF/services/" + oPrefix)) name = name.replace (oPrefix, nPrefix);
        return new EntryInfo (name, data);
    }
    // This method is for development use; neven run in-game
    // <oPrefix> <nPrefix> <input> <output>
    public static void main (String... args) throws IOException {
        assert args.length >= 4;
        var shadow = new ShadowClass (args [0], args [1]);
        JarProcessor.process (shadow, args [2], args [3]);
    }
}
