package com.example.mixin;

import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 光源計算のバグを防ぐため、完全に何もしない状態（バニラのまま）に戻しました。
 */
@Mixin(LightEngine.class)
public class LightEngineMixin {
    // 処理を空にすることで、バニラの光更新エンジンが一切阻害されず動作します。
}
