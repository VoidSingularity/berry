package berry.loader;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class BerryClassTransformer implements ClassFileTransformer {
    public static interface ByteCodeTransformer {
        public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] code) throws IOException;
    }
    public final List <ByteCodeTransformer> all = new ArrayList <> ();
    private static BerryClassTransformer instance;
    public static BerryClassTransformer instance () {
        return instance;
    }
    private final Instrumentation inst;
    public static Instrumentation instrumentation () {
        return instance.inst;
    }
    public BerryClassTransformer (Instrumentation inst) {
        instance = this;
        this.inst = inst;
    }
    public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buffer) {
        // DO NOT TRANSFORM THESE!
        if (name.startsWith ("java") || name.startsWith ("jdk") || name.startsWith ("sun")) return null;
        for (var consumer : all) {
            try {
                buffer = consumer.transform (loader, name, clazz, domain, buffer);
            } catch (IOException e) {
                System.err.println (String.format ("[JA] Failed to transform class %s using %s", name, consumer.getClass () .getName ()));
            }
        }
        return buffer;
    }
}
