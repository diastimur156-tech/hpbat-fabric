package com.example.hpbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class HpBarClientMod implements ClientModInitializer {
    private static ModConfig config;
    private static KeyBinding openConfigKey;

    private static LivingEntity lastTarget = null;
    private static long lastTargetSeenMs = 0L;

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hpbar.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.hpbar.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HpBarConfigScreen(null, config));
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.options.hudHidden) {
                return;
            }
            if (client.currentScreen instanceof HpBarConfigScreen) {
                return;
            }

            int screenWidth = drawContext.getScaledWindowWidth();
            int screenHeight = drawContext.getScaledWindowHeight();

            // 1. Svoe HP nad khotbarom (po centru snizu, kak na video)
            float playerTotalHp = client.player.getHealth() + client.player.getAbsorptionAmount();
            float playerMaxHp = client.player.getMaxHealth() + client.player.getAbsorptionAmount();

            NumericHpRenderer.renderCenteredHp(
                    drawContext,
                    client.textRenderer,
                    playerTotalHp,
                    playerMaxHp,
                    (screenWidth / 2) + config.playerOffsetX,
                    screenHeight + config.playerOffsetY,
                    config.playerScale,
                    config.showDecimals,
                    false
            );

            // 2. HP vraga pod pricelem
            LivingEntity lookedTarget = findLookedAtTarget(client);
            long now = System.currentTimeMillis();

            if (lookedTarget != null) {
                lastTarget = lookedTarget;
                lastTargetSeenMs = now;
            } else if (lastTarget != null && (!lastTarget.isAlive() || lastTarget.isRemoved() || now - lastTargetSeenMs > 1800L)) {
                lastTarget = null;
            }

            if (lastTarget != null) {
                float targetTotalHp = lastTarget.getHealth() + lastTarget.getAbsorptionAmount();
                float targetMaxHp = lastTarget.getMaxHealth() + lastTarget.getAbsorptionAmount();

                NumericHpRenderer.renderCenteredHp(
                        drawContext,
                        client.textRenderer,
                        targetTotalHp,
                        targetMaxHp,
                        (screenWidth / 2) + config.targetOffsetX,
                        (screenHeight / 2) + config.targetOffsetY,
                        config.targetScale,
                        config.showDecimals,
                        false
                );
            }
        });
    }

    private LivingEntity findLookedAtTarget(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            if (entityHit.getEntity() instanceof LivingEntity living && living != client.player && living.isAlive()) {
                return living;
            }
        }
        return null;
    }
}
