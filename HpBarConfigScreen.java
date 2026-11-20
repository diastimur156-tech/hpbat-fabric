package com.example.hpbar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HpBarConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private enum DragTarget {
        NONE,
        TARGET,
        PLAYER
    }

    private DragTarget activeDrag = DragTarget.NONE;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public HpBarConfigScreen(Screen parent, ModConfig config) {
        super(Text.literal("Настройка индикаторов HP"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int buttonWidth = 135;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int topY = 16;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Дробные: " + (config.showDecimals ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    config.showDecimals = !config.showDecimals;
                    button.setMessage(Text.literal("Дробные: " + (config.showDecimals ? "ВКЛ" : "ВЫКЛ")));
                    config.save();
                }
        ).dimensions(centerX - 210, topY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Сбросить"),
                button -> {
                    config.resetDefaults();
                    config.save();
                }
        ).dimensions(centerX - 67, topY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Готово"),
                button -> close()
        ).dimensions(centerX + 76, topY, buttonWidth, buttonHeight).build());
    }

    private int getTargetScreenX() {
        return (this.width / 2) + config.targetOffsetX;
    }

    private int getTargetScreenY() {
        return (this.height / 2) + config.targetOffsetY;
    }

    private int getPlayerScreenX() {
        return (this.width / 2) + config.playerOffsetX;
    }

    private int getPlayerScreenY() {
        return this.height + config.playerOffsetY;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x55000000);
        super.render(context, mouseX, mouseY, delta);

        // Risuyem orientir pricela v centre ekrana
        int cx = this.width / 2;
        int cy = this.height / 2;
        context.fill(cx - 5, cy, cx + 6, cy + 1, 0x88FFFFFF);
        context.fill(cx, cy - 5, cx + 1, cy + 6, 0x88FFFFFF);

        MinecraftClient mc = MinecraftClient.getInstance();
        float playerHp = mc.player != null ? mc.player.getHealth() : 16.5f;
        float playerMaxHp = mc.player != null ? mc.player.getMaxHealth() : 20.0f;

        // 1. Primer HP vraga pod pricelem (zhelto-oranzhevyj dlya naglyadnosti)
        NumericHpRenderer.renderCenteredHp(
                context,
                this.textRenderer,
                11.45f,
                20.0f,
                getTargetScreenX(),
                getTargetScreenY(),
                config.targetScale,
                config.showDecimals,
                true
        );

        // 2. Svoe HP nad khotbarom
        NumericHpRenderer.renderCenteredHp(
                context,
                this.textRenderer,
                playerHp,
                playerMaxHp,
                getPlayerScreenX(),
                getPlayerScreenY(),
                config.playerScale,
                config.showDecimals,
                true
        );

        String hint1 = "ЛКМ + тянуть — переместить цифры | Колёсико мыши над цифрой — размер";
        String hint2 = String.format("Враг: x=%.2f | Своё: x=%.2f | ESC — сохранить",
                config.targetScale, config.playerScale);

        context.drawCenteredTextWithShadow(this.textRenderer, hint1, cx, 46, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, hint2, cx, 58, 0xFFCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            String targetSample = NumericHpRenderer.formatHp(11.45f, config.showDecimals);
            if (NumericHpRenderer.isPointOver(mouseX, mouseY, this.textRenderer, targetSample,
                    getTargetScreenX(), getTargetScreenY(), config.targetScale)) {
                activeDrag = DragTarget.TARGET;
                dragOffsetX = mouseX - getTargetScreenX();
                dragOffsetY = mouseY - getTargetScreenY();
                return true;
            }

            String playerSample = NumericHpRenderer.formatHp(20.0f, config.showDecimals);
            if (NumericHpRenderer.isPointOver(mouseX, mouseY, this.textRenderer, playerSample,
                    getPlayerScreenX(), getPlayerScreenY(), config.playerScale)) {
                activeDrag = DragTarget.PLAYER;
                dragOffsetX = mouseX - getPlayerScreenX();
                dragOffsetY = mouseY - getPlayerScreenY();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && activeDrag != DragTarget.NONE) {
            int newScreenX = (int) Math.round(mouseX - dragOffsetX);
            int newScreenY = (int) Math.round(mouseY - dragOffsetY);

            if (activeDrag == DragTarget.TARGET) {
                config.targetOffsetX = newScreenX - (this.width / 2);
                config.targetOffsetY = newScreenY - (this.height / 2);
            } else if (activeDrag == DragTarget.PLAYER) {
                config.playerOffsetX = newScreenX - (this.width / 2);
                config.playerOffsetY = newScreenY - this.height;
            }
            config.clampValues();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && activeDrag != DragTarget.NONE) {
            activeDrag = DragTarget.NONE;
            config.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float step = (float) (verticalAmount * 0.1f);
        if (step != 0.0f) {
            String targetSample = NumericHpRenderer.formatHp(11.45f, config.showDecimals);
            if (NumericHpRenderer.isPointOver(mouseX, mouseY, this.textRenderer, targetSample,
                    getTargetScreenX(), getTargetScreenY(), config.targetScale)) {
                config.targetScale += step;
                config.clampValues();
                config.save();
                return true;
            }

            String playerSample = NumericHpRenderer.formatHp(20.0f, config.showDecimals);
            if (NumericHpRenderer.isPointOver(mouseX, mouseY, this.textRenderer, playerSample,
                    getPlayerScreenX(), getPlayerScreenY(), config.playerScale)) {
                config.playerScale += step;
                config.clampValues();
                config.save();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
