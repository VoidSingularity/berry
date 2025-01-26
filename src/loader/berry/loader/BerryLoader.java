package berry.loader;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import berry.utils.Graph;

public final class BerryLoader {
    private static String side;
    public static String getSide () { return side; }
    private static String modir;
    public static String getModDirectory () { return modir; }
    private static String gamdir = null;
    public static String getGameDirectory () { return gamdir; }

    public static void main (String[] args) {
        String s = System.getProperty ("berry.side");
        side = s == null ? "CLIENT" /* defaults to CLIENT */ : s;
        new BerryLoader (args);
    }
    private static record JarStringInfo (JarContainer jar, String info, String name) {}
    private BerryLoader (String[] args) {
        int i;
        String moddir = null;
        for (i=2; i<args.length; i++)
        if (args [i-1] .equals ("--gameDir")) {
            String s = args [i];
            if (!s.endsWith (File.separator)) s += File.separator;
            gamdir = s;
            moddir = s + "mods" + File.separator;
        }
        if (gamdir == null) gamdir = "./";
        if (moddir == null) moddir = "./mods/";
        modir = moddir;
        File dir = new File (moddir);
        var mods = dir.list ();
        List <JarStringInfo> bmc = new ArrayList <> ();
        for (var mod : mods) {
            try {
                File file = new File (moddir + mod);
                JarContainer container = new JarContainer (file);
                JarContainer.containers.put (mod, container);
                var jar = container.file ();
                var mf = jar.getManifest ();
                if (mf == null) continue;
                var attr = mf.getMainAttributes () .getValue ("Berry-Base-Mod");
                var name = mf.getMainAttributes () .getValue ("Berry-Base-Mod-Name");
                if (attr != null) {
                    BerryClassTransformer.instrumentation () .appendToSystemClassLoaderSearch (container.file ());
                    bmc.add (new JarStringInfo (container, attr, name));
                }
            } catch (IOException e) {}
        }
        String[] argv = new String [args.length - 1];
        for (i=1; i<args.length; i++) argv [i-1] = args [i];
        Thread thread = new Thread (
            () -> {
                Graph init = new Graph ();
                Map <String, BerryModInitializer> ins = new HashMap <> ();
                for (JarStringInfo info : bmc) {
                    JarContainer jar = info.jar;
                    String cls = info.info;
                    try {
                        Class <?> basemod = Class.forName (cls);
                        Constructor <?> c = basemod.getConstructor ();
                        BerryModInitializer initializer = (BerryModInitializer) c.newInstance ();
                        initializer.preinit (init, jar, info.name);
                        ins.put (info.name, initializer);
                    } catch (ClassNotFoundException e) {
                        System.err.println (String.format ("[ERROR] Cannot find class %s", cls));
                    } catch (ClassCastException e) {
                        System.err.println (String.format ("[ERROR] %s does not implement berry.loader.BerryModInitializer!", cls));
                    } catch (NoSuchMethodException e) {}
                    catch (IllegalAccessException e) {}
                    catch (Throwable throwable) {
                        throw new RuntimeException (throwable);
                    }
                }
                for (String name : init.sorted ()) {
                    ins.get (name) .initialize (argv);
                    System.out.println (String.format ("Initialized mod %s!", name));
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
