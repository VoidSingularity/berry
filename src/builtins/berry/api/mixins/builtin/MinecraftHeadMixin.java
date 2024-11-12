package berry.api.mixins.builtin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;

@Mixin (Minecraft.class)
public abstract class MinecraftHeadMixin {
    @Inject (at = @At ("HEAD"), method = "<init>", remap = false)
    private static void hello (GameConfig cfg, CallbackInfo ci) {
        System.out.println ("[BERRY/MIXIN] Hello, world!");
    }
}
