package bluet.berry;

import java.lang.instrument.Instrumentation;

import bluet.berry.loader.BerryClassTransformer;

public class BerryAgent {
    public static void premain (String arg, Instrumentation inst) {
        // Basically, how Berry Mod Loader works is like this:
        // The berryagent.jar is started through JVM arguments: -javaagent:berryagent.jar
        // The berrylib.jar and asm.jar is added to the classpath
        // Then the berryagent.jar will initialize bluet.berry.loader.BerryClassTransformer
        // The main class is changed into bluet.berry.loader.BerryLoaderMain
        // The main class will search for loader mods in the mods directory and load them
        // The loader mods should load the rest of the mods
        // However, all the classes are loaded by the main class (bluet.berry.loader.BerryLoaderMain)
        inst.addTransformer (new BerryClassTransformer (inst), true);
        System.out.println ("[JA] BerryAgent loaded.");
    }
}
