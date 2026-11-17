package com.legendfps;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Polished, Essential-inspired LEGEND FPS social notifications. */
public final class NotificationHud {
    private enum Kind { WORLD, FRIEND }
    private record Toast(Kind kind, String from, String id, String address, long createdAt) {}

    private static final int WIDTH = 268;
    private static final int HEIGHT = 84;
    private static final int LIFETIME_MS = 12_000;
    private static final int GAP = 8;
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
        ACTIVE.removeIf(toast -> now - toast.createdAt() >= LIFETIME_MS);

        int shown = Math.min(3, ACTIVE.size());
        int right = mc.getWindow().getGuiScaledWidth() - 12;
        int bottom = mc.getWindow().getGuiScaledHeight() - 12;
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getHeight();

        for (int i = 0; i < shown; i++) {
            Toast toast = ACTIVE.get(i);
            int x = right - WIDTH;
            int y = bottom - (i + 1) * HEIGHT - i * GAP;
            int age = (int) Math.max(0, now - toast.createdAt());
            int slide = age < 240 ? (int) Math.ceil((240 - age) * 0.075) : 0;
            drawToast(graphics, mc, toast, x + slide, y, mouseX, mouseY, age);
        }
    }

    private static void drawToast(GuiGraphics g, Minecraft mc, Toast toast, int x, int y,
                                  double mouseX, double mouseY, int age) {
        // Soft shadow, fine outline and a restrained LEGEND red accent.
        g.fill(x + 3, y + 4, x + WIDTH + 3, y + HEIGHT + 4, 0x66000000);
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xF20D1017);
        g.fill(x, y, x + WIDTH, y + 1, 0xFF45434D);
        g.fill(x, y + 1, x + 2, y + HEIGHT - 1, 0xFFE23A48);
        g.fill(x + 2, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, 0xFF34333C);
        g.fill(x + 2, y + 1, x + WIDTH - 1, y + 2, 0xFF292832);

        // Icon tile keeps both notification types visually consistent.
        int iconColor = toast.kind() == Kind.WORLD ? 0xFF64232D : 0xFF293B55;
        g.fill(x + 12, y + 12, x + 42, y + 42, 0xFF20212B);
        g.fill(x + 13, y + 13, x + 41, y + 41, iconColor);
        g.drawCenteredString(mc.font, toast.kind() == Kind.WORLD ? "W" : "F",
                x + 27, y + 23, 0xFFFFFFFF);

        String heading = toast.kind() == Kind.WORLD ? "WORLD INVITE" : "FRIEND REQUEST";
        String sender = toast.kind() == Kind.WORLD
                ? toast.from() + " invited you to play"
                : toast.from() + " sent you a friend request";
        String hint = toast.kind() == Kind.WORLD
                ? "Join their world and play together"
                : "Accept to add them to your friends";

        g.drawString(mc.font, heading, x + 52, y + 10, 0xFFFF6672, false);
        g.drawString(mc.font, fit(mc, sender, WIDTH - 64), x + 52, y + 23, 0xFFF5F5F7, false);
        g.drawString(mc.font, fit(mc, hint, WIDTH - 64), x + 52, y + 36, 0xFF9B9BA6, false);

        int buttonY = y + 55;
        int leftX = x + 12;
        int rightX = x + 140;
        int buttonWidth = 116;
        boolean overAccept = inside(mouseX, mouseY, leftX, buttonY, buttonWidth, 21);
        boolean overDecline = inside(mouseX, mouseY, rightX, buttonY, buttonWidth, 21);

        g.fill(leftX, buttonY, leftX + buttonWidth, buttonY + 21,
                overAccept ? 0xFFB52B39 : 0xFF84202B);
        g.fill(leftX, buttonY, leftX + buttonWidth, buttonY + 1, 0xFFFF6975);
        g.fill(rightX, buttonY, rightX + buttonWidth, buttonY + 21,
                overDecline ? 0xFF3B3B46 : 0xFF292A33);
        g.fill(rightX, buttonY, rightX + buttonWidth, buttonY + 1, 0xFF555560);

        String accept = toast.kind() == Kind.WORLD ? "JOIN WORLD" : "ACCEPT";
        g.drawCenteredString(mc.font, accept, leftX + buttonWidth / 2, buttonY + 7, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, "DECLINE", rightX + buttonWidth / 2, buttonY + 7, 0xFFE1E1E6);

        // A quiet lifetime indicator, matching the accent rather than flashing.
        float remaining = Math.max(0F, 1F - age / (float) LIFETIME_MS);
        int progressWidth = Math.round((WIDTH - 4) * remaining);
        if (progressWidth > 0) {
            g.fill(x + 2, y + HEIGHT - 2, x + 2 + progressWidth, y + HEIGHT - 1, 0xFFE34A57);
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private static String fit(Minecraft mc, String text, int maxWidth) {
        if (mc.font.width(text) <= maxWidth) return text;
        String shortened = text;
        while (!shortened.isEmpty() && mc.font.width(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened.isEmpty() ? "" : shortened + "...";
    }

    /** Runs before vanilla mouse handling, so notifications remain clickable over open screens. */
    public static boolean handleMouse(Minecraft mc, int button, int action) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || action != GLFW.GLFW_PRESS || ACTIVE.isEmpty()) return false;

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getHeight();
        int right = mc.getWindow().getGuiScaledWidth() - 12;
        int bottom = mc.getWindow().getGuiScaledHeight() - 12;
        int shown = Math.min(3, ACTIVE.size());

        for (int i = 0; i < shown; i++) {
            Toast toast = ACTIVE.get(i);
            int x = right - WIDTH;
            int y = bottom - (i + 1) * HEIGHT - i * GAP;
            int age = (int) Math.max(0, System.currentTimeMillis() - toast.createdAt());
            int slide = age < 240 ? (int) Math.ceil((240 - age) * 0.075) : 0;
            x += slide;

            if (!inside(mouseX, mouseY, x, y, WIDTH, HEIGHT)) continue;

            int buttonY = y + 55;
            boolean accept = inside(mouseX, mouseY, x + 12, buttonY, 116, 21);
            boolean decline = inside(mouseX, mouseY, x + 140, buttonY, 116, 21);
            if (!accept && !decline) return true;

            if (toast.kind() == Kind.WORLD) {
                SocialService.respondToInvite(toast.id(), accept);
                if (accept) WorldInviteService.join(toast.address());
            } else if (accept) {
                SocialService.acceptRequest(toast.from());
            } else {
                SocialService.declineRequest(toast.from());
            }
            ACTIVE.remove(i);
            return true;
        }
        return false;
    }
}
