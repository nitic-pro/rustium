package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import static net.minecraft.commands.Commands.literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RustiumMod implements ModInitializer {
        public static final String MOD_ID = "modid";
        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitialize() {
                LOGGER.info("🚀 Rustium (Rustium) プロジェクト: 極限最適化サーバー起動...");
                
                try {
                    com.example.rustium.RustNativeOffloader.ARENA.allocate(1);
                    LOGGER.info("✅ Rust Native Offload Module loaded successfully! (FFM API active)");
                } catch(Exception e) {
                    LOGGER.warn("⚠️ Rust Native Engine loading failed", e);
                }

                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                    dispatcher.register(literal("rustiumbench").executes(context -> {
                        com.example.rustium.RustiumTestRunner.runTests(context.getSource());
                        return 1;
                    }));
                });

                LOGGER.info("⚡ DoD & Fixed Point API (Vector Math) fully initialized.");
        }
}
