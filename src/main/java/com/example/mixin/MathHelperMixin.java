package com.example.mixin;

import com.example.rustium.math.FastTrig;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mth.class)
public class MathHelperMixin {
    @Inject(method = "sin(D)F", at = @At("HEAD"), cancellable = true)
    private static void fastSin(double d, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FastTrig.sin(d));
    }

    @Inject(method = "cos(D)F", at = @At("HEAD"), cancellable = true)
    private static void fastCos(double d, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FastTrig.cos(d));
    }
}
