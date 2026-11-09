package fr.akkun.newmeriarpg.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fr.akkun.newmeriarpg.client.CompanionTextures;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseRenderer.class)
public abstract class HorseRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void newmeriarpg$captureCompanionTexture(Horse entity, HorseRenderState state, float partialTicks, CallbackInfo ci) {
        CompanionTextures.capture(entity, state);
    }

    @ModifyReturnValue(method = "getTextureLocation", at = @At("RETURN"))
    private Identifier newmeriarpg$companionTexture(Identifier original, HorseRenderState state) {
        return CompanionTextures.resolve(state, original);
    }
}
