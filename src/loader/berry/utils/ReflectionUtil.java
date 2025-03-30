package berry.utils;

import java.lang.reflect.Field;

public class ReflectionUtil {
    public static Object getFieldFrom (Class <?> source, Object obj, String name) {
        try {
            Field f = null;
            for (Field g : source.getDeclaredFields ()) {
                if (g.getName () .equals (name)) {
                    f = g;
                    break;
                }
            }
            if (f == null) return null;
            f.setAccessible (true);
            return f.get (obj);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
