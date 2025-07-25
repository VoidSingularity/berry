package berry.api.asm;

import java.io.IOException;

import berry.loader.BerryClassTransformer.ByteCodeTransformer;

public interface ClassFileHandler {
    public void handle (ClassFile classfile) throws IOException;
    default ByteCodeTransformer transformer (ClassFileHandler... handlers) {
        return (loader, name, clazz, domain, buffer) -> {
            if (buffer == null) return null;
            var cf = new ClassFile (buffer);
            for (var handler : handlers) handler.handle (cf);
            return cf.get ();
        };
    }
}
