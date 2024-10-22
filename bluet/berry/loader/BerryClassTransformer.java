package bluet.berry.loader;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import bluet.berry.asm.ClassFile;

public class BerryClassTransformer implements ClassFileTransformer {
    public static interface ByteCodeTransformer {
        public void transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, ClassFile file) throws IOException;
    }
    public final List <ByteCodeTransformer> all = new ArrayList <> ();
    private static BerryClassTransformer instance;
    public static BerryClassTransformer instance () {
        return instance;
    }
    private final Instrumentation inst;
    public Instrumentation instrumentation () {
        return this.inst;
    }
    public BerryClassTransformer (Instrumentation inst) {
        instance = this;
        this.inst = inst;
    }
    public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buffer) {
        try {
            ClassFile file = new ClassFile (buffer);
            for (var consumer : all) consumer.transform (loader, name, clazz, domain, file);
            return file.get ();
        } catch (IOException e) {
            return null;
        }
    }
}
