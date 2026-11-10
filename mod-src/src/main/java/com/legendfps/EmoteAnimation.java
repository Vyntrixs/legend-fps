package com.legendfps;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Client-side, local-player emote choreography applied to the actual player model. */
public final class EmoteAnimation {
    private static String current = "";
    private static long startedAt;
    private static final long DURATION_MS = 8000L;

    private EmoteAnimation() {}

    public static synchronized void play(String name) {
        if (name == null || name.isBlank()) return;
        current = name;
        startedAt = System.currentTimeMillis();
    }

    public static synchronized boolean isPlaying() {
        return !current.isBlank() && System.currentTimeMillis() - startedAt < DURATION_MS;
    }

    public static synchronized String current() {
        return isPlaying() ? current : "";
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        if (!isPlaying()) return;
        long elapsed = System.currentTimeMillis() - startedAt;
        float t = elapsed / 1000.0F;
        float fade = Math.min(1.0F, Math.min(t / 0.35F, (DURATION_MS / 1000.0F - t) / 0.45F));
        fade = Math.max(0.0F, fade);
        float wave = (float)Math.sin(t * 5.2F);
        float bounce = Math.abs((float)Math.sin(t * 4.0F));
        HumanoidModel<?> m = model;

        String name = current.toLowerCase(java.util.Locale.ROOT);
        int style = name.contains("legend") ? 1 : name.contains("special") ? 2 : 0;
        float side = (float)Math.sin(t * (style == 2 ? 7.0F : 5.0F));
        float pulse = (float)Math.sin(t * (style == 1 ? 3.0F : 4.5F));

        // Each style changes the full-body rhythm, not just a particle effect.
        m.body.yRot += fade * side * (style == 2 ? 0.24F : 0.16F);
        m.body.xRot += fade * (style == 1 ? -0.12F : 0.08F * pulse);
        m.head.yRot += fade * -side * 0.12F;
        m.head.xRot += fade * (float)Math.sin(t * 2.2F) * 0.08F;

        if (style == 0) {
            m.rightArm.xRot += fade * (-1.25F + 0.38F * wave);
            m.leftArm.xRot += fade * (-0.85F - 0.30F * wave);
            m.rightArm.zRot += fade * (0.30F + 0.18F * side);
            m.leftArm.zRot -= fade * (0.30F + 0.18F * side);
            m.rightLeg.xRot += fade * 0.28F * wave;
            m.leftLeg.xRot -= fade * 0.28F * wave;
        } else if (style == 1) {
            m.rightArm.xRot += fade * (-1.85F + 0.22F * wave);
            m.leftArm.xRot += fade * (-1.85F - 0.22F * wave);
            m.rightArm.zRot += fade * (0.42F + 0.12F * side);
            m.leftArm.zRot -= fade * (0.42F + 0.12F * side);
            m.rightLeg.xRot += fade * (0.48F * wave - 0.12F);
            m.leftLeg.xRot -= fade * (0.48F * wave - 0.12F);
            m.body.zRot += fade * side * 0.10F;
        } else {
            m.rightArm.xRot += fade * (-1.25F + 0.65F * (float)Math.sin(t * 8.0F));
            m.leftArm.xRot += fade * (-1.25F - 0.65F * (float)Math.sin(t * 8.0F));
            m.rightArm.zRot += fade * (0.62F + 0.16F * pulse);
            m.leftArm.zRot -= fade * (0.62F + 0.16F * pulse);
            m.rightLeg.xRot += fade * (0.38F * (float)Math.sin(t * 8.0F));
            m.leftLeg.xRot -= fade * (0.38F * (float)Math.sin(t * 8.0F));
            m.body.yRot += fade * 0.12F * (float)Math.sin(t * 8.0F);
        }

        float lift = fade * bounce * (style == 2 ? 0.16F : 0.10F);
        m.rightLeg.yRot += fade * 0.08F * side;
        m.leftLeg.yRot -= fade * 0.08F * side;
        m.rightArm.yRot += fade * 0.08F * side;
        m.leftArm.yRot -= fade * 0.08F * side;
        m.body.xRot -= lift * 0.22F;
    }
}
