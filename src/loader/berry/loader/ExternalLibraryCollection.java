package berry.loader;

import static berry.loader.BerryLoader.libraries;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExternalLibraryCollection {
    static String prefix;
    public static String getPrefix () {
        return prefix;
    }
    protected final Map <String, URL> col = new HashMap <> ();
    protected void lib (String hash, String url) {
        try {
            URL u = new URI (url) .toURL ();
            col.put (hash, u);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException ("URL " + url + " is invalid");
        }
    }
    public void initialize () {
        for (String hash : col.keySet ())
        libraries.put (hash, col.get (hash));
    }
    public void imports () {
        for (String hash : col.keySet ())
        BerryClassLoader.getInstance () .appendToClassPathForInstrumentation (prefix + hash + ".jar");
    }
    public Set <String> keySet () {
        return col.keySet ();
    }
}
