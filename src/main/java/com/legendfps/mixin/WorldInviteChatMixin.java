package com.legendfps.mixin;

import com.legendfps.WorldInviteService;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * e4mc adds its assigned domain directly to the local chat HUD. That message does not pass
 * through Fabric's received-game-message event, so observe the actual HUD insertion instead.
 */
@Mixin(ChatComponent.class)
public abstract class WorldInviteChatMixin {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void legendfps$captureE4mcDomain(Component message, CallbackInfo ci) {
        WorldInviteService.observeGameMessage(message);
    }
}
