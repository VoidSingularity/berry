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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BerryClassLoader extends URLClassLoader {
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
    public BerryClassLoader (ClassLoader parent) {
        super (new URL[0], parent);
        instance = this;
    }
    public static record JarLocation (String path, JarFile jar) {}
	private final List <String> paths = new ArrayList <> ();
    public void appendToClassPathForInstrumentation (String path) {
        try {
			super.addURL (new File (path) .toURI () .toURL ());
			paths.add (path);
		}
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
	public List <String> getPaths () {
		return List.copyOf (paths);
	}
    public static URL toURL (String name, String jarpath) throws MalformedURLException {
        var file = new File (jarpath);
        var url = file.toURI () .toURL () .toString ();
        var ret = URI.create ("jar:" + url + "!/" + name) .toURL ();
        return ret;
    }
	private final Map <String, Enumeration <URL>> controlled = new HashMap <> ();
	public static Enumeration <URL> single (URL url) {
		return new Enumeration <> () {
			boolean flag = false;
			public boolean hasMoreElements () { return !flag; }
			public URL nextElement () {
				if (flag) throw new NoSuchElementException ();
				flag = true;
				return url;
			}
		};
	}
	public static Enumeration <URL> empty () {
		return new Enumeration <> () {
			public boolean hasMoreElements () { return false; }
			public URL nextElement () { throw new NoSuchElementException (); }
		};
	}
	public void controlResources (String resource, Enumeration <URL> urls) {
		controlled.put (resource, urls);
	}
	public void controlResources (String resource, URL url) {
		controlResources (resource, single (url));
	}
	@Override
	public Enumeration <URL> getResources (String name) throws IOException {
		if (controlled.containsKey (name)) return controlled.get (name);
		else return super.getResources (name);
	}
	// This method is convenient for debugging
	protected Class <?> defineClass0 (String name, byte[] buf, CodeSource cs) {
		return super.defineClass (name, buf, 0, buf.length, cs);
	}
    @Override
    public Class <?> findClass (String name) throws ClassNotFoundException {
        String rname = name.replace ('.', '/') + ".class";
        // Get resource
        try {
            InputStream is = this.getResourceAsStream (rname);
            if (is != null) {
                byte[] data = is.readAllBytes (); is.close ();
                return defineClass0 (name, data, getMetadata (name) .codeSource);
            } else {
                // generate code, i suppose
                byte[] data = BerryClassTransformer.instance () .transform (this, name, null, null, null);
                if (data != null)
                return defineClass0 (name, data, null);
            }
        } catch (IOException e) {}
        throw new ClassNotFoundException (name);
    }
    static {
        registerAsParallelCapable ();
    }
}
