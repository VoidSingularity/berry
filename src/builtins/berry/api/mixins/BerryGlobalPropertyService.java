package berry.api.mixins;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class BerryGlobalPropertyService implements IGlobalPropertyService {
    public static record MixinStringPropertyKey (String key) implements IPropertyKey {}
    private final Map <String, Object> properties = new HashMap <> ();
    @Override public IPropertyKey resolveKey (String name) { return new MixinStringPropertyKey (name); }
    @Override public <T> T getProperty (IPropertyKey key) { return (T) properties.get (((MixinStringPropertyKey) key) .key); }
    @Override public <T> T getProperty (IPropertyKey key, T fb) { return (T) properties.getOrDefault (((MixinStringPropertyKey) key) .key, fb); }
    @Override public void setProperty (IPropertyKey key, Object value) { properties.put (((MixinStringPropertyKey) key) .key, value); }
    @Override public String getPropertyString (IPropertyKey key, String fb) {
        Object o = properties.get (((MixinStringPropertyKey) key) .key);
        return o == null ? fb : o.toString ();
    }
}
