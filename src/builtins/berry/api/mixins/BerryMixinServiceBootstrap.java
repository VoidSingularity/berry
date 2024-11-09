package berry.api.mixins;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class BerryMixinServiceBootstrap implements IMixinServiceBootstrap {
    @Override public String getName () { return "Berry"; }
    @Override public String getServiceClassName () { return BerryMixinService.class.getName (); }
    @Override public void bootstrap () {  }
}
