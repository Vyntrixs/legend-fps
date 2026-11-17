package com.legendfps.mixin;

import com.legendfps.NotificationHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes clicks on bottom-right LEGEND FPS toasts before vanilla screens receive them. */
@Mixin(MouseHandler.class)
public abstract class NotificationMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void legendfps$handleNotificationClick(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (NotificationHud.handleMouse(Minecraft.getInstance(), input.button(), action)) {
            ci.cancel();
        }
    }
}
