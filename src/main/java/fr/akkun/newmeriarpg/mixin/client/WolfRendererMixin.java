package fr.akkun.newmeriarpg.mixin.client;

import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.akkun.newmeriarpg.client.CompanionTextures;

/**
 * Wolf already exposes a mutable per-instance texture field on its render state (used for its
 * wet/angry variants), so a companion Wolf can just overwrite that field directly after vanilla
 * resolves its usual texture - no need for the capture/resolve helper the other 4 forms use.
 */
@Mixin(WolfRenderer.class)
public abstract class WolfRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void newmeriarpg$companionTexture(Wolf entity, WolfRenderState state, float partialTicks, CallbackInfo ci) {
        CompanionTextures.capture(entity, state);
        state.texture = CompanionTextures.resolve(state, state.texture);
    }
}
