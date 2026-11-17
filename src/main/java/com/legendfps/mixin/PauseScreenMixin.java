package com.legendfps.mixin;

import com.legendfps.LegendMenuScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void legendfps$addSocialButton(CallbackInfo ci) {
        int buttonWidth = 124;
        int x = Math.max(4, this.width - buttonWidth - 8);
        int y = Math.max(4, this.height - 30);
        this.addRenderableWidget(Button.builder(Component.literal("LEGEND FPS"), button ->
                this.minecraft.setScreen(new LegendMenuScreen(this)))
            .bounds(x, y, buttonWidth, 20)
            .build());
    }
}
