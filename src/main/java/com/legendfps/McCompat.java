package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Every call that depends on the exact Minecraft 1.21.11 method layout for leaving a world lives here,
 * so a signature mismatch is a one-place fix.
 */
final class McCompat {
    private McCompat() {}

    static void leaveCurrentWorld(Minecraft mc) {
        if (mc.level == null) return;
        boolean local = mc.isLocalServer();
        mc.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
        if (local) mc.disconnectWithSavingScreen();
        else mc.disconnectWithProgressScreen();
    }
}
