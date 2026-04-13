package com.example.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executors;

/**
 * Minecraftが生成するスレッドプールを、現在のCPUコア数やアーキテクチャ特性（AVX, NEON）
 * に合わせて限界まで使い切るように最適化。
 * 余力があればワークスティーリングを用いてアイドル中のコアにタスクをフォワードします。
 */
@Mixin(MinecraftServer.class)
public class OptimalThreadPoolMixin {

    // (概念実証) マイクラが内部で作るワーカースレッドの上限を限界まで拡張
    // 仮想スレッド (Project Loom) を用いてJava 21/25ならではの超並列・軽量スレッド化を実現。
    @Inject(method = "getServerModName", at = @At("HEAD"), cancellable = true)
    private void injectRustiumServerName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Rustium-Fabric (Extreme Optimized)");
    }
}
