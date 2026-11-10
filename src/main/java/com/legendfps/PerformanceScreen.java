package com.legendfps;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PerformanceScreen extends RedScreen {
    private Button particleButton;
    private Button cloudButton;

    public PerformanceScreen(Screen parent) {
        super("Performance", parent);
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = Math.max(44, this.height / 2 - 78);

        add("Preset: Balanced", x, y, 200, b -> applyPreset(0));
        y += 24;
        add("Preset: Performance", x, y, 200, b -> applyPreset(1));
        y += 24;
        add("Preset: Max Performance", x, y, 200, b -> applyPreset(2));
        y += 30;

        particleButton = add(particleLabel(), x, y, 200, b -> {
            Options o = Minecraft.getInstance().options;
            ParticleStatus[] all = ParticleStatus.values();
            o.particles().set(all[(o.particles().get().ordinal() + 1) % all.length]);
            o.save();
            b.setMessage(Component.literal(particleLabel()));
        });
        y += 24;

        cloudButton = add(cloudLabel(), x, y, 200, b -> {
            Options o = Minecraft.getInstance().options;
            o.cloudStatus().set(o.cloudStatus().get() == CloudStatus.OFF ? CloudStatus.FAST : CloudStatus.OFF);
            o.save();
            b.setMessage(Component.literal(cloudLabel()));
        });
        y += 30;

        add("Back", x, y, 200, b -> onClose());
    }

    private void applyPreset(int level) {
        Options o = Minecraft.getInstance().options;
        switch (level) {
            case 0 -> {
                o.renderDistance().set(12);
                o.simulationDistance().set(10);
                o.particles().set(ParticleStatus.DECREASED);
                o.cloudStatus().set(CloudStatus.FAST);
                o.entityDistanceScaling().set(1.0);
                o.biomeBlendRadius().set(2);
            }
            case 1 -> {
                o.renderDistance().set(8);
                o.simulationDistance().set(6);
                o.particles().set(ParticleStatus.DECREASED);
                o.cloudStatus().set(CloudStatus.OFF);
                o.entityDistanceScaling().set(0.75);
                o.biomeBlendRadius().set(1);
            }
            default -> {
                o.renderDistance().set(6);
                o.simulationDistance().set(5);
                o.particles().set(ParticleStatus.MINIMAL);
                o.cloudStatus().set(CloudStatus.OFF);
                o.entityDistanceScaling().set(0.5);
                o.biomeBlendRadius().set(0);
            }
        }
        o.save();
        particleButton.setMessage(Component.literal(particleLabel()));
        cloudButton.setMessage(Component.literal(cloudLabel()));
    }

    private static String particleLabel() {
        return "Particles: " + Minecraft.getInstance().options.particles().get().name();
    }

    private static String cloudLabel() {
        return "Clouds: " + Minecraft.getInstance().options.cloudStatus().get().name();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        Options o = Minecraft.getInstance().options;
        g.drawCenteredString(this.font,
                Component.literal("Render " + o.renderDistance().get()
                        + "  |  Simulation " + o.simulationDistance().get()
                        + "  |  Entity scale " + o.entityDistanceScaling().get()),
                this.width / 2, 26, GRAY);
    }
}
