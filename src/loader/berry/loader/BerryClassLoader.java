package berry.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BerryClassLoader extends SecureClassLoader {
    private static BerryClassLoader instance;
    public static BerryClassLoader getInstance () {
        return instance;
    }
    static final class Metadata {
		static final Metadata EMPTY = new Metadata(null, null);

		final Manifest manifest;
		final CodeSource codeSource;

		Metadata(Manifest manifest, CodeSource codeSource) {
			this.manifest = manifest;
			this.codeSource = codeSource;
		}
	}
    private static boolean hasRegularCodeSource(URL url) {
		return url.getProtocol().equals("file") || url.getProtocol().equals("jar");
	}
    private static final Map <Path, Metadata> metadataCache = new ConcurrentHashMap <> ();
    public static Path asPath(URL url) {
		try {
			return Paths.get(url.toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException (e);
		}
	}
    private static Path getCodeSource(URL url, String filename) {
		try {
			return LoaderUtil.normalizeExistingPath (UrlUtil.getCodeSource (url, filename));
		} catch (UrlConversionException e) {
			throw new RuntimeException (e);
		}
	}
    private Metadata getMetadata(String name) {
		String fileName = LoaderUtil.getClassFileName (name);
		URL url = this.getResource(fileName);
		if (url == null || !hasRegularCodeSource(url)) return Metadata.EMPTY;

		return getMetadata(getCodeSource(url, fileName));
	}
	private Metadata getMetadata(Path codeSource) {
		return metadataCache.computeIfAbsent(codeSource, (Path path) -> {
			Manifest manifest = null;
			CodeSource cs = null;
			Certificate[] certificates = null;

			try {
				if (Files.isDirectory(path)) {
					manifest = ManifestUtil.readManifestFromBasePath(path);
				} else {
                    @SuppressWarnings ("deprecation")
					URLConnection connection = new URL("jar:" + path.toUri().toString() + "!/").openConnection();

					if (connection instanceof JarURLConnection) {
						manifest = ((JarURLConnection) connection).getManifest();
						certificates = ((JarURLConnection) connection).getCertificates();
					}

					if (manifest == null) {
						try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(path, false)) {
							manifest = ManifestUtil.readManifestFromBasePath(jarFs.get().getRootDirectories().iterator().next());
						}
					}
				}
			} catch (IOException | FileSystemNotFoundException e) {
				if (BerryLoader.isDevelopment()) {
					// Log.warn(LogCategory.KNOT, "Failed to load manifest", e);
				}
			}

			if (cs == null) {
				try {
					cs = new CodeSource(UrlUtil.asUrl(path), certificates);
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}

			return new Metadata(manifest, cs);
		});
	}
    private final List <String> paths = new ArrayList <> ();
    public BerryClassLoader (ClassLoader parent) {
        super (parent);
        instance = this;
    }
    private static record JarLocation (String path, JarFile jar) {}
    private static record EntriesStorage (JarLocation first, Set <JarLocation> all) {}
    private final Map <String, EntriesStorage> allentries = new HashMap <> ();
    public void appendToClassPathForInstrumentation (String path) {
        paths.add (path);
        if (path.endsWith (".jar")) {
            // Try scanning this jar
            try {
                JarFile jar = new JarFile (path);
                addJar (jar, path);
            } catch (IOException e) {}
        }
    }
    public void addJar (JarFile jar, String path) {
        Consumer <JarEntry> con = (entry) -> {
            String name = entry.getName ();
            if (allentries.containsKey (name)) {
                allentries.get (name) .all.add (new JarLocation (path, jar));
            } else {
                JarLocation loc = new JarLocation (path, jar);
                Set <JarLocation> set = new HashSet <> (); set.add (loc);
                allentries.put (name, new EntriesStorage (loc, set));
            }
        };
        jar.entries () .asIterator () .forEachRemaining (con);
    }
    public URL toURL (String name, JarLocation loc) throws MalformedURLException {
        var file = new File (loc.path);
        var url = file.toURI () .toURL () .toString ();
        var ret = URI.create ("jar:" + url + "!/" + name) .toURL ();
        return ret;
    }
    public URL getResource (String name) {
        var par = this.getParent () .getResource (name);
        if (par != null) return par;
        if (allentries.containsKey (name)) {
            var entry = allentries.get (name);
            try {
                return toURL (name, entry.first);
            } catch (MalformedURLException e) {
                // Fall through
            }
        }
        return null;
    }
    public InputStream getResourceAsStream (String name) {
        var par = this.getParent () .getResourceAsStream (name);
        if (par != null) return par;
        if (allentries.containsKey (name)) {
            var entry = allentries.get (name);
            try {
                return entry.first.jar.getInputStream (entry.first.jar.getEntry (name));
            } catch (IOException e) {
                // Fall through
            }
        }
        return null;
    }
    public Enumeration <URL> getResources (String name) throws IOException {
        Vector <URL> vec = new Vector <> ();
        var par = this.getParent () .getResources (name);
        par.asIterator () .forEachRemaining ((e) -> vec.add (e));
        if (allentries.containsKey (name)) {
            var entry = allentries.get (name);
            for (var loc : entry.all) {
                try {
                    vec.add (toURL (name, loc));
                } catch (MalformedURLException e) {}
            }
        }
        return vec.elements ();
    }
    @Override
    public Class <?> findClass (String name) throws ClassNotFoundException {
        String rname = name.replace ('.', '/') + ".class";
        // Get resource
        try {
            InputStream is = this.getResourceAsStream (rname);
            if (is != null) {
                byte[] data = is.readAllBytes (); is.close ();
                return defineClass (name, data, 0, data.length, getMetadata (name) .codeSource);
            } else {
                // generate code, i suppose
                byte[] data = BerryClassTransformer.instance () .transform (this, name, null, null, null);
                if (data != null)
                return defineClass (name, data, 0, data.length);
            }
        } catch (IOException e) {}
        throw new ClassNotFoundException (name);
    }
    static {
        registerAsParallelCapable ();
    }
}
