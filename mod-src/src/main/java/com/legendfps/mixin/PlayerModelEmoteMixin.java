package com.legendfps.mixin;

import com.legendfps.EmoteAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelEmoteMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void legendfps$applyEmote(AvatarRenderState state, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || state == null || state.id != minecraft.player.getId()) return;
        EmoteAnimation.apply((PlayerModel)(Object)this, state);
    }
}
