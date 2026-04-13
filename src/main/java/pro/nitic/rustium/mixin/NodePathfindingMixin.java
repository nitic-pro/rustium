package pro.nitic.rustium.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Node.class)
public abstract class NodePathfindingMixin {
    @Shadow public int x;
    @Shadow public int y;
    @Shadow public int z;

    /**
     * @author AI
     * @reason 【Lithium互換化】パスファインディング軽量化。
     * Overwriteを使わず、HEADでInjectして早期リターン（cancellable=true）させることで競合を回避しつつ、高速近似をねじ込みます。
     */
    @Inject(method = "distanceTo(Lnet/minecraft/world/level/pathfinder/Node;)F", at = @At("HEAD"), cancellable = true)
    public void distanceTo(Node node, CallbackInfoReturnable<Float> cir) {
        pro.nitic.rustium.rustium.RustiumTestRunner.DebugStats.pathfindingCalls++;
        int dx = Math.abs(node.x - this.x);
        int dy = Math.abs(node.y - this.y);
        int dz = Math.abs(node.z - this.z);

        if (dx < dy) { int t = dx; dx = dy; dy = t; }
        if (dx < dz) { int t = dx; dx = dz; dz = t; }
        if (dy < dz) { int t = dy; dy = dz; dz = t; }

        int approx = dx + (11 * dy >> 5) + (1 * dz >> 2); 
        cir.setReturnValue((float) approx);
    }

    /**
     * @author AI
     * @reason BlockPosに対する距離計算もInteger化
     */
    @Inject(method = "distanceTo(Lnet/minecraft/core/BlockPos;)F", at = @At("HEAD"), cancellable = true)
    public void distanceTo(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        pro.nitic.rustium.rustium.RustiumTestRunner.DebugStats.pathfindingCalls++;
        int dx = Math.abs(pos.getX() - this.x);
        int dy = Math.abs(pos.getY() - this.y);
        int dz = Math.abs(pos.getZ() - this.z);

        if (dx < dy) { int t = dx; dx = dy; dy = t; }
        if (dx < dz) { int t = dx; dx = dz; dz = t; }
        if (dy < dz) { int t = dy; dy = dz; dz = t; }

        int approx = dx + (11 * dy >> 5) + (1 * dz >> 2); 
        cir.setReturnValue((float) approx);
    }
}
