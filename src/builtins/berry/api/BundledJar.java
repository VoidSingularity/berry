package berry.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import berry.loader.BerryClassTransformer;
import berry.loader.JarContainer;

public class BundledJar {
    private static void mktree (String tree) {
        File t = new File (tree);
        if (t.exists ()) return;
        mktree (t.getParent ());
        t.mkdir ();
    }
    private static String sha1 (InputStream stream) {
        try {
            MessageDigest digest = MessageDigest.getInstance ("SHA-1");
            byte[] buf = new byte [65536];
            while (true) {
                int len = stream.read (buf);
                if (len <= 0) break;
                digest.update (buf, 0, len);
            }
            stream.close ();
            // To hex
            StringBuilder builder = new StringBuilder (digest.getDigestLength () * 2);
            for (byte b : digest.digest ()) {
                String h = Integer.toHexString (0xff & b);
                if (h.length () == 1) builder.append ('0');
                builder.append (h);
            }
            return builder.toString ();
        } catch (NoSuchAlgorithmException e) {
            System.out.println ("Warning: your current environment does not support SHA-1");
            return null; // ?
        } catch (IOException e) {
            return null;
        }
    }
    public static void addBundled (String root, JarContainer mod, String path) throws IOException {
        // A ZipEntry cannot be directly added, so we extract the bundled jars
        // Extract dir
        if (! root.endsWith (File.separator)) root += File.separator;
        root += ".bundled/";
        mktree (root);
        // Now check the hash
        ZipEntry entry = mod.file () .getEntry (path);
        InputStream stream = mod.file () .getInputStream (entry);
        String s1 = sha1 (stream); stream.close ();
        File target = new File (root + s1 + ".jar");
        // If the file already exists and the sha-1 matches the previous calculated one
        if (target.exists ()) {
            stream = new FileInputStream (target);
            String s2 = sha1 (stream); stream.close ();
            if (s1.equals (s2)) {
                BerryClassTransformer.instrumentation () .appendToSystemClassLoaderSearch (new JarFile (target));
                return;
            }
        }
        // Now simply copy
        stream = mod.file () .getInputStream (entry);
        OutputStream out = new FileOutputStream (target);
        byte[] buffer = new byte [65536]; int len;
        while ((len = stream.read (buffer)) > 0) out.write (buffer, 0, len);
        out.close (); stream.close ();
        BerryClassTransformer.instrumentation () .appendToSystemClassLoaderSearch (new JarFile (target));
    }
}