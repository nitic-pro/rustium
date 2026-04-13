package pro.nitic.rustium.rustium;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.concurrent.CompletableFuture;
import java.util.Collections;

public class RustiumTestRunner {

    public static class DebugStats {
        public static long rustCalls = 0;
        public static long simdCalls = 0;
        public static long culledTicks = 0;
        public static long pathfindingCalls = 0;
    }

    public static void runTests(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            var server = source.getServer();
            ServerLevel sLevel = (ServerLevel) player.level();
            
            source.sendSuccess(() -> Component.literal("§a[Rustium bench] §bデバッグテストを開始します... (約6秒かかります)"), false);
            
            final long startRust = DebugStats.rustCalls;
            final long startSimd = DebugStats.simdCalls;
            final long startCull = DebugStats.culledTicks;
            final long startPath = DebugStats.pathfindingCalls;
            
            source.sendSuccess(() -> Component.literal("§c[Test 1] 500ブロック先へテレポート (新規チャンクRust・通常生成テスト)"), false);
            
            // 落下耐性をつけてサバイバルに変更（正確にAIを稼働させるため）
            player.setGameMode(GameType.SURVIVAL);
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 255, false, false)); // 20秒無敵
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false)); // 20秒落下軽減
            
            player.teleportTo(sLevel, player.getX() + 500, 150, player.getZ() + 500, Collections.emptySet(), player.getYRot(), player.getXRot(), false);
            
            CompletableFuture.runAsync(() -> {
                try { Thread.sleep(3000); } catch (Exception e){} // チャンク生成待ち
                
                server.execute(() -> {
                    source.sendSuccess(() -> Component.literal("§c[Test 2] 周囲にグロウストーンを格子状に設置 (SIMD光計算ストレステスト)"), false);
                    BlockPos pPos = player.blockPosition();
                    
                    // 足場を確保 (落下死防止念押し)
                    for(int dx = -5; dx <= 5; dx++) {
                        for(int dz = -5; dz <= 5; dz++) {
                            sLevel.setBlockAndUpdate(pPos.offset(dx, -1, dz), Blocks.GLASS.defaultBlockState());
                        }
                    }

                    // グロウストーン配置
                    for(int dx = -5; dx <= 5; dx += 2) {
                        for(int dz = -5; dz <= 5; dz += 2) {
                            sLevel.setBlockAndUpdate(pPos.offset(dx, 0, dz), Blocks.GLOWSTONE.defaultBlockState());
                        }
                    }
                    
                    source.sendSuccess(() -> Component.literal("§c[Test 3] ゾンビ100体召喚 (AI経路・Cullingテスト)"), false);
                    for(int i = 0; i < 100; i++) {
                        Zombie z = EntityType.ZOMBIE.create(sLevel, EntitySpawnReason.COMMAND);
                        if (z != null) {
                            z.setPos(player.getX() + (Math.random() * 10 - 5), player.getY(), player.getZ() + (Math.random() * 10 - 5));
                            z.setTarget(player);
                            // 確実に近接ターゲットにしてA*を強要
                            z.getNavigation().moveTo(player, 1.0D);
                            sLevel.addFreshEntity(z);
                        }
                    }
                });
                
                try { Thread.sleep(2000); } catch (Exception e){} // ゾンビのPathfinding/Culling、光源アップデート待ち
                
                server.execute(() -> {
                    long diffRust = DebugStats.rustCalls - startRust;
                    long diffSimd = DebugStats.simdCalls - startSimd;
                    long diffCull = DebugStats.culledTicks - startCull;
                    long diffPath = DebugStats.pathfindingCalls - startPath;
                    
                    source.sendSuccess(() -> Component.literal("§a✅ 全テスト完了！ このテスト間に発生した最適化回数:"), false);
                    source.sendSuccess(() -> Component.literal(" - 🦀 Rust ノイズ生成フック: " + diffRust + " 回"), false);
                    source.sendSuccess(() -> Component.literal(" - 🔦 SIMD 光配列スキャン: " + diffSimd + " 回"), false);
                    source.sendSuccess(() -> Component.literal(" - 🛡 遠距離処理カリング: " + diffCull + " tickスキップ"), false);
                    source.sendSuccess(() -> Component.literal(" - 🧠 整数近似ルート検索: " + diffPath + " 回"), false);
                });
            });
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("プレイヤーとして実行してください！"));
        }
    }
}
