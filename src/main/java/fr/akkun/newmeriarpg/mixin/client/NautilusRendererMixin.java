package fr.akkun.newmeriarpg.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fr.akkun.newmeriarpg.client.CompanionTextures;
import net.minecraft.client.renderer.entity.NautilusRenderer;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NautilusRenderer.class)
public abstract class NautilusRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void newmeriarpg$captureCompanionTexture(AbstractNautilus entity, NautilusRenderState state, float partialTicks, CallbackInfo ci) {
        CompanionTextures.capture(entity, state);
    }

    @ModifyReturnValue(method = "getTextureLocation", at = @At("RETURN"))
    private Identifier newmeriarpg$companionTexture(Identifier original, NautilusRenderState state) {
        return CompanionTextures.resolve(state, original);
    }
}
