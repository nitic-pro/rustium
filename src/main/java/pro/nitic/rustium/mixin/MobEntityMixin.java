package pro.nitic.rustium.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void optimizeAiIntCalculation(CallbackInfo ci) {
        // Disabled manual setPos override. 
        // setPos bypasses the collision engine (move method), causing mobs to phase through blocks.
        // Proper fixed-point AI requires overriding PathNavigation and Node calculations.
    }
}
