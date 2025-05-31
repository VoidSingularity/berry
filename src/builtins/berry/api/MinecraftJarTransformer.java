package berry.api;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import berry.loader.BerryClassTransformer;
import berry.loader.BerryLoader;
import berry.utils.Graph;

public class MinecraftJarTransformer {
    public static Graph transformers = new Graph ();
    private static void transform (ZipFile file, ZipEntry entry, ZipOutputStream out) {
        try {
            var is = file.getInputStream (entry);
            var all = is.readAllBytes ();
            is.close ();
            for (var tr : transformers.sorted ()) {
                try {
                    @SuppressWarnings("unchecked")
                    var vertex = (BiFunction <String, byte[], byte[]>) transformers.getVertices () .get (tr) .getValue ();
                    all = vertex.apply (entry.getName (), all);
                } catch (ClassCastException e) {
                    throw new RuntimeException (e);
                }
            }
            out.putNextEntry (entry);
            out.write (all);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    public static void transform () {
        String orig = System.getProperty ("berry.mcjar");
        String trans = BerryLoader.getGameDirectory () + "transformed.jar";
        try {
            ZipFile file = new ZipFile (orig);
            FileOutputStream os = new FileOutputStream (trans);
            ZipOutputStream out = new ZipOutputStream (os);
            file.entries () .asIterator () .forEachRemaining (entry -> transform (file, entry, out));
            out.close (); os.close ();
            BerryClassTransformer.instrumentation () .appendToSystemClassLoaderSearch (new JarFile (trans));
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
}
