package com.example.mixin;

import com.example.rustium.RustNativeOffloader;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@Mixin(ImprovedNoise.class)
public abstract class ImprovedNoiseMixin {

    @Shadow
    public byte[] p;

    private MemorySegment pArraySegment;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(net.minecraft.util.RandomSource randomSource, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        // Allocate off-heap memory for the probability array once per noise instance
        com.example.rustium.RustiumTestRunner.DebugStats.rustCalls++;
        pArraySegment = RustNativeOffloader.ARENA.allocate(256);
        for (int i = 0; i < 256; i++) {
            pArraySegment.set(ValueLayout.JAVA_BYTE, i, p[i]);
        }
    }

    @Inject(method = "noise(DDD)D", at = @At("HEAD"), cancellable = true)
    private void onNoise(double x, double y, double z, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Double> cir) {
        
        if (pArraySegment != null) {
            double result = RustNativeOffloader.computeNoiseSingle(x, y, z, pArraySegment);
            cir.setReturnValue(result);
        }
    }
}
