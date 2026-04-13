package pro.nitic.rustium.mixin;

import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public abstract class FastExplosionMixin {
    
    /**
     * @author AI
     * @reason 【Lithium互換化: 爆発レイキャストの近似演算】
     * Overwriteを使わずに @Inject(HEAD) で横入りし、完全に独自の近似計算結果を即座に Return させることで他MODとの激しい競合を抑えます。
     */
    @Inject(method = "getSeenPercent", at = @At("HEAD"), cancellable = true)
    private static void getSeenPercent(Vec3 explosionCenter, Entity entity, CallbackInfoReturnable<Float> cir) {
        net.minecraft.world.phys.AABB aabb = entity.getBoundingBox();
        
        // 判定する9点（中心 ＋ 当たり判定の8つの頂点）
        Vec3[] points = new Vec3[]{
            aabb.getCenter(),
            new Vec3(aabb.minX, aabb.minY, aabb.minZ),
            new Vec3(aabb.maxX, aabb.minY, aabb.minZ),
            new Vec3(aabb.minX, aabb.minY, aabb.maxZ),
            new Vec3(aabb.maxX, aabb.minY, aabb.maxZ),
            new Vec3(aabb.minX, aabb.maxY, aabb.minZ),
            new Vec3(aabb.maxX, aabb.maxY, aabb.minZ),
            new Vec3(aabb.minX, aabb.maxY, aabb.maxZ),
            new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)
        };
        
        int visibleCount = 0;
        for (Vec3 pt : points) {
            ClipContext context = new ClipContext(
                explosionCenter, 
                pt, 
                ClipContext.Block.COLLIDER, 
                ClipContext.Fluid.NONE, 
                entity
            );
            // 障害物に当たらなかった（Miss）ら見えていると判定
            if (entity.level().clip(context).getType() == HitResult.Type.MISS) {
                visibleCount++;
            }
        }
        
        // 9本中何本通ったかで露出度（0.0 ~ 1.0）を返す
        cir.setReturnValue(visibleCount / 9.0F);
    }
}
