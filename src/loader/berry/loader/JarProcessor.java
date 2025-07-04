package berry.loader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public interface JarProcessor {
    public static record EntryInfo (String name, byte[] data) {}
    public EntryInfo process (EntryInfo original) throws IOException;
    default JarProcessor concat (JarProcessor second) {
        return (original) -> {
            var mid = this.process (original);
            return second.process (mid);
        };
    }
    public static void process (JarProcessor processor, ZipFile in, ZipOutputStream out) {
        in.entries () .asIterator () .forEachRemaining (
            (entry) -> {
                try {
                    byte[] data = in.getInputStream (entry) .readAllBytes ();
                    EntryInfo proc = processor.process (new EntryInfo (entry.getName (), data));
                    out.putNextEntry (new ZipEntry (proc.name));
                    out.write (proc.data);
                } catch (IOException e) {
                    throw new RuntimeException (e);
                }
            }
        );
    }
    public static void process (JarProcessor processor, String input, String output) throws IOException {
        ZipFile in = new ZipFile (input);
        OutputStream os = new FileOutputStream (output);
        ZipOutputStream zout = new ZipOutputStream (os);
        process (processor, in, zout);
        zout.close (); os.close ();
    }
}
