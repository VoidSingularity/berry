package bluet.berry.berrytool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import bluet.berry.asm.ClassFile;

public class ClassAnalyzer {
    public static void main (String[] args) throws IOException {
        for (String arg : args) {
            if (arg.endsWith (".jar")) {
                try {
                    JarFile file = new JarFile (arg);
                    Enumeration <JarEntry> entries = file.entries ();
                    while (entries.hasMoreElements ()) {
                        JarEntry entry = entries.nextElement ();
                        if (! entry.getName () .endsWith (".class")) continue;
                        InputStream stream = file.getInputStream (entry);
                        byte[] all = stream.readAllBytes ();
                        ClassFile clazz = new ClassFile (all);
                        System.out.println (String.format ("Class file %s from %s", entry.getName (), arg));
                        clazz.debug ();
                    }
                    file.close ();
                    continue;
                } catch (IOException e) {
                    System.out.println (String.format ("Failed to open jar %s", arg));
                }
            }
            File file; InputStream stream; byte[] all;
            try {
                file = new File (arg);
                stream = new FileInputStream (file);
                all = stream.readAllBytes ();
                stream.close ();
            } catch (IOException e) {
                System.err.println (String.format ("Cannot open file %s", arg));
                continue;
            }
            try {
                ClassFile clazz = new ClassFile (all);
                System.out.println (String.format ("Class file %s", arg));
                clazz.debug ();
            } catch (IndexOutOfBoundsException e) {
                System.err.println (String.format ("Bad class file %s", arg));
                e.printStackTrace ();
                continue;
            }
        }
    }
}
