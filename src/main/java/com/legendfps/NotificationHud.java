package com.legendfps;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Essential-style bottom-right notifications for world invites and friend requests. */
public final class NotificationHud {
    private enum Kind { WORLD, FRIEND }
    private record Toast(Kind kind, String from, String id, String address, long createdAt) {}

    private static final int WIDTH = 224;
    private static final int HEIGHT = 68;
    private static final long LIFETIME_MS = 12_000L;
    private static final ConcurrentLinkedQueue<Toast> INCOMING = new ConcurrentLinkedQueue<>();
    private static final List<Toast> ACTIVE = new ArrayList<>();

    private NotificationHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("legendfps", "social_notifications"),
                NotificationHud::render
        );
    }

    public static void worldInvite(SocialService.Invite invite) {
        INCOMING.add(new Toast(Kind.WORLD, invite.from(), invite.id(), invite.address(), System.currentTimeMillis()));
    }

    public static void friendRequest(String from) {
        INCOMING.add(new Toast(Kind.FRIEND, from, "", "", System.currentTimeMillis()));
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Toast queued;
        while ((queued = INCOMING.poll()) != null) ACTIVE.add(0, queued);

        long now = System.currentTimeMillis();
        ACTIVE.removeIf(toast -> now - toast.createdAt() > LIFETIME_MS);
        int shown = Math.min(3, ACTIVE.size());
        int right = mc.getWindow().getGuiScaledWidth() - 8;
        int bottom = mc.getWindow().getGuiScaledHeight() - 8;

        for (int i = 0; i < shown; i++) {
            Toast toast = ACTIVE.get(i);
            int x = right - WIDTH;
            int y = bottom - (i + 1) * HEIGHT - i * 7;
            drawToast(graphics, mc, toast, x, y);
        }
    }

    private static void drawToast(GuiGraphics g, Minecraft mc, Toast toast, int x, int y) {
        // Layered dark panel, red accent and subtle outline.
        g.fill(x + 2, y + 2, x + WIDTH + 2, y + HEIGHT + 2, 0x88000000);
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xF20B0B10);
        g.fill(x, y, x + 3, y + HEIGHT, 0xFFE01B1B);
        g.fill(x + 3, y, x + WIDTH, y + 1, 0xFF70202A);
        g.fill(x + 3, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, 0xFF70202A);

        String heading = toast.kind() == Kind.WORLD ? "WORLD INVITE" : "FRIEND REQUEST";
        String detail = toast.kind() == Kind.WORLD
                ? toast.from() + " invited you to play"
                : toast.from() + " wants to be your friend";

        g.drawString(mc.font, heading, x + 11, y + 7, 0xFFFF5555, false);
        g.drawString(mc.font, detail, x + 11, y + 21, 0xFFFFFFFF, false);

        int buttonY = y + 43;
        int leftX = x + 10;
        int buttonW = 96;
        int rightX = x + 116;
        g.fill(leftX, buttonY, leftX + buttonW, buttonY + 18, 0xFF7F151D);
        g.fill(leftX, buttonY, leftX + buttonW, buttonY + 1, 0xFFFF5555);
        g.fill(rightX, buttonY, rightX + buttonW, buttonY + 18, 0xFF24242B);
        g.fill(rightX, buttonY, rightX + buttonW, buttonY + 1, 0xFF777780);

        String accept = toast.kind() == Kind.WORLD ? "JOIN" : "ACCEPT";
        g.drawCenteredString(mc.font, accept, leftX + buttonW / 2, buttonY + 5, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, "DECLINE", rightX + buttonW / 2, buttonY + 5, 0xFFDDDDDD);
    }

    /** Called from MouseHandler before vanilla input, so a toast remains clickable over open screens. */
    public static boolean handleMouse(Minecraft mc, int button, int action) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || action != GLFW.GLFW_PRESS || ACTIVE.isEmpty()) return false;

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();
        int right = mc.getWindow().getGuiScaledWidth() - 8;
        int bottom = mc.getWindow().getGuiScaledHeight() - 8;
        int shown = Math.min(3, ACTIVE.size());

        for (int i = 0; i < shown; i++) {
            Toast toast = ACTIVE.get(i);
            int x = right - WIDTH;
            int y = bottom - (i + 1) * HEIGHT - i * 7;
            if (mouseX < x || mouseX > x + WIDTH || mouseY < y || mouseY > y + HEIGHT) continue;

            int buttonY = y + 43;
            if (mouseY >= buttonY && mouseY <= buttonY + 18) {
                boolean accept = mouseX >= x + 10 && mouseX <= x + 106;
                boolean decline = mouseX >= x + 116 && mouseX <= x + 212;
                if (!accept && !decline) return true;

                if (toast.kind() == Kind.WORLD) {
                    SocialService.respondToInvite(toast.id(), accept);
                    if (accept) WorldInviteService.join(toast.address());
                } else {
                    if (accept) SocialService.acceptRequest(toast.from());
                    else SocialService.declineRequest(toast.from());
                }
                ACTIVE.remove(i);
                return true;
            }
            return true;
        }
        return false;
    }
}
