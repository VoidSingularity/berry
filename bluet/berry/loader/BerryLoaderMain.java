package bluet.berry.loader;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class BerryLoaderMain {
    public static void main (String[] args) {
        int i;
        String moddir = null;
        for (i=2; i<args.length; i++)
        if (args [i-1] .equals ("--gameDir")) {
            String s = args [i];
            if (!s.endsWith (File.separator)) s += File.separator;
            moddir = s + "mods" + File.separator;
        }
        if (moddir == null) moddir = "./mods/";
        File dir = new File (moddir);
        var mods = dir.list ();
        List <URL> cps = new ArrayList <> ();
        for (var mod : mods) {
            try {
                File file = new File (moddir + mod);
                JarContainer container = new JarContainer (file);
                JarContainer.containers.put (mod, container);
                cps.add (file.toURI () .toURL ());
            } catch (IOException e) {}
        }
        URL[] urls = new URL [cps.size ()];
        for (i=0; i<urls.length; i++) urls [i] = cps.get (i);
        URLClassLoader loader = new URLClassLoader (urls, ClassLoader.getSystemClassLoader ());
        String[] argv = new String [args.length - 1];
        for (i=1; i<args.length; i++) argv [i-1] = args [i];
        Thread thread = new Thread (
            () -> {
                try {
                    Class <?> main = Class.forName (args [0]);
                    MethodHandle handle = MethodHandles.lookup () .findStatic (main, "main", MethodType.methodType (Void.TYPE, String[].class)) .asFixedArity ();
                    handle.invoke (argv);
                } catch (ClassNotFoundException exception) {
                    System.err.println (String.format ("Unable to load main class %s. Exiting.", args [0]));
                } catch (NoSuchMethodException exception) {
                    System.err.println (String.format ("Unable to find void main(String[]) in main class. Exiting."));
                } catch (IllegalAccessException exception) {
                    System.err.println (String.format ("Unable to access void main(String[]) in main class. Exiting."));
                } catch (Throwable throwable) {
                    throw new RuntimeException (throwable);
                }
            }
        );
        thread.setContextClassLoader (loader);
        thread.start ();
    }
}
