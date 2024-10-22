package bluet.berry.asm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public enum Type {
    BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, CLASS, SHORT, BOOLEAN, ARRAY, VOID;
    public static Type ret (String descriptor) {
        int i;
        for (i=0; i<descriptor.length(); i++)
        if (descriptor.charAt (i) == ')')
        break;
        switch (descriptor.charAt (i+1)) {
            case 'B': return BYTE;
            case 'C': return CHAR;
            case 'D': return DOUBLE;
            case 'F': return FLOAT;
            case 'I': return INT;
            case 'J': return LONG;
            case 'L': return CLASS;
            case 'S': return SHORT;
            case 'V': return VOID;
            case 'Z': return BOOLEAN;
            case '[': return ARRAY;
            default: return null;
        }
    }
    public static List <Type> args (String descriptor) {
        int i;
        List <Type> res = new ArrayList <> ();
        for (i=1; ; i++) {
            switch (descriptor.charAt (i)) {
                case 'B': res.add (BYTE); break;
                case 'C': res.add (CHAR); break;
                case 'D': res.add (DOUBLE); break;
                case 'F': res.add (FLOAT); break;
                case 'I': res.add (INT); break;
                case 'J': res.add (LONG); break;
                case 'S': res.add (SHORT); break;
                case 'Z': res.add (BOOLEAN); break;
                case ')': return res;
                case 'L': res.add (CLASS); for (; descriptor.charAt (i) != ';'; i++); break;
                case '[': res.add (ARRAY);
                    for (; descriptor.charAt (i) == '['; i++);
                    if (descriptor.charAt (i) == 'L')
                    for (; descriptor.charAt (i) != ';'; i++);
                    break;
            }
        }
    }
    public void loadInstruction (DataOutputStream stream, int index) throws IOException {
        switch (this) {
            case CLASS: case ARRAY: // aload
            if (index < 4) stream.writeByte (42 + index);
            else {
                stream.writeByte (25);
                stream.writeByte (index);
            }
            break;
            case DOUBLE: // dload
            if (index < 4) stream.writeByte (38 + index);
            else {
                stream.writeByte (24);
                stream.writeByte (index);
            }
            break;
            case FLOAT: // fload
            if (index < 4) stream.writeByte (34 + index);
            else {
                stream.writeByte (23);
                stream.writeByte (index);
            }
            break;
            case LONG: // lload
            if (index < 4) stream.writeByte (30 + index);
            else {
                stream.writeByte (22);
                stream.writeByte (index);
            }
            break;
            default: // iload
            if (index < 4) stream.writeByte (26 + index);
            else {
                stream.writeByte (21);
                stream.writeByte (index);
            }
            break;
        }
    }
}
