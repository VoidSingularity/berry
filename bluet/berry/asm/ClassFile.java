package bluet.berry.asm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Stack;

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
    public static interface Attribute {
        public void write (DataOutputStream stream) throws IOException;
        public int size ();
        public int getName ();
        default void debug (ClassFile file, int tabSpaces) {
            while (tabSpaces--!=0) System.out.print (' ');
            System.out.println ("No debug info now!");
        }
    }
    public static interface AttributeProvider {
        public Attribute newAttribute (ClassFile file, int name, int len, DataInputStream stream) throws IOException;
    }
    private static final Map <String, AttributeProvider> providers = Map.of ("Code", CodeAttribute::new);
    public Attribute newAttribute (int name, int len, DataInputStream stream) throws IOException {
        AttributeProvider provider = providers.get (if_str (name));
        if (provider == null) return new AnyAttribute (name, len, stream);
        else return provider.newAttribute (this, name, len, stream);
    }
    public static void outputSpaces (int tabSpaces) { for (int j=0; j<tabSpaces; j++) System.out.print (' '); }
    public static class AnyAttribute implements Attribute {
        public int name;
        public byte[] info;
        public AnyAttribute (int name, int len, DataInputStream input) throws IOException {
            this.name = name;
            this.info = input.readNBytes (len);
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
        public void debug (ClassFile file, int tab) {
            int k;
            for (k=0; k<info.length; k++) {
                if (k % 12 == 0) {
                    System.out.println ();
                    outputSpaces (tab);
                }
                System.out.print (hex (info [k]));
            }
            System.out.println ();
        }
    }
    public static class CodeAttribute implements Attribute {
        public int name, maxStack, maxLocals;
        public byte[] code;
        public ExceptionTable[] exceptionTables;
        public Attribute[] subAttributes;
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
            subAttributes = new Attribute [l];
            for (i=0; i<l; i++) {
                int n = 0, m = 0;
                n = input.readUnsignedShort ();
                m = input.readInt ();
                subAttributes [i] = file.newAttribute (n, m, input);
            }
        }
        @Override
        public int size () {
            int length = 18 + code.length + exceptionTables.length * 8;
            for (Attribute attr : subAttributes) length += attr.size ();
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
            stream.writeShort (subAttributes.length);
            for (Attribute attr : subAttributes) attr.write (stream);
        }
        public int getName () { return this.name; }
        public void debug (ClassFile file, int tab) {
            int k, l;
            System.out.println ();
            outputSpaces (tab);
            System.out.println (String.format ("Max stack: %d, Max locals: %d", maxStack, maxLocals));
            outputSpaces (tab);
            System.out.print ("Code:");
            for (k=0; k<code.length; k++) {
                if (k % 12 == 0) {
                    System.out.println ();
                    outputSpaces (tab+1);
                }
                System.out.print (hex (code [k]));
            }
            System.out.println ();
            outputSpaces (tab);
            System.out.println (String.format ("Exception tables: %d", exceptionTables.length));
            for (ExceptionTable t : exceptionTables) {
                outputSpaces (tab+1);
                System.out.println (String.format ("%d %d %d %d", t.start, t.end, t.handler, t.catchType));
            }
            outputSpaces (tab);
            System.out.println (String.format ("Sub-attributes: %d", subAttributes.length));
            for (l=0; l<subAttributes.length; l++) {
                Attribute a = subAttributes [l];
                outputSpaces (tab+1);
                System.out.print (String.format ("Sub-attribute %d: name %s", l + 1, file.if_str (a.getName ())));
                a.debug (file, tab+2);
            }
        }
    }
    public static class FieldOrMethod {
        public int accessFlags;
        public int name;
        public int descriptor;
        public Attribute[] attributes;
        public FieldOrMethod (int accessFlags, int name, int descriptor, Attribute[] attributes) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.descriptor = descriptor;
            this.attributes = attributes;
        }
        public void write (DataOutputStream stream) throws IOException {
            stream.writeShort (accessFlags);
            stream.writeShort (name);
            stream.writeShort (descriptor);
            stream.writeShort (attributes.length);
            for (Attribute attr : attributes) attr.write (stream);
        }
        public int length () {
            int r = 8;
            for (Attribute attr : attributes) r += attr.size ();
            return r;
        }
    }
    public FieldOrMethod[] fields;
    public FieldOrMethod[] methods;
    public Attribute[] attributes;
    // Legacy code, now I have java.io.DataInputStream and DataOutputStream :)
    // But still for debug use
    private static int wrap (byte b) {
        int r = b;
        if (r < 0) r += 256;
        return r;
    }
    // Debug flag; turn it OFF after debugging
    // The debug code is incomplete because there aren't too many bugs for now :3
    public static final boolean debug = false;
    // Another debug switch
    public static final boolean verify = false;
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
        fields = new FieldOrMethod [cnt];
        for (i=0; i<cnt; i++) {
            int a, n, d, s, j;
            a = stream.readUnsignedShort ();
            n = stream.readUnsignedShort ();
            d = stream.readUnsignedShort ();
            s = stream.readUnsignedShort ();
            Attribute[] attrs = new Attribute [s];
            for (j=0; j<s; j++) {
                int name, len;
                name = stream.readUnsignedShort ();
                len = stream.readInt ();
                attrs [j] = newAttribute (name, len, stream);
            }
            fields [i] = new FieldOrMethod (a, n, d, attrs);
        }
        cnt = stream.readUnsignedShort ();
        methods = new FieldOrMethod [cnt];
        for (i=0; i<cnt; i++) {
            int a, n, d, s, j;
            a = stream.readUnsignedShort ();
            n = stream.readUnsignedShort ();
            d = stream.readUnsignedShort ();
            s = stream.readUnsignedShort ();
            Attribute[] attrs = new Attribute [s];
            for (j=0; j<s; j++) {
                int name, len;
                name = stream.readUnsignedShort ();
                len = stream.readInt ();
                attrs [j] = newAttribute (name, len, stream);
            }
            methods [i] = new FieldOrMethod (a, n, d, attrs);
        }
        cnt = stream.readUnsignedShort ();
        attributes = new Attribute [cnt];
        for (i=0; i<cnt; i++) {
            int name, len;
            name = stream.readUnsignedShort ();
            len = stream.readInt ();
            attributes [i] = newAttribute (name, len, stream);
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
        for (Attribute attr : attributes) length += attr.size ();
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
            if (constant.type == 1) stream.writeShort (constant.data.length);
            stream.write (constant.data);
        }
        stream.writeShort (accessFlags);
        stream.writeShort (thisClass);
        stream.writeShort (superClass);
        stream.writeShort (interfaces.length);
        for (i=0; i<interfaces.length; i++) stream.writeShort (interfaces [i]);
        stream.writeShort (fields.length);
        for (FieldOrMethod f : fields) f.write (stream);
        stream.writeShort (methods.length);
        for (FieldOrMethod f : methods) f.write (stream);
        stream.writeShort (attributes.length);
        for (Attribute attr : attributes) attr.write (stream);
        var ret = out.toByteArray ();
        stream.close ();
        out.close ();
        return ret;
    }
    public String if_str (int index) {
        Constant constant = constants.get (index);
        if (constant.type != 1) return null;
        return new String (constant.data);
    }
    private String display_str (String str) {
        byte[] d = str.getBytes () .clone ();
        int i;
        for (i=0; i<d.length; i++) {
            if (d [i] < ' ' || d [i] > '~') d [i] = '?';
        }
        return new String (d);
    }
    private static String hex (int b) {
        String x = "0123456789ABCDEF";
        b = (b + 256) % 256;
        char[] c = {x.charAt (b >> 4), x.charAt (b & 15)};
        return String.valueOf (c);
    }
    private static String hex2b (int i) {
        return hex ((byte) (i / 256)) + hex ((byte) (i % 256));
    }
    //WIP
    public void debug () {
        System.out.println ("Class info:");
        System.out.println (String.format ("Major ver. %d Minor ver. %d", major, minor));
        System.out.println (String.format ("Constant Pool Count: %d", constants.size ()));
        int i;
        for (i=1; i<constants.size(); i++) {
            System.out.print (String.format ("Constant %d: ", i));
            var constant = constants.get (i);
            if (constant.type == 0) {
                System.out.println ("<unavailable>");
            } else if (constant.type == 1) {
                System.out.println (String.format ("UTF8 info %s", display_str (if_str (i))));
            } else if (constant.type == 7) {
                System.out.println (String.format ("Class reference %s", if_str (wrap (constant.data [0]) * 256 + wrap (constant.data [1]))));
            } else if (constant.type == 8) {
                System.out.println (String.format ("String %s", display_str (if_str (wrap (constant.data [0]) * 256 + wrap (constant.data [1])))));
            } else if (constant.type >= 9 && constant.type <= 11) {
                int cls_ndex, nnt_ndex;
                cls_ndex = wrap (constant.data [0]) * 256 + wrap (constant.data [1]);
                nnt_ndex = wrap (constant.data [2]) * 256 + wrap (constant.data [3]);
                Constant nnt = constants.get (nnt_ndex);
                int name, desc;
                name = wrap (nnt.data [0]) * 256 + wrap (nnt.data [1]);
                desc = wrap (nnt.data [2]) * 256 + wrap (nnt.data [3]);
                Constant cls = constants.get (cls_ndex);
                cls_ndex = wrap (cls.data [0]) * 256 + wrap (cls.data [1]);
                String prefix;
                if (constant.type == 9) prefix = "Field";
                else if (constant.type == 10) prefix = "Class method";
                else prefix = "Interface method";
                System.out.println (String.format ( "%s reference name %s type %s of class %s", prefix, if_str (name), if_str (desc), if_str (cls_ndex)));
            } else {
                String out = String.format ("%s ", hex (constant.type));
                for (byte b : constant.data) out += hex (b);
                System.out.println (out);
            }
        }
        System.out.println (String.format ("Access Flag: %s", hex2b (accessFlags)));
        System.out.println (String.format ("This class: %d, Super class: %d", thisClass, superClass));
        System.out.println (String.format ("Interfaces implemented: %d", interfaces.length));
        for (i=0; i<interfaces.length; i++) {
            System.out.println (String.format ("Interface %d: %d", i + 1, interfaces [i]));
        }
        System.out.println (String.format ("Fields: %d", fields.length));
        for (i=0; i<fields.length; i++) {
            FieldOrMethod field = fields [i];
            System.out.println (String.format (" Field %d:", i + 1));
            System.out.println (String.format (" Access flags: %s; Name: %s; Descriptor: %s", hex2b (field.accessFlags), if_str (field.name), if_str (field.descriptor)));
            System.out.println (String.format (" Attributes: %d", field.attributes.length));
            int j;
            for (j=0; j<field.attributes.length; j++) {
                Attribute attr = field.attributes [j];
                System.out.print (String.format ("  Attribute %d: name %s", j + 1, if_str (attr.getName ())));
                attr.debug (this, 3);
            }
        }
        System.out.println (String.format ("Methods: %d", methods.length));
        for (i=0; i<methods.length; i++) {
            FieldOrMethod method = methods [i];
            System.out.println (String.format (" Method %d:", i + 1));
            System.out.println (String.format (" Access flags: %s; Name: %s; Descriptor: %s", hex2b (method.accessFlags), if_str (method.name), if_str (method.descriptor)));
            System.out.println (String.format (" Attributes: %d", method.attributes.length));
            int j;
            for (j=0; j<method.attributes.length; j++) {
                Attribute attr = method.attributes [j];
                System.out.print (String.format ("  Attribute %d: name %s", j + 1, if_str (attr.getName ())));
                attr.debug (this, 3);
            }
        }
        System.out.println (String.format ("Attributes: %d", attributes.length));
        for (i=0; i<attributes.length; i++) {
            Attribute attr = attributes [i];
            System.out.print (String.format (" Attribute %d: name %s", i+1, if_str (attr.getName ())));
            attr.debug (this, 2);
        }
    }
    // Also for debug use
    public void save (String path) throws IOException {
        File file = new File (path);
        OutputStream stream = new FileOutputStream (file);
        stream.write (this.get ());
        stream.close ();
    }
}
