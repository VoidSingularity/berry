package berry.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtil {
    public static Field getField (Class <?> source, String name) {
        Field f = null;
        for (Field g : source.getDeclaredFields ()) {
            if (g.getName () .equals (name)) {
                f = g;
                break;
            }
        }
        if (f == null) return null;
        f.setAccessible (true);
        return f;
    }
    public static Object getFieldFrom (Class <?> source, Object obj, String name) {
        try {
            Field f = getField (source, name);
            return f.get (obj);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
    public static void setFieldTo (Class <?> source, Object obj, String name, Object value) {
        try {
            Field f = getField (source, name);
            f.set (obj, value);
        } catch (ReflectiveOperationException e) {}
    }
    public static Method getMethod (Class <?> source, String name) {
        for (Method m : source.getDeclaredMethods ()) {
            if (name.equals (m.getName ())) {
                m.setAccessible (true);
                return m;
            }
        }
        return null;
    }
    public static Type[] getGenerics (Class <?> source) {
        Type type = source.getGenericSuperclass ();
        if (type instanceof ParameterizedType ptype) return ptype.getActualTypeArguments ();
        return null;
    }
}
