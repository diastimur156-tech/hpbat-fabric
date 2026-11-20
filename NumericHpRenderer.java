package com.example.hpbar;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.Locale;

public class NumericHpRenderer {

    /**
     * Risuet minimalistichnuyu cifru HP po centru (kak na PvP-klientakh pod pricelem i nad khotbarom).
     */
    public static void renderCenteredHp(
            DrawContext context,
            TextRenderer textRenderer,
            float hp,
            float maxHp,
            int centerX,
            int centerY,
            float scale,
            boolean showDecimals,
            boolean showEditBox
    ) {
        if (maxHp <= 0.0f) {
            return;
        }

        float safeHp = Math.max(0.0f, hp);
        float ratio = Math.min(1.0f, safeHp / maxHp);
        int textColor = getGradientColor(ratio) | 0xFF000000;

        String text = formatHp(safeHp, showDecimals);
        int textWidth = textRenderer.getWidth(text);
        int textHeight = textRenderer.fontHeight;

        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate((float) centerX, (float) centerY, 0.0f);
        matrices.scale(scale, scale, 1.0f);

        int drawX = -textWidth / 2;
        int drawY = -textHeight / 2;

        if (showEditBox) {
            // Podsvetka ramki v rezhime redaktirovaniya (klavisha H)
            context.fill(drawX - 4, drawY - 3, drawX + textWidth + 4, drawY + textHeight + 2, 0x66000000);
            context.drawBorder(drawX - 4, drawY - 3, textWidth + 8, textHeight + 5, 0xFFFFD700);
        }

        // Chistaya cifra s ten'yu bez fona (PvP-stil')
        context.drawTextWithShadow(textRenderer, text, drawX, drawY, textColor);

        matrices.pop();
    }

    public static String formatHp(float hp, boolean showDecimals) {
        if (showDecimals) {
            // Format kak "14.73" ili "16.5" (ubiraem lishnie nuli v konce)
            String formatted = String.format(Locale.US, "%.2f", hp);
            if (formatted.endsWith("00")) {
                return formatted.substring(0, formatted.length() - 3);
            } else if (formatted.endsWith("0")) {
                return formatted.substring(0, formatted.length() - 1);
            }
            return formatted;
        } else {
            return String.valueOf(Math.round(hp));
        }
    }

    /**
     * Plavnyj perekhod cveta po HSV:
     * 100% HP -> Zelenyj (120 deg / 0.333f)
     *  65% HP -> Salato-zheltyj
     *  50% HP -> Zheltyj (60 deg / 0.166f)
     *  25% HP -> Oranzhevyj (30 deg / 0.083f)
     *   0% HP -> Krasnyj (0 deg / 0.0f)
     */
    public static int getGradientColor(float ratio) {
        float clamped = Math.max(0.0f, Math.min(1.0f, ratio));
        float hue = clamped * 0.333f;
        return Color.HSBtoRGB(hue, 0.90f, 1.0f);
    }

    public static boolean isPointOver(
            double mouseX,
            double mouseY,
            TextRenderer textRenderer,
            String sampleText,
            int centerX,
            int centerY,
            float scale
    ) {
        double halfW = (textRenderer.getWidth(sampleText) / 2.0 + 6.0) * scale;
        double halfH = (textRenderer.fontHeight / 2.0 + 4.0) * scale;
        return mouseX >= centerX - halfW && mouseX <= centerX + halfW
                && mouseY >= centerY - halfH && mouseY <= centerY + halfH;
    }
}
