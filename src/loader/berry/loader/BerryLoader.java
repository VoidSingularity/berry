package berry.loader;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public class BerryLoader {
    private static String side;
    public static String getSide () { return side; }

    public static void main (String[] args) {
        String s = System.getProperty ("berry.side");
        side = s == null ? "CLIENT" /* defaults to CLIENT */ : s;
        new BerryLoader (args);
    }
    public static record JarStringInfo (JarContainer jar, String info) {}
    public BerryLoader (String[] args) {
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
        List <JarStringInfo> bmc = new ArrayList <> ();
        for (var mod : mods) {
            try {
                File file = new File (moddir + mod);
                JarContainer container = new JarContainer (file);
                JarContainer.containers.put (mod, container);
                BerryClassTransformer.instance () .instrumentation () .appendToSystemClassLoaderSearch (container.file ());
                var jar = container.file ();
                var mf = jar.getManifest ();
                if (mf == null) continue;
                var attr = mf.getMainAttributes () .getValue ("Berry-Base-Mod");
                if (attr != null) bmc.add (new JarStringInfo (container, attr));
            } catch (IOException e) {}
        }
        String[] argv = new String [args.length - 1];
        for (i=1; i<args.length; i++) argv [i-1] = args [i];
        Thread thread = new Thread (
            () -> {
                for (JarStringInfo info : bmc) {
                    JarContainer jar = info.jar;
                    String cls = info.info;
                    try {
                        Class <?> basemod = Class.forName (cls);
                        MethodHandle handle = MethodHandles.lookup () .findStatic (basemod, "initialize", MethodType.methodType (Void.TYPE, JarContainer.class, String[].class)) .asFixedArity ();
                        handle.invoke (jar, argv);
                    } catch (ClassNotFoundException e) {
                        System.err.println (String.format ("[ERROR] Cannot find class %s", cls));
                    } catch (NoSuchMethodException e) {}
                    catch (IllegalAccessException e) {}
                    catch (Throwable throwable) {
                        throw new RuntimeException (throwable);
                    }
                }
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
        thread.run ();
    }
}
