package berry.api.asm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import berry.loader.BerryClassTransformer;
import berry.loader.JarProcessor;
import berry.loader.BerryClassTransformer.ByteCodeTransformer;
import berry.utils.Graph;

public class AccessTransformer implements JarProcessor {
    public static void init () {
        var graph = BerryClassTransformer.instance () .all.graph;
        ByteCodeTransformer transformer = (loader, name, clazz, domain, code) -> AccessTransformer.transform (name, code);
        var vtx = new Graph.Vertex ("berry::at", transformer);
        var rmp = graph.getVertices () .get ("berry::remap");
        var mxn = graph.getVertices () .get ("berry::mixin");
        graph.addVertex (vtx);
        graph.addEdge (null, rmp, vtx, null);
        graph.addEdge (null, vtx, mxn, null);
    }
    public static enum Finality {
        DEFAULT,
        ADD, // why would you do that, like, how
        REMOVE;
        public Finality merge (Finality other) {
            if (this == REMOVE || other == REMOVE) return REMOVE;
            if (this == ADD || other == ADD) return ADD;
            return DEFAULT;
        }
        public int apply (int flags) {
            if (this == ADD) flags |= 16;
            else if (this == REMOVE) flags &= ~16;
            return flags;
        }
    }
    public static enum Accessibility {
        PUBLIC,
        PROTECTED,
        DEFAULT,
        PRIVATE;
        public Accessibility merge (Accessibility other) {
            return ordinal () < other.ordinal () ? this : other;
        }
        public int apply (int flags) {
            flags &= ~7;
            switch (this) {
                case PUBLIC: flags |= 1; break;
                case PRIVATE: flags |= 2; break;
                case PROTECTED: flags |= 4; break;
                case DEFAULT:
            }
            return flags;
        }
    }
    public static record TransformerSingleton (
        Finality finality,
        Accessibility accessibility
    ) {
        public TransformerSingleton merge (TransformerSingleton other) {
            return new TransformerSingleton (
                finality.merge (other.finality),
                accessibility.merge (other.accessibility)
            );
        }
        public int apply (int flags) {
            return finality.apply (accessibility.apply (flags));
        }
    }
    public static class ClassTransformerCollection {
        private final Map <String, TransformerSingleton> targets = new HashMap <> ();
        public void add (String target, TransformerSingleton transformer) {
            if (target == null) target = "";
            if (targets.containsKey (target)) targets.put (target, targets.get (target) .merge (transformer));
            else targets.put (target, transformer);
        }
        public void apply (ClassFile cf) {
            // Class transform
            if (targets.containsKey ("")) {
                var trans = targets.get ("");
                // no private/protected classes
                cf.accessFlags = trans.apply (cf.accessFlags);
            }
            // Fields transform
            for (ClassFile.FieldOrMethod field : cf.fields) {
                String name = cf.ifStr (field.name);
                if (targets.containsKey (name)) {
                    var trans = targets.get (name);
                    field.accessFlags = trans.apply (field.accessFlags);
                }
            }
            // Methods transform
            for (ClassFile.FieldOrMethod method : cf.methods) {
                String name = cf.ifStr (method.name), desc = cf.ifStr (method.descriptor);
                if (name == null || desc == null) continue; // how
                name += desc;
                if (targets.containsKey (name)) {
                    var trans = targets.get (name);
                    method.accessFlags = trans.apply (method.accessFlags);
                }
            }
        }
    }
    public static final Map <String, ClassTransformerCollection> collections = new HashMap <> ();
    public static final Map <String, Function <String, String>> remappers = new HashMap <> ();
    public static void loadline (String line, String provider)
    {
        var func = remappers.get (provider);
        if (func != null) line = func.apply (line);
        if (line.contains ("#")) line = line.split ("#") [0];
        line = line.strip ();
        if (line.isEmpty ()) return;
        var splits = line.split (" ");
        if (splits.length < 2) return;
        Finality finality;
        if (splits [0] .endsWith ("-f")) finality = Finality.REMOVE;
        else if (splits [0] .endsWith ("+f")) finality = Finality.ADD;
        else finality = Finality.DEFAULT;
        Accessibility accessibility;
        if (splits [0] .startsWith ("public")) accessibility = Accessibility.PUBLIC;
        else if (splits [0] .startsWith ("protected")) accessibility = Accessibility.PROTECTED;
        else if (splits [0] .startsWith ("private")) accessibility = Accessibility.PRIVATE;
        else accessibility = Accessibility.DEFAULT;
        if (!collections.containsKey (splits [1])) collections.put (splits [1], new ClassTransformerCollection ());
        collections.get (splits [1]) .add (splits.length == 2 ? null : splits [2], new TransformerSingleton (finality, accessibility));
    }
    public static void loadstream (String provider, InputStream input) {
        Scanner scanner = new Scanner (input);
        while (scanner.hasNextLine ()) {
            String line = scanner.nextLine ();
            loadline (line, provider);
        }
        scanner.close ();
    }
    public static byte[] transform (String cls, byte[] buf) throws IOException {
        cls = cls.replace ('/', '.');
        var col = collections.get (cls);
        if (col != null) {
            if (buf == null) System.err.println ("WTF? " + cls);
            ClassFile cf = new ClassFile (buf);
            col.apply (cf);
            return cf.get ();
        }
        return buf;
    }
    public EntryInfo process (EntryInfo orig) throws IOException {
        if (orig.name () .endsWith (".class")) {
            String name = orig.name () .split ("\\.") [0] .replace ('/', '.');
            return new EntryInfo (orig.name (), transform (name, orig.data ()));
        }
        return orig;
    }
    // This method is for development use; neven run in-game
    // <AT file> <input jar> <output jar>
    public static void main (String... args) throws IOException {
        assert args.length == 3;
        File atf = new File (args [0]);
        InputStream stream = new FileInputStream (atf);
        loadstream ("berry", stream);
        stream.close ();
        JarProcessor.process (new AccessTransformer (), args [1], args [2]);
    }
}
