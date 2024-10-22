package bluet.berry.api;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluet.berry.asm.ClassFile;
import bluet.berry.asm.TypedDescriptor;
import bluet.berry.asm.ClassFile.AnyAttribute;
import bluet.berry.asm.ClassFile.ClassAttribute;
import bluet.berry.asm.ClassFile.CodeAttribute;
import bluet.berry.asm.ClassFile.FieldOrMethod;
import bluet.berry.loader.BerryClassTransformer;
import bluet.berry.asm.NameAndType;
import bluet.berry.asm.Type;

public class BerryDefaultBaseMod {
    public static void initialize () {
    }
    public static void test () {
        System.out.println ("Test!");
    }
    public static void inject (ClassFile clazz, NameAndType target, List <TypedDescriptor> headhooks, List <TypedDescriptor> tailhooks) throws IOException {
        //
        int optional = clazz.newClassDescriptor ("java/util/Optional");
        int optisemp = clazz.newMethodDescriptor (new TypedDescriptor ("java/util/Optional", new NameAndType ("isEmpty", "()Z"), 10));
        int optalget = clazz.newMethodDescriptor (new TypedDescriptor ("java/util/Optional", new NameAndType ("get", "()Ljava/lang/Object;"), 10));
        // See the todo below
        int retstype = clazz.newUTF8 (target.type () .split (")L") [1] .split (";") [0]);
        // Move the method
        String newmeth = target.name () + "$mixed";
        FieldOrMethod meth = clazz.methods.get (clazz.getMethod (target));
        clazz.methods.add (new FieldOrMethod (meth.accessFlags, clazz.newUTF8 (newmeth), meth.descriptor, meth.attributes));
        ByteArrayOutputStream baos; DataOutputStream stream;
        // Construct the byte code
        // load args
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        int i=0;
        if ((meth.accessFlags & 8) == 0) Type.CLASS.loadInstruction (stream, i++);
        for (Type t : Type.args (target.type ())) {
            t.loadInstruction (stream, i++);
        }
        byte[] headload = baos.toByteArray () .clone ();
        Type.ret (target.type ()) .loadInstruction (stream, ++i);
        byte[] tailload = baos.toByteArray () .clone ();
        stream.close (); baos.close ();
        // invokestatic
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        // astore
        if (i < 5) stream.writeByte (74 + i);
        else {
            stream.writeByte (58);
            stream.writeByte (i-1);
        }
        // aload
        if (i < 5) stream.writeByte (41 + i);
        else {
            stream.writeByte (25);
            stream.writeByte (i-1);
        }
        // invokevirtual
        stream.writeByte (0xb6);
        stream.writeShort (optisemp);
        byte[] p2 = baos.toByteArray ();
        stream.close (); baos.close ();
        // ifne
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        // aload
        if (i < 5) stream.writeByte (41 + i);
        else {
            stream.writeByte (25);
            stream.writeByte (i-1);
        }
        // invokevirtual
        stream.writeByte (0xb6);
        stream.writeShort (optalget);
        // checkcast
        // TODO: support non-Object return type
        stream.writeByte (0xc0);
        stream.writeShort (retstype);
        // areturn
        // See the todo above
        byte[] p3 = baos.toByteArray ();
        stream.close (); baos.close ();
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        for (TypedDescriptor hook : headhooks) {
            stream.write (headload);
            stream.writeByte (0xb8);
            stream.writeShort (clazz.newMethodDescriptor (hook));
            stream.write (p2);
            stream.writeByte (0x9a);
            stream.writeShort (0x0b);
            stream.write (p3);
            stream.write (0xb0);
        }
        stream.write (headload);
        if ((meth.accessFlags & 8) == 0) stream.writeByte (0xb6);
        else stream.writeByte (0xb8);
        stream.writeShort (clazz.newMethodDescriptor (new TypedDescriptor (clazz.ifStr (ClassFile.getShortFromBytes (clazz.constants.get (clazz.thisClass) .data)), new NameAndType (newmeth, target.type ()), 10)));
        // TODO
        stream.writeByte (0x3a);
        stream.writeByte (i);
        for (TypedDescriptor hook : tailhooks) {
            stream.write (tailload);
            stream.writeByte (0xb8);
            stream.writeShort (clazz.newMethodDescriptor (hook));
            stream.write (p2);
            stream.writeByte (0x9a);
            stream.writeShort (0x0c);
            stream.write (p3);
            stream.writeByte (0x3a);
            stream.writeByte (i);
        }
        Type.CLASS.loadInstruction (stream, i);
        stream.writeByte (0xb0);
        byte[] code = baos.toByteArray ();
        baos.close (); stream.close ();
        int hl, ml, tl;
        hl = headload.length + p2.length + p3.length + 7;
        ml = headload.length + 5;
        tl = tailload.length + p2.length + p3.length + 8;
        // Construct the stack map table
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        stream.writeShort (headhooks.size () + tailhooks.size ());
        if (headhooks.size () > 0) {
            stream.writeByte (0xfc);
            stream.writeShort (hl);
            stream.writeByte (0x07);
            stream.writeShort (optional);
            int ofs = hl - 1;
            for (i=1; i<headhooks.size(); i++) {
                if (ofs < 64) stream.writeByte (ofs);
                else {
                    stream.writeByte (251);
                    stream.writeShort (ofs);
                }
            }
            if (tailhooks.size () > 0) {
                stream.writeByte (0xfc);
                stream.writeShort (ml+tl-1);
                stream.writeByte (0x07);
                stream.writeShort (retstype);
                ofs = tl - 1;
                for (i=1; i<tailhooks.size(); i++) {
                    if (ofs < 64) stream.writeByte (ofs);
                    else {
                        stream.writeByte (251);
                        stream.writeShort (ofs);
                    }
                }
            }
        } else {
            stream.writeByte (0xfd);
            stream.writeShort (ml+tl);
            stream.writeByte (0x07);
            stream.writeShort (optional);
            stream.writeByte (0x07);
            stream.writeShort (retstype);
            int ofs = tl - 1;
            for (i=1; i<tailhooks.size(); i++) {
                if (ofs < 64) stream.writeByte (ofs);
                else {
                    stream.writeByte (251);
                    stream.writeShort (ofs);
                }
            }
        }
        byte[] smt = baos.toByteArray ();
        baos.close (); stream.close ();
        List <ClassAttribute> li = new ArrayList <> ();
        // Find the Code attribute
        CodeAttribute codeattr = null;
        for (i=0; i<meth.attributes.size(); i++) {
            ClassAttribute attr = meth.attributes.get (i);
            if (attr instanceof CodeAttribute c) codeattr = c;
            else li.add (attr);
        }
        if (codeattr == null) return;
        li.add (new CodeAttribute (clazz, codeattr.getName (), codeattr, code, Map.of ("StackMapTable", new AnyAttribute (clazz.newUTF8 ("StackMapTable"), smt)), Set.of ("LineNumberTable")));
        meth.attributes = li;
    }
    public static void injectVoid (ClassFile clazz, NameAndType target, List <TypedDescriptor> headhooks, List <TypedDescriptor> tailhooks) throws IOException {
        //
        String newmeth = target.name () + "$mixed";
        if (newmeth.equals ("<init>$mixed")) newmeth = "constructor$mixed";
        else if (newmeth.equals ("<clinit>$mixed")) newmeth = "clinit$mixed";
        FieldOrMethod meth = clazz.methods.get (clazz.getMethod (target));
        clazz.methods.add (new FieldOrMethod (meth.accessFlags, clazz.newUTF8 (newmeth), meth.descriptor, meth.attributes));
        ByteArrayOutputStream baos; DataOutputStream stream;
        // Construct the byte code
        // load args
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        int i=0;
        if ((meth.accessFlags & 8) == 0) Type.CLASS.loadInstruction (stream, i++);
        for (Type t : Type.args (target.type ())) {
            t.loadInstruction (stream, i++);
        }
        byte[] load = baos.toByteArray () .clone ();
        stream.close (); baos.close ();
        baos = new ByteArrayOutputStream ();
        stream = new DataOutputStream (baos);
        for (TypedDescriptor hook : headhooks) {
            stream.write (load);
            stream.writeByte (0xb8);
            stream.writeShort (clazz.newMethodDescriptor (hook));
        }
        stream.write (load);
        if ((meth.accessFlags & 8) == 0) stream.writeByte (0xb6);
        else stream.writeByte (0xb8);
        stream.writeShort (clazz.newMethodDescriptor (new TypedDescriptor (clazz.ifStr (ClassFile.getShortFromBytes (clazz.constants.get (clazz.thisClass) .data)), new NameAndType (newmeth, target.type ()), 10)));
        //
        for (TypedDescriptor hook : tailhooks) {
            stream.write (load);
            stream.writeByte (0xb8);
            stream.writeShort (clazz.newMethodDescriptor (hook));
        }
        stream.writeByte (0xb1);
        byte[] code = baos.toByteArray ();
        baos.close (); stream.close ();
        List <ClassAttribute> li = new ArrayList <> ();
        // clazz.debug ();
        // Find the Code attribute
        CodeAttribute codeattr = null;
        for (i=0; i<meth.attributes.size(); i++) {
            ClassAttribute attr = meth.attributes.get (i);
            if (attr instanceof CodeAttribute c) codeattr = c;
            else li.add (attr);
        }
        if (codeattr == null) return;
        // clazz.debug ();
        li.add (new CodeAttribute (clazz, codeattr.getName (), codeattr, code, Map.of (), Set.of ("LineNumberTable")));
        meth.attributes = li;
    }
}
