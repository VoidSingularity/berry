package berry.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Save {
    public static void save (String name, byte[] data) {
        try {
            OutputStream stream = new FileOutputStream (name);
            stream.write (data);
            stream.close ();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    public static void save (InputStream in, String out) {
        try {
            OutputStream stream = new FileOutputStream (out);
            stream.write (in.readAllBytes ());
            stream.close ();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
}
