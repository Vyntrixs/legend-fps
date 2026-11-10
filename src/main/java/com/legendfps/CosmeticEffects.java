package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Particle-based cosmetics, shown on the local player only. */
public class CosmeticEffects {

    public static void tick(Minecraft mc) {
        if (!Config.data.effects) return;
        LocalPlayer p = mc.player;
        ClientLevel level = mc.level;
        if (p == null || level == null || p.isSpectator() || p.isInvisible()) return;

        int t = p.tickCount;

        Cosmetic wing = Cosmetic.equipped(Cosmetic.Category.WING);
        if (wing != null && t % 2 == 0) wings(level, p, wing, t);

        Cosmetic halo = Cosmetic.equipped(Cosmetic.Category.HALO);
        if (halo != null) halo(level, p, halo, t);

        Cosmetic aura = Cosmetic.equipped(Cosmetic.Category.AURA);
        if (aura != null) aura(level, p, aura, t);

        Cosmetic trail = Cosmetic.equipped(Cosmetic.Category.TRAIL);
        if (trail != null) trail(level, p, trail);
    }

    private static void wings(ClientLevel level, LocalPlayer p, Cosmetic c, int t) {
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double lx = Math.cos(yaw), lz = Math.sin(yaw);
        double flap = Math.sin(t * 0.18) * 0.35;
        double baseY = p.getY() + (p.isCrouching() ? 1.1 : 1.3);

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i <= 6; i++) {
                double u = i / 6.0;
                double lat = side * (0.2 + u * 0.95);
                double up = 0.25 - u * 0.55 + Math.sin(u * Math.PI) * 0.35;
                double back = 0.28 + u * 0.25 + flap * u;
                double px = p.getX() + lx * lat - fx * back;
                double py = baseY + up + flap * 0.3 * u;
                double pz = p.getZ() + lz * lat - fz * back;
                level.addParticle(c.particle(), px, py, pz, 0, 0, 0);
            }
        }
    }

    private static void halo(ClientLevel level, LocalPlayer p, Cosmetic c, int t) {
        double angle = t * 0.35;
        double y = p.getY() + p.getBbHeight() + 0.25;
        for (int k = 0; k < 2; k++) {
            double a = angle + k * Math.PI;
            level.addParticle(c.particle(),
                    p.getX() + Math.cos(a) * 0.32, y, p.getZ() + Math.sin(a) * 0.32, 0, 0, 0);
        }
    }

    private static void aura(ClientLevel level, LocalPlayer p, Cosmetic c, int t) {
        for (int k = 0; k < 2; k++) {
            double a = t * 0.3 + k * Math.PI;
            double h = ((t + k * 15) % 30) / 30.0 * p.getBbHeight();
            level.addParticle(c.particle(),
                    p.getX() + Math.cos(a) * 0.55, p.getY() + h, p.getZ() + Math.sin(a) * 0.55,
                    0, 0.02, 0);
        }
    }

    private static void trail(ClientLevel level, LocalPlayer p, Cosmetic c) {
        Vec3 d = p.getDeltaMovement();
        if (d.x * d.x + d.z * d.z < 0.0004) return;
        double rx = (p.getRandom().nextDouble() - 0.5) * 0.4;
        double rz = (p.getRandom().nextDouble() - 0.5) * 0.4;
        level.addParticle(c.particle(), p.getX() + rx, p.getY() + 0.1, p.getZ() + rz, 0, 0.01, 0);
    }
}
