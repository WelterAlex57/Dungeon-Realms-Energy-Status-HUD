package com.dungeonrealms.circularxp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a ring around the crosshair that mirrors the vanilla XP bar (which stays
 * visible at the bottom of the screen as normal). The ring has a small gap at the
 * bottom rather than being a full closed circle.
 *
 * Size, color, and on/off are all controlled by ModConfig (see the mod's Config
 * button on the mods list, or the in-game toggle keybind under Controls).
 */
@SideOnly(Side.CLIENT)
public class CircularXPRenderer {

    // ---- Appearance knobs -------------------------------------------------
    private static final float RADIUS_INNER_BASE = 12f;   // px from crosshair center, inner edge of ring
    private static final float RADIUS_OUTER_BASE = 16f;   // px from crosshair center, outer edge of ring
    private static final int   SEGMENTS          = 80;     // smoothness of the circle (higher = smoother)

    // Gap in the ring, in degrees, centered at the bottom (like the reference screenshot).
    // Set to 0 for a full unbroken circle.
    private static final float GAP_DEGREES = 40f;
    private static final float GAP_FRACTION = GAP_DEGREES / 360f;

    // Internal: where the drawable (non-gap) part of the ring starts/how far it sweeps,
    // expressed as a fraction of a full circle. 0.75 = bottom, using this class's angle convention.
    private static final float USABLE_START_T = 0.75f + GAP_FRACTION / 2f;
    private static final float USABLE_SWEEP_T = 1f - GAP_FRACTION;

    // Background (empty) ring color — fixed regardless of style, so it always reads as "empty"
    private static final float BG_R = 0.10f, BG_G = 0.10f, BG_B = 0.10f, BG_A = 0.55f;
    private static final float FG_A = 0.95f; // foreground alpha (color itself comes from ModConfig.colorStyle)

    // Foreground color when running low, so you get a visual warning mid-fight regardless of style
    private static final float LOW_R = 0.90f, LOW_G = 0.25f, LOW_B = 0.20f, LOW_A = 0.95f;
    private static final float LOW_THRESHOLD = 0.2f; // below this fraction, lerp toward LOW_* color
    // ------------------------------------------------------------------------

    /**
     * Handles the toggle keybind. isPressed() is queue-based (like vanilla), so
     * looping with while() catches multiple presses that happened in one tick.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (CircularXPMod.toggleKeyBinding == null) {
            return;
        }
        while (CircularXPMod.toggleKeyBinding.isPressed()) {
            ModConfig.enabled = !ModConfig.enabled;
            ModConfig.save();
        }
    }

    /**
     * Draw our ring right after the crosshair renders, so it sits centered on it.
     */
    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }

        if (!ModConfig.enabled) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return;
        }

        // Only draw in first person — a ring around a crosshair you can't see
        // in 3rd person doesn't make much sense.
        if (mc.gameSettings.thirdPersonView != 0) {
            return;
        }

        // Don't draw while a GUI (inventory, chat, etc.) is open.
        if (mc.currentScreen != null) {
            return;
        }

        float xpPercent = player.experience; // 0.0 - 1.0 within current level
        if (Float.isNaN(xpPercent)) {
            xpPercent = 0f;
        }
        xpPercent = MathHelperClamp(xpPercent, 0f, 1f);

        ScaledResolution sr = event.getResolution();
        float centerX = sr.getScaledWidth() / 2f;
        float centerY = sr.getScaledHeight() / 2f;

        drawXPRing(centerX, centerY, xpPercent);
    }

    private void drawXPRing(float cx, float cy, float percent) {
        float scale = ModConfig.scale;
        float radiusInner = RADIUS_INNER_BASE * scale;
        float radiusOuter = RADIUS_OUTER_BASE * scale;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);

        // Full background ring (always shown, so you can see "empty" too)
        drawRingArc(cx, cy, radiusInner, radiusOuter, 0f, 1f, BG_R, BG_G, BG_B, BG_A);

        if (percent > 0f) {
            ModConfig.ColorStyle style = ModConfig.colorStyle;
            float r, g, b, a;
            if (percent <= LOW_THRESHOLD) {
                // lerp between LOW_* and the chosen style color as it climbs out of the danger zone
                float t = percent / LOW_THRESHOLD;
                r = lerp(LOW_R, style.r, t);
                g = lerp(LOW_G, style.g, t);
                b = lerp(LOW_B, style.b, t);
                a = lerp(LOW_A, FG_A, t);
            } else {
                r = style.r; g = style.g; b = style.b; a = FG_A;
            }
            drawRingArc(cx, cy, radiusInner, radiusOuter, 0f, percent, r, g, b, a);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * Draws an annulus arc from startFrac to endFrac (0..1, where 0 = start of the
     * drawable ring just past the gap, 1 = end of the drawable ring just before the
     * gap on the other side) using a triangle strip between the inner and outer radius.
     */
    private void drawRingArc(float cx, float cy, float radiusInner, float radiusOuter,
                              float startFrac, float endFrac,
                              float r, float g, float b, float a) {
        if (endFrac <= startFrac) {
            return;
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int segs = Math.max(1, Math.round(SEGMENTS * (endFrac - startFrac)));

        for (int i = 0; i <= segs; i++) {
            float f = startFrac + (endFrac - startFrac) * (i / (float) segs);
            float t = USABLE_START_T + f * USABLE_SWEEP_T;
            float angle = (float) (-Math.PI / 2.0 + t * Math.PI * 2.0);
            float dx = (float) Math.sin(angle);
            float dy = (float) -Math.cos(angle);

            buf.pos(cx + dx * radiusOuter, cy + dy * radiusOuter, 0.0).color(r, g, b, a).endVertex();
            buf.pos(cx + dx * radiusInner, cy + dy * radiusInner, 0.0).color(r, g, b, a).endVertex();
        }

        tess.draw();
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float MathHelperClamp(float val, float min, float max) {
        return val < min ? min : (val > max ? max : val);
    }
}
