package berry.api.mixins;

import org.spongepowered.asm.launch.MixinBootstrap;

import berry.loader.BerryClassTransformer;

public class MixinInitialize {
    public static void initialize () {
        MixinBootstrap.init ();
        BerryClassTransformer.instance () .all .add (
            (loader, name, clazz, domain, code) -> {
                name = name.replace ('/', '.');
                return BerryMixinService.transformer.transformClassBytes (name, name, code);
            }
        );
    }
}
