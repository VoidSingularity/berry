package berry.api.asm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ClassFile {
    public static final int magicnum = 0xCAFEBABE;
    public int minor, major;
    // Constant Pool
    public static class Constant {
        public final int type;
        public byte[] data;
        public Constant (int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }
    public Stack <Constant> constants;
    public int accessFlags, thisClass, superClass;
    public String cls_name (int idx) {
        idx = getShortFromBytes (constants.get (idx) .data);
        return ifStr (idx);
    }
    public int[] interfaces;
    public static record ExceptionTable (int start, int end, int handler, int catchType) {
        public static ExceptionTable read (DataInputStream stream) throws IOException {
            int s, e, h, c;
            s = stream.readUnsignedShort ();
            e = stream.readUnsignedShort ();
            h = stream.readUnsignedShort ();
            c = stream.readUnsignedShort ();
            return new ExceptionTable (s, e, h, c);
        }
    }
    public static interface ClassAttribute {
        public void write (DataOutputStream stream) throws IOException;
        public int size ();
        public int getName ();
        default void debug (PrintStream ps, ClassFile file, int tabSpaces) {
            outputSpaces (tabSpaces);
            ps.println ("No debug info now!");
        }
    }
    public static interface AttributeProvider {
        public ClassAttribute newAttribute (ClassFile file, int name, int len, DataInputStream stream) throws IOException;
    }
    private static final Map <String, AttributeProvider> providers = Map.of (
        "ConstantValue", ConstantValue::new,
        "Code", CodeAttribute::new,
        "Exceptions", ExceptionsAttribute::new
    );
    public ClassAttribute newAttribute (int name, int len, DataInputStream stream) throws IOException {
        AttributeProvider provider = providers.get (ifStr (name));
        if (provider == null) return new AnyAttribute (name, len, stream);
        else return provider.newAttribute (this, name, len, stream);
    }
    public static void outputSpaces (int tabSpaces) { for (int j=0; j<tabSpaces; j++) System.out.print (' '); }
    public static class AnyAttribute implements ClassAttribute {
        public int name;
        public byte[] info;
        public AnyAttribute (int name, int len, DataInputStream input) throws IOException {
            this.name = name;
            this.info = input.readNBytes (len);
        }
        public AnyAttribute (int name, byte[] info) {
            this.name = name;
            this.info = info;
        }
        public void write (DataOutputStream stream) throws IOException {
            stream.writeShort (name);
            stream.writeInt (info.length);
            stream.write (info);
        }
        public int size () {
            return 6 + this.info.length;
        }
        public int getName () { return this.name; }
        public void debug (PrintStream ps, ClassFile file, int tab) {
            int k;
            for (k=0; k<info.length; k++) {
                if (k % 12 == 0) {
                    if (k > 0) ps.println ();
                    outputSpaces (tab);
                }
                ps.print (hex (info [k]));
            }
            ps.println ();
        }
    }
    public static class CodeAttribute implements ClassAttribute {
        public int name, maxStack, maxLocals;
        public byte[] code;
        public ExceptionTable[] exceptionTables;
        public List <ClassAttribute> subAttributes;
        public CodeAttribute (ClassFile file, int name, int len, DataInputStream input) throws IOException {
            this.name = name; // We just throw the argument len away
            int i, l;
            maxStack = input.readUnsignedShort ();
            maxLocals = input.readUnsignedShort ();
            code = input.readNBytes (input.readInt ());
            l = input.readUnsignedShort ();
            exceptionTables = new ExceptionTable [l];
            for (i=0; i<l; i++) exceptionTables [i] = ExceptionTable.read (input);
            l = input.readUnsignedShort ();
            subAttributes = new ArrayList <> ();
            for (i=0; i<l; i++) {
                int n = 0, m = 0;
                n = input.readUnsignedShort ();
                m = input.readInt ();
                subAttributes.add (file.newAttribute (n, m, input));
            }
        }
        public CodeAttribute (ClassFile file, int name, CodeAttribute origin, byte[] code, Map <String, ClassAttribute> mods, Set <String> removes) {
            this.name = name;
            maxStack = origin.maxStack;
            maxLocals = origin.maxLocals;
            if (code == null) this.code = origin.code;
            else this.code = code;
            exceptionTables = origin.exceptionTables;
            subAttributes = new ArrayList <> ();
            for (var a : origin.subAttributes) {
                var s = file.ifStr (a.getName ());
                if (mods.containsKey (s)) subAttributes.add (mods.get (s));
                else if (!removes.contains (s)) subAttributes.add (a);
            }
        }
        @Override
        public int size () {
            int length = 18 + code.length + exceptionTables.length * 8;
            for (ClassAttribute attr : subAttributes) length += attr.size ();
            return length;
        }
        @Override
        public void write (DataOutputStream stream) throws IOException {
            int csize = size (), i;
            stream.writeShort (name);
            stream.writeInt (csize - 6);
            stream.writeShort (maxStack);
            stream.writeShort (maxLocals);
            stream.writeInt (code.length);
            stream.write (code);
            stream.writeShort (exceptionTables.length);
            for (i=0; i<exceptionTables.length; i++) {
                var exc = exceptionTables [i];
                stream.writeShort (exc.start);
                stream.writeShort (exc.end);
                stream.writeShort (exc.handler);
                stream.writeShort (exc.catchType);
            }
            stream.writeShort (subAttributes.size());
            for (ClassAttribute attr : subAttributes) attr.write (stream);
        }
        public int getName () { return this.name; }
        public void debug (PrintStream ps, ClassFile file, int tab) {
            int k, l;
            outputSpaces (tab);
            ps.println (String.format ("Max stack: %d, Max locals: %d", maxStack, maxLocals));
            outputSpaces (tab);
            ps.print ("Code:");
            for (k=0; k<code.length; k++) {
                if (k % 12 == 0) {
                    ps.println ();
                    outputSpaces (tab+1);
                }
                ps.print (hex (code [k]));
            }
            ps.println ();
            outputSpaces (tab);
            ps.println (String.format ("Exception tables: %d", exceptionTables.length));
            for (ExceptionTable t : exceptionTables) {
                outputSpaces (tab+1);
                ps.println (String.format ("%d %d %d %d", t.start, t.end, t.handler, t.catchType));
            }
            outputSpaces (tab);
            ps.println (String.format ("Sub-attributes: %d", subAttributes.size ()));
            for (l=0; l<subAttributes.size(); l++) {
                ClassAttribute a = subAttributes.get (l);
                outputSpaces (tab+1);
                ps.println (String.format ("Sub-attribute %d: name %s", l + 1, file.ifStr (a.getName ())));
                a.debug (ps, file, tab+2);
            }
        }
    }
    public static class ConstantValue implements ClassAttribute {
        public int index, name;
        public int getName () { return this.name; }
        public ConstantValue (ClassFile file, int name, int len, DataInputStream input) throws IOException {
            this.name = name;
            this.index = input.readUnsignedShort ();
        }
        public int size () { return 8; }
        public void write (DataOutputStream stream) throws IOException {
            stream.writeShort (name);
            stream.writeInt (2);
            stream.writeShort (index);
        }
        public void debug (PrintStream ps, ClassFile file, int tab) {
            outputSpaces (tab);
            ps.println (String.format ("Constant ID: %d", index));
        }
    }
    public static class ExceptionsAttribute implements ClassAttribute {
        public int[] table;
        public int name;
        public int getName () { return this.name; }
        public ExceptionsAttribute (ClassFile file, int name, int len, DataInputStream input) throws IOException {
            this.name = name;
            table = new int [input.readUnsignedShort ()];
            int i;
            for (i=0; i<table.length; i++) table [i] = input.readUnsignedShort ();
        }
        public int size () { return 8 + 2 * table.length; }
        public void write (DataOutputStream stream) throws IOException {
            stream.writeShort (name);
            stream.writeInt (table.length * 2 + 2);
            stream.writeShort (table.length);
            int i;
            for (i=0; i<table.length; i++) stream.writeShort (table [i]);
        }
        public void debug (PrintStream ps, ClassFile file, int tab) {
            outputSpaces (tab);
            ps.println (String.format ("Exceptions: %d", table.length));
            int i;
            for (i=0; i<table.length; i++) {
                outputSpaces (tab+1);
                ps.println (String.format ("Exception %d: %s", i+1, file.ifStr (getShortFromBytes (file.constants.get (table [i]) .data))));
            }
        }
    }
    public static class FieldOrMethod {
        public int accessFlags;
        public int name;
        public int descriptor;
        public List <ClassAttribute> attributes;
        public FieldOrMethod (int accessFlags, int name, int descriptor, List <ClassAttribute> attributes) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.descriptor = descriptor;
            this.attributes = attributes;
        }
        public void write (DataOutputStream stream) throws IOException {
            stream.writeShort (accessFlags);
            stream.writeShort (name);
            stream.writeShort (descriptor);
            stream.writeShort (attributes.size ());
            for (ClassAttribute attr : attributes) attr.write (stream);
        }
        public int length () {
            int r = 8;
            for (ClassAttribute attr : attributes) r += attr.size ();
            return r;
        }
    }
    public List <FieldOrMethod> fields;
    public List <FieldOrMethod> methods;
    public List <ClassAttribute> attributes;
    private static int wrap (byte b) {
        int r = b;
        if (r < 0) r += 256;
        return r;
    }
    // Debug flag; turn it OFF after debugging
    // The debug code is incomplete because there aren't too many bugs for now :3
    public static final boolean debug = false;
    // Another debug switch
    public static final boolean verify;
    static {
        String v = System.getProperty ("berry.asm.verifyClassFiles");
        if ("true".equals (v)) verify = true;
        else verify = false;
    }
    public ClassFile (byte[] file) throws IOException {
        constants = new Stack <> ();
        ByteArrayInputStream in = new ByteArrayInputStream (file);
        DataInputStream stream = new DataInputStream (in);
        if (stream.readInt () != magicnum) throw new IOException ("Magic number incorrect");
        minor = stream.readUnsignedShort ();
        major = stream.readUnsignedShort ();
        int cnt = stream.readUnsignedShort (), i;
        if (debug) System.out.println (String.format ("Major %d Minor %d Constant %d", major, minor, cnt));
        constants.add (null);
        for (i=1; i<cnt; i++) {
            int type = stream.readUnsignedByte ();
            int l;
            switch (type) {
                case 1:
                l = stream.readUnsignedShort ();
                break;
                case 7: case 8: case 16: case 19: case 20: l = 2; break;
                case 15: l = 3; break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: l = 4; break;
                case 5: case 6: l = 8; break;
                default: throw new IOException ();
            }
            constants.add (new Constant (type, stream.readNBytes (l)));
            // idk why they do this, but wcis
            if (type == 5 || type == 6) {
                constants.add (new Constant ((byte) 0, null));
                i++;
            }
        }
        accessFlags = stream.readUnsignedShort ();
        thisClass = stream.readUnsignedShort ();
        superClass = stream.readUnsignedShort ();
        cnt = stream.readUnsignedShort ();
        interfaces = new int [cnt];
        for (i=0; i<cnt; i++) interfaces [i] = stream.readUnsignedShort ();
        cnt = stream.readUnsignedShort ();
        fields = new ArrayList <> ();
        for (i=0; i<cnt; i++) {
            int a, n, d, s, j;
            a = stream.readUnsignedShort ();
            n = stream.readUnsignedShort ();
            d = stream.readUnsignedShort ();
            s = stream.readUnsignedShort ();
            List <ClassAttribute> attrs = new ArrayList <> ();
            for (j=0; j<s; j++) {
                int name, len;
                name = stream.readUnsignedShort ();
                len = stream.readInt ();
                attrs.add (newAttribute (name, len, stream));
            }
            fields.add (new FieldOrMethod (a, n, d, attrs));
        }
        cnt = stream.readUnsignedShort ();
        methods = new ArrayList <> ();
        for (i=0; i<cnt; i++) {
            int a, n, d, s, j;
            a = stream.readUnsignedShort ();
            n = stream.readUnsignedShort ();
            d = stream.readUnsignedShort ();
            s = stream.readUnsignedShort ();
            List <ClassAttribute> attrs = new ArrayList <> ();
            for (j=0; j<s; j++) {
                int name, len;
                name = stream.readUnsignedShort ();
                len = stream.readInt ();
                attrs.add (newAttribute (name, len, stream));
            }
            methods.add (new FieldOrMethod (a, n, d, attrs));
        }
        cnt = stream.readUnsignedShort ();
        attributes = new ArrayList <> ();
        for (i=0; i<cnt; i++) {
            int name, len;
            name = stream.readUnsignedShort ();
            len = stream.readInt ();
            attributes.add (newAttribute (name, len, stream));
        }
        stream.close ();
        in.close ();
        if (verify) {
            var e = get ();
            for (i=0; i<file.length; i++)
            if (e [i] != file [i]) {
                System.err.println (String.format ("@ %d Original %s Now %s", i, hex (file [i]), hex (e [i])));
                throw new IndexOutOfBoundsException ();
            }
        }
    }
    public byte[] get () throws IOException {
        int length = 24, i;
        for (i=1; i<constants.size(); i++) {
            var constant = constants.get (i);
            if (constant.type == 0) continue;
            length++; length += constant.data.length;
        }
        length += interfaces.length * 2;
        for (FieldOrMethod field : fields) length += field.length ();
        for (FieldOrMethod method : methods) length += method.length ();
        for (ClassAttribute attr : attributes) length += attr.size ();
        ByteArrayOutputStream out = new ByteArrayOutputStream (length);
        DataOutputStream stream = new DataOutputStream (out);
        stream.writeInt (magicnum);
        stream.writeShort (minor);
        stream.writeShort (major);
        stream.writeShort (constants.size ());
        for (i=1; i<constants.size(); i++) {
            var constant = constants.get (i);
            if (constant.type == 0) continue;
            stream.writeByte (constant.type);
            if (constant.type == 1) {
                stream.writeShort (constant.data.length);
                // System.out.println ("con " + new String (constant.data));
            }
            stream.write (constant.data);
        }
        stream.writeShort (accessFlags);
        stream.writeShort (thisClass);
        stream.writeShort (superClass);
        stream.writeShort (interfaces.length);
        for (i=0; i<interfaces.length; i++) stream.writeShort (interfaces [i]);
        stream.writeShort (fields.size ());
        for (FieldOrMethod f : fields) f.write (stream);
        stream.writeShort (methods.size ());
        for (FieldOrMethod f : methods) f.write (stream);
        stream.writeShort (attributes.size ());
        for (ClassAttribute attr : attributes) attr.write (stream);
        var ret = out.toByteArray ();
        stream.close ();
        out.close ();
        return ret;
    }
    public String ifStr (int index) {
        Constant constant = constants.get (index);
        if (constant.type != 1) return null;
        return new String (constant.data);
    }
    public static String displayStr (String str) {
        byte[] d = str.getBytes () .clone ();
        int i;
        for (i=0; i<d.length; i++) {
            if (d [i] < ' ' || d [i] > '~') d [i] = '?';
        }
        return new String (d);
    }
    public static int getShortFromBytes (byte[] bytes, int start) {
        return wrap (bytes [start]) * 256 + wrap (bytes [start+1]);
    }
    public static int getShortFromBytes (byte[] bytes) { return getShortFromBytes (bytes, 0); }
    public static void setShortToBytes (byte[] bytes, int value, int start) {
        bytes [start] = (byte) (value / 256);
        bytes [start+1] = (byte) (value % 256);
    }
    public static void setShortToBytes (byte[] bytes, int value) { setShortToBytes (bytes, value, 0); }
    private static String hex (int b) {
        String x = "0123456789ABCDEF";
        b = (b + 256) % 256;
        char[] c = {x.charAt (b >> 4), x.charAt (b & 15)};
        return String.valueOf (c);
    }
    private static String hex2b (int i) {
        return hex ((byte) (i / 256)) + hex ((byte) (i % 256));
    }
    public int getMethod (NameAndType descriptor) {
        int i;
        for (i=0; i<methods.size(); i++) {
            var method = methods.get (i);
            if (descriptor.name () .equals (ifStr (method.name))
                && descriptor.type () .equals (ifStr (method.descriptor)))
            return i;
        }
        return -1;
    }
    public int newUTF8 (String info) {
        int i;
        for (i=1; i<constants.size(); i++) {
            var constant = constants.get (i);
            if (constant.type == 1) {
                if (new String (constant.data) .equals (info)) return i;
            }
        }
        Constant c = new Constant (1, info.getBytes ());
        constants.add (c);
        return constants.size () - 1;
    }
    public int newClassDescriptor (String clazz) {
        int i;
        for (i=1; i<constants.size(); i++) {
            var constant = constants.get (i);
            if (constant.type == 7) {
                int j = getShortFromBytes (constant.data);
                if (clazz.equals (ifStr (j))) return i;
            } 
        }
        Constant c = new Constant (7, new byte [2]);
        setShortToBytes (c.data, newUTF8 (clazz));
        constants.add (c);
        return constants.size () - 1;
    }
    public int newNameAndType (NameAndType descriptor) {
        int i;
        for (i=1; i<constants.size(); i++) {
            var constant = constants.get (i);
            if (constant.type == 12 && descriptor.name () .equals (ifStr (getShortFromBytes (constant.data)))
                && descriptor.type () .equals (ifStr (getShortFromBytes (constant.data, 2)))) return i;
        }
        Constant c = new Constant (12, new byte [4]);
        setShortToBytes (c.data, newUTF8 (descriptor.name ()));
        setShortToBytes (c.data, newUTF8 (descriptor.type ()), 2);
        constants.add (c);
        return constants.size () - 1;
    }
    public int newMethodDescriptor (TypedDescriptor descriptor) {
        int cd = newClassDescriptor (descriptor.type ()), nt = newNameAndType (descriptor.descriptor ());
        Constant c = new Constant (descriptor.methtype (), new byte [4]);
        setShortToBytes (c.data, cd);
        setShortToBytes (c.data, nt, 2);
        constants.add (c);
        return constants.size () - 1;
    }
    //WIP
    public void debug (String outname) throws FileNotFoundException {
        PrintStream ps;
        if (outname == null) ps = System.out;
        else ps = new PrintStream (outname);
        ps.println ("Class info:");
        ps.println (String.format ("Major ver. %d Minor ver. %d", major, minor));
        ps.println (String.format ("Constant Pool Count: %d", constants.size ()));
        int i;
        for (i=1; i<constants.size(); i++) {
            ps.print (String.format ("Constant %d: ", i));
            var constant = constants.get (i);
            if (constant.type == 0) {
                ps.println ("<unavailable>");
            } else if (constant.type == 1) {
                ps.println (String.format ("UTF8 info %s", displayStr (ifStr (i))));
            } else if (constant.type == 7) {
                ps.println (String.format ("Class reference %s", ifStr (getShortFromBytes (constant.data))));
            } else if (constant.type == 8) {
                ps.println (String.format ("String %s", displayStr (ifStr (getShortFromBytes (constant.data)))));
            } else if (constant.type >= 9 && constant.type <= 11) {
                int cls_ndex, nnt_ndex;
                cls_ndex = getShortFromBytes (constant.data);
                nnt_ndex = getShortFromBytes (constant.data, 2);
                Constant nnt = constants.get (nnt_ndex);
                int name, desc;
                name = getShortFromBytes (nnt.data);
                desc = getShortFromBytes (nnt.data, 2);
                Constant cls = constants.get (cls_ndex);
                cls_ndex = getShortFromBytes (cls.data);
                String prefix;
                if (constant.type == 9) prefix = "Field";
                else if (constant.type == 10) prefix = "Class method";
                else prefix = "Interface method";
                ps.println (String.format ( "%s reference name %s type %s of class %s", prefix, ifStr (name), ifStr (desc), ifStr (cls_ndex)));
            } else {
                String out = String.format ("%s ", hex (constant.type));
                for (byte b : constant.data) out += hex (b);
                ps.println (out);
            }
        }
        ps.println (String.format ("Access Flag: %s", hex2b (accessFlags)));
        ps.println (String.format ("This class: %d, Super class: %d", thisClass, superClass));
        ps.println (String.format ("Interfaces implemented: %d", interfaces.length));
        for (i=0; i<interfaces.length; i++) {
            ps.println (String.format ("Interface %d: %d", i + 1, interfaces [i]));
        }
        ps.println (String.format ("Fields: %d", fields.size ()));
        for (i=0; i<fields.size(); i++) {
            FieldOrMethod field = fields.get (i);
            ps.println (String.format (" Field %d:", i + 1));
            ps.println (String.format (" Access flags: %s; Name: %s; Descriptor: %s", hex2b (field.accessFlags), ifStr (field.name), ifStr (field.descriptor)));
            ps.println (String.format (" Attributes: %d", field.attributes.size ()));
            int j;
            for (j=0; j<field.attributes.size(); j++) {
                ClassAttribute attr = field.attributes.get (j);
                ps.println (String.format ("  Attribute %d: name %s", j + 1, ifStr (attr.getName ())));
                attr.debug (ps, this, 3);
            }
        }
        ps.println (String.format ("Methods: %d", methods.size ()));
        for (i=0; i<methods.size(); i++) {
            FieldOrMethod method = methods.get (i);
            ps.println (String.format (" Method %d:", i + 1));
            ps.println (String.format (" Access flags: %s; Name: %s; Descriptor: %s", hex2b (method.accessFlags), ifStr (method.name), ifStr (method.descriptor)));
            ps.println (String.format (" Attributes: %d", method.attributes.size ()));
            int j;
            for (j=0; j<method.attributes.size(); j++) {
                ClassAttribute attr = method.attributes.get (j);
                ps.println (String.format ("  Attribute %d: name %s", j + 1, ifStr (attr.getName ())));
                attr.debug (ps, this, 3);
            }
        }
        ps.println (String.format ("Attributes: %d", attributes.size ()));
        for (i=0; i<attributes.size(); i++) {
            ClassAttribute attr = attributes.get (i);
            ps.println (String.format (" Attribute %d: name %s", i+1, ifStr (attr.getName ())));
            attr.debug (ps, this, 2);
        }
    }
    // Also for debug use
    public void save (String path) throws IOException {
        File file = new File (path);
        OutputStream stream = new FileOutputStream (file);
        stream.write (this.get ());
        stream.close ();
    }
    // If you want to debug
    public static void main (String[] args) throws Throwable {
        for (String name : args) {
            if (name.endsWith (".jar")) {
                JarFile jar = new JarFile (name);
                Consumer <ZipEntry> con = (entry) -> {
                    try {
                        if (! entry.getName () .endsWith (".class")) return;
                        ClassFile cf = new ClassFile (jar.getInputStream (entry) .readAllBytes ());
                        cf.debug (null);
                    } catch (IOException e) {
                        throw new RuntimeException (e);
                    }
                };
                jar.entries().asIterator().forEachRemaining(con);
                jar.close ();
            } else if (name.endsWith (".class")) {
                var is = new FileInputStream (name);
                new ClassFile (is.readAllBytes ()) .debug (null);
                is.close ();
            }
        }
    }
}
