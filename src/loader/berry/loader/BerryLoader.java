// Copyright (C) 2025 VoidSingularity

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or (at
// your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
import java.util.function.Consumer;

import berry.utils.StringSorter;

public final class BerryLoader {
    private static String side;
    public static String getSide () { return side; }
    private static String modir;
    public static String getModDirectory () { return modir; }
    private static String gamdir = null;
    public static String getGameDirectory () { return gamdir; }
    private static String[] sargs;
    public static String[] getArgs () { return sargs; }
    private static String entry;
    public static String getEntrypoint () { return entry; }
    private static boolean indev;
    public static boolean isDevelopment () { return indev; }

    public static final List <Consumer <String>> preloaders = new ArrayList <> ();
    public static void preload (String cl) {
        for (var pl : preloaders) pl.accept (cl);
    }

    public static void main (String[] args) {
        String s = System.getProperty ("berry.side");
        side = s == null ? "CLIENT" /* defaults to CLIENT */ : s;
        new BerryLoader (args);
    }
    private static record JarStringInfo (JarContainer jar, String info, String name) {}
    private static Class <?> forname (String name) throws ClassNotFoundException {
        // berry.loader.BerryLoader is loaded by app cl,
        // but we want to search classes in our system cl
        return ClassLoader.getSystemClassLoader () .loadClass (name);
    }
    private BerryLoader (String[] args) {
        for (var str : System.getProperty ("berry.cps") .split (File.pathSeparator)) BerryClassLoader.getInstance () .appendToClassPathForInstrumentation (str);
        indev = "true" .equals (System.getProperty ("berry.indev"));
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
        System.out.println ("[BERRY] Discovering mods");
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
        sargs = argv;
        entry = args [0];
        StringSorter init = new StringSorter ();
        Map <String, BerryModInitializer> ins = new HashMap <> ();
        for (JarStringInfo info : bmc) {
            JarContainer jar = info.jar;
            String cls = info.info;
            try {
                Class <?> basemod = forname (cls);
                Constructor <?> c = basemod.getConstructor ();
                BerryModInitializer initializer = (BerryModInitializer) c.newInstance ();
                initializer.preinit (init, jar, info.name);
                ins.put (info.name, initializer);
            } catch (ClassNotFoundException e) {
                System.err.println (String.format ("[BERRY/ERROR] Cannot find class %s", cls));
                e.printStackTrace ();
            } catch (ClassCastException e) {
                System.err.println (String.format ("[BERRY/ERROR] %s does not implement berry.loader.BerryModInitializer!", cls));
                e.printStackTrace ();
            } catch (NoSuchMethodException e) {}
            catch (IllegalAccessException e) {}
            catch (Throwable throwable) {
                throw new RuntimeException (throwable);
            }
        }
        for (String name : init.sort ()) {
            System.out.println (String.format ("[BERRY] Initializing mod %s...", name));
            ins.get (name) .initialize (argv);
            System.out.println (String.format ("[BERRY] Initialized mod %s!", name));
        }
        System.out.println ("[BERRY] Preloading...");
        preload (args [0]);
        System.out.println ("[BERRY] Preload done, starting main class.");
        try {
            Class <?> main = forname (args [0]);
            MethodHandle handle = MethodHandles.lookup () .findStatic (main, "main", MethodType.methodType (Void.TYPE, String[].class)) .asFixedArity ();
            handle.invoke (argv);
        } catch (ClassNotFoundException exception) {
            System.err.println (String.format ("[BERRY] Unable to load main class %s. Exiting.", args [0]));
            exception.printStackTrace ();
        } catch (NoSuchMethodException exception) {
            System.err.println (String.format ("[BERRY] Unable to find void main(String[]) in main class. Exiting."));
            exception.printStackTrace ();
        } catch (IllegalAccessException exception) {
            System.err.println (String.format ("[BERRY] Unable to access void main(String[]) in main class. Exiting."));
            exception.printStackTrace ();
        } catch (Throwable throwable) {
            throw new RuntimeException (throwable);
        }
    }
}
