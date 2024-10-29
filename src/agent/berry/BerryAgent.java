package berry;

import java.lang.instrument.Instrumentation;

import berry.loader.BerryClassTransformer;

public class BerryAgent {
    public static void premain (String arg, Instrumentation inst) {
        // Basically, how Berry Mod Loader works is like this:
        // The berryagent.jar is started through JVM arguments: -javaagent:berryagent.jar
        // The berrylib.jar and asm.jar is added to the classpath
        // Then the berryagent.jar will initialize berry.loader.BerryClassTransformer
        // The main class is changed into berry.loader.BerryLoaderMain
        // The main class will search for loader mods in the mods directory and load them
        // The loader mods should load the rest of the mods
        // However, all the classes are loaded by the main class (berry.loader.BerryLoaderMain)
        inst.addTransformer (new BerryClassTransformer (inst), true);
        System.out.println ("[JA] BerryAgent loaded.");
    }
}
