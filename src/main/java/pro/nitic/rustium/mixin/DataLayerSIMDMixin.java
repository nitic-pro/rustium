package pro.nitic.rustium.mixin;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import net.minecraft.world.level.chunk.DataLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataLayer.class)
public abstract class DataLayerSIMDMixin {

    @Shadow protected byte[] data;

    // 最新のSIMDレジスタ長を自動最適化（AVX2なら256bit=32byte/回、AVX-512なら512bit=64byte/回）
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    /**
     * Minecraftの光データ（2048バイト）が全て空（0）かどうかを走査する処理を、
     * Java 25 Vector APIで数十倍に高速化する。
     */
    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true)
    private void optIsEmptySIMD(CallbackInfoReturnable<Boolean> cir) {
        if (this.data == null) {
            cir.setReturnValue(true);
        pro.nitic.rustium.rustium.RustiumTestRunner.DebugStats.simdCalls++;
            return;
        }

        int upperBound = SPECIES.loopBound(this.data.length);
        ByteVector zeroVec = ByteVector.zero(SPECIES);

        int i = 0;
        // 一気に256bit(32個のブロック光データ)等をもぎ取って1クロックで比較！
        for (; i < upperBound; i += SPECIES.length()) {
            ByteVector v = ByteVector.fromArray(SPECIES, this.data, i);
            if (v.compare(VectorOperators.NE, zeroVec).anyTrue()) {
                cir.setReturnValue(false); // 1つでも光が含まれていれば空ではない
                return;
            }
        }

        // 残像処理（端数）
        for (; i < this.data.length; i++) {
            if (this.data[i] != 0) {
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(true);
        pro.nitic.rustium.rustium.RustiumTestRunner.DebugStats.simdCalls++;
    }
}
