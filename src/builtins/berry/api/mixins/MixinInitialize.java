package berry.api.mixins;

import java.lang.reflect.Method;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import berry.loader.BerryClassTransformer;

public class MixinInitialize {
    public static void initialize () {
        MixinBootstrap.init ();
        try {
			Method m = MixinEnvironment.class.getDeclaredMethod ("gotoPhase", MixinEnvironment.Phase.class);
			m.setAccessible (true);
			m.invoke (null, MixinEnvironment.Phase.INIT);
			m.invoke (null, MixinEnvironment.Phase.DEFAULT);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
        // hello world
        Mixins.addConfiguration ("builtin_mixins.json");
        BerryClassTransformer.instance () .all .add (
            (loader, name, clazz, domain, code) -> {
                try {
                    name = name.replace ('/', '.');
                    return BerryMixinService.transformer.transformClassBytes (name, name, code);
                } catch (Throwable t) {
                    System.err.println (String.format ("[BERRY/MIXIN] Error transforming class %s", name));
                    t.printStackTrace ();
                    return null;
                }
            }
        );
    }
}
