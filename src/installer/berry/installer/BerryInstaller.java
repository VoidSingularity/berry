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

package berry.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;

import net.md_5.specialsource.SpecialSource;

public class BerryInstaller {
    // TODO: Change these before publishing!
    public static final String VERSION = "0.0.0+2025021201";
    public static final Map <String, Integer> SIZE = Map.of ("loader", 23829);
    public static final Map <String, String> SHA1 = Map.of (
        "loader", "ef766dafc565b4cadeb607b34852ae06fa1eb499"
    );

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
            return "";
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    private static boolean sha1 (File f, String hash) {
        try {
            String h = sha1 (new FileInputStream (f));
            if (h.isEmpty () || h.equals (hash)) return true;
            else return false;
        } catch (IOException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }
    private static void dunsafe (URL url, String local, File fo) throws IOException {
        URLConnection connection = url.openConnection ();
        InputStream stream = connection.getInputStream ();
        OutputStream fout = new FileOutputStream (fo);
        byte[] buffer = new byte [65536];
        int len;
        while ((len = stream.read (buffer)) > 0) fout.write (buffer, 0, len);
        stream.close (); fout.close ();
    }
    private static void download (URL url, String local, String hash) throws IOException {
        File fo = new File (local);
        mktree (fo.getParent ());
        if (hash == null) {
            int i;
            for (i=0; i<10; i++) {
                try { dunsafe (url, local, fo); return; }
                catch (IOException e) {}
            }
            throw new IOException (String.format ("Cannot download file %s", url.toString ()));
        }
        int i;
        for (i=0; i<10; i++) {
            if (sha1 (fo, hash)) return;
            try {
                URLConnection connection = url.openConnection ();
                InputStream stream = connection.getInputStream ();
                OutputStream fout = new FileOutputStream (fo);
                byte[] buffer = new byte [65536];
                int len;
                while ((len = stream.read (buffer)) > 0) fout.write (buffer, 0, len);
                stream.close (); fout.close ();
            } catch (IOException e) {}
        }
        if (sha1 (fo, hash) == false) throw new IOException (String.format ("Cannot download file %s", url.toString ()));
    }
    private static String remapFilePath (String path) {
        switch (path) {
            case "int": return "I";
            case "double": return "D";
            case "boolean": return "Z";
            case "float": return "F";
            case "long": return "J";
            case "byte": return "B";
            case "short": return "S";
            case "char": return "C";
            case "void": return "V";
            default: return "L" + path.replace ('.', '/') + ";";
        }
    }
    private static record RB (String line, int cnt) {}
    private static RB removeBrackets (RB orig) {
        int cnt = orig.cnt; String line = orig.line;
        while (line.endsWith ("[]")) {
            cnt ++;
            line = line.substring (0, line.length () - 2);
        }
        return new RB (line, cnt);
    }
    private static void reformat (File in, File out) throws IOException {
        // Copied from DecompilerMC, translated into Java
        // Java sucks at scripting
        Map <String, String> fn = new HashMap <> ();
        try (Scanner sc = new Scanner (in)) {
            while (sc.hasNextLine ()) {
                var line = sc.nextLine ();
                if (line.startsWith ("#")) continue;
                var names = line.split (" -> ");
                if (! line.startsWith ("    ")) fn.put (remapFilePath (names [0]), names [1] .split (":") [0]);
            }
        }
        try (Scanner sc = new Scanner (in); PrintStream print = new PrintStream (out)) {
            while (sc.hasNextLine ()) {
                String line = sc.nextLine ();
                if (line.startsWith ("#")) continue;
                String[] names = line.split (" -> ");
                String deobf_name = names [0], obf_name = names [1];
                if (line.startsWith ("    ")) {
                    obf_name = obf_name.stripTrailing ();
                    deobf_name = deobf_name.stripLeading ();
                    String[] meth = deobf_name.split (" ");
                    String method_type = meth [0], method_name = meth [1];
                    var mdd = method_type.split (":");
                    method_type = mdd [mdd.length - 1];
                    if (method_name .contains ("(") && method_name .contains (")")) {
                        var a = method_name.split ("\\(");
                        var md = a [a.length - 1];
                        var variables = md.substring (0, md.length () - 1);
                        var function_name = a [0];
                        int alt = 0, i, j;
                        var rb = removeBrackets (new RB (method_type, alt));
                        method_type = rb.line; alt = rb.cnt;
                        method_type = remapFilePath (method_type);
                        method_type = fn.containsKey (method_type) ? ("L" + fn.get (method_type) + ";") : method_type;
                        if (method_type.contains (".")) method_type = method_type.replace (".", "/");
                        for (i=0; i<alt; i++) method_type = "[" + method_type;
                        if (variables.length () > 0) {
                            int[] alv = new int [variables.length ()];
                            for (i=0; i<variables.length (); i++) alv [i] = 0;
                            var varl = variables.split (",");
                            for (i=0; i<varl.length; i++) {
                                rb = removeBrackets (new RB (varl [i], alv [i]));
                                varl [i] = rb.line; alv [i] = rb.cnt;
                            }
                            for (i=0; i<varl.length; i++) {
                                var vr = varl [i];
                                vr = remapFilePath (vr);
                                if (fn.containsKey (vr)) vr = "L" + fn.get (vr) + ";";
                                vr = vr.replace ('.', '/');
                                for (j=0; j<alv[i]; j++) vr = "[" + vr;
                                varl [i] = vr;
                            }
                            variables = String.join ("", varl);
                        }
                        print.println (String.format ("\t%s (%s)%s %s", obf_name, variables, method_type, function_name));
                    } else {
                        print.println (String.format ("\t%s %s", obf_name, method_name));
                    }
                } else {
                    obf_name = obf_name.split (":") [0];
                    String rfp = remapFilePath (obf_name), fpp = remapFilePath (deobf_name);
                    print.println (rfp.substring (1, rfp.length () - 1) + " " + fpp.substring (1, fpp.length () - 1));
                }
            }
        }
    }
    public static void main (String[] args) throws Throwable {
        // Download manifest
        System.out.print ("Downloading manifest... ");
        download (
            new URI ("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json") .toURL (),
            "./manifest.json",
            null
        );
        System.out.println ("Done.");
        // Parse manifest and download 1.21.3
        String mcj = args.length == 0 ? "1.21.3" : args [0];
        var t = new File (".") .getAbsolutePath () .split (File.separator);
        String curd = t [t.length - 2];
        JSONObject mf;
        InputStream stream = new FileInputStream ("manifest.json");
        mf = new JSONObject (new String (stream.readAllBytes ()));
        stream.close ();
        JSONObject verobj = null;
        for (Object obj : mf.getJSONArray ("versions")) {
            if (obj instanceof JSONObject js && js.getString ("id") .equals (mcj)) {
                verobj = js;
                break;
            }
        }
        System.out.print ("Downloading client.json... ");
        download (
            new URI (verobj.getString ("url")) .toURL (),
            String.format ("./%s.json", curd),
            verobj.getString ("sha1")
        );
        System.out.println ("Done.");
        // Read json file
        JSONObject jsin;
        stream = new FileInputStream (curd + ".json");
        String in = new String (stream.readAllBytes ());
        stream.close ();
        jsin = new JSONObject (in);
        // Download client.jar
        System.out.print ("Downloading client.jar... ");
        String url = jsin.getJSONObject ("downloads") .getJSONObject ("client") .getString ("url");
        try {
            download (new URI (url) .toURL (), "./" + curd + ".jar", null);
        } catch (URISyntaxException e) {
            throw new RuntimeException (e);
        }
        System.out.println ("Done.");
        // Download mapping
        url = jsin.getJSONObject ("downloads") .getJSONObject ("client_mappings") .getString ("url");
        System.out.print ("Downloading client.txt... ");
        try {
            download (new URI (url) .toURL (), "./client.txt", null);
        } catch (URISyntaxException e) {
            throw new RuntimeException (e);
        }
        System.out.println ("Done.");
        // Re-format
        File inf = new File ("client.txt"), outf = new File ("client.tsrg");
        System.out.print ("Re-formatting client.txt into client.tsrg... ");
        reformat (inf, outf);
        System.out.println ("Done.");
        // Deobfuscate
        System.out.print ("Deobfuscating client.jar... ");
        SpecialSource.main (new String[] {
            "-q",
            "-i", curd + ".jar",
            "-o", "__tmp.jar",
            "-m", "client.tsrg"
        });
        System.out.println ("Done.");
        File mj = new File (curd + ".jar"); mj.delete ();
        File oj = new File ("__tmp.jar"); oj.renameTo (mj);
        // Extract agent.jar
        System.out.print ("Extracting agent.jar... ");
        InputStream is = BerryInstaller.class.getClassLoader () .getResourceAsStream ("jars/agent.jar");
        String agentdir = System.getProperty ("user.home") + File.separator + ".berry" + File.separator + "agents" + File.separator;
        mktree (agentdir);
        String agentfile = agentdir + sha1 (BerryInstaller.class.getClassLoader () .getResourceAsStream ("jars/agent.jar")) + ".jar";
        OutputStream os = new FileOutputStream (agentfile);
        byte[] b;
        while ((b = is.readNBytes (65536)) .length > 0) os.write (b);
        os.close ();
        System.out.println ("Done.");
        // Add agent to JVM args
        jsin.getJSONObject ("arguments") .getJSONArray ("jvm") .put ("-javaagent:" + agentfile);
        // Add loader to libs
        Function <String, JSONObject> func = (str) -> {
            JSONObject obj = new JSONObject ();
            obj.put ("name", "berry:" + str + ":" + VERSION);
            JSONObject atf = new JSONObject ();
            atf.put ("size", SIZE.get (str));
            atf.put ("path", "berry/" + str + "/" + VERSION + "/" + str + "-" + VERSION + ".jar");
            atf.put ("sha1", SHA1.get (str));
            atf.put ("url", "https://azfs.pages.dev/berry-dist/" + VERSION + "/" + str + ".jar");
            obj.put ("downloads", new JSONObject () .put ("artifact", atf));
            return obj;
        };
        JSONArray arr = jsin.getJSONArray ("libraries");
        arr.put (func.apply ("loader"));
        // Mojang ships ASM 9.3, but we need higher versions.
        int i;
        for (i=0; i<arr.length(); i++) {
            if (arr.getJSONObject (i) .getString ("name") .startsWith ("org.ow2.asm")) {
                arr.remove (i);
                break;
            }
        }
        // Modify game arguments
        String mclass = jsin.getString ("mainClass");
        jsin.put ("mainClass", "berry.loader.BerryLoader");
        arr = new JSONArray () .put (mclass);
        for (var o : jsin.getJSONObject ("arguments") .getJSONArray ("game"))
        arr.put (o);
        jsin.getJSONObject ("arguments") .put ("game", arr);
        // Save changes
        os = new FileOutputStream (curd + ".json");
        os.write (jsin.toString () .getBytes ());
        os.close ();
        // Builtin API
        System.out.print ("Extracting builtins.jar... ");
        mktree ("./mods/");
        is = BerryInstaller.class.getClassLoader () .getResourceAsStream ("jars/builtins.jar");
        os = new FileOutputStream ("mods/builtins.jar");
        while ((b = is.readNBytes (65536)) .length > 0) os.write (b);
        os.close ();
        System.out.println ("Done.");
    }
}
