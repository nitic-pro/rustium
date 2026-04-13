package pro.nitic.rustium.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class TickCullingMixin {
    private static final int CULLING_DISTANCE_SQUARED = 64 * 64; 

    @Inject(method = "baseTick", at = @At("HEAD"), cancellable = true)
    private void cullEntityTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (self.level().isClientSide() || self instanceof Player || self instanceof net.minecraft.world.entity.item.ItemEntity) {
            return; 
        }

        List<? extends Player> players = self.level().players();
        boolean hasPlayerNear = false;
        
        for (Player player : players) {
            double dx = self.getX() - player.getX();
            double  dy = self.getY() - player.getY();
            double dz = self.getZ() - player.getZ();
            double  distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < CULLING_DISTANCE_SQUARED) {
                hasPlayerNear = true;
                break;
            }
        }

        if (!hasPlayerNear) {
            pro.nitic.rustium.rustium.RustiumTestRunner.DebugStats.culledTicks++;
            ci.cancel(); 
        }
    }
}
