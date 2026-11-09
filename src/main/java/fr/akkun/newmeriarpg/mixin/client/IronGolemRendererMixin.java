package fr.akkun.newmeriarpg.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fr.akkun.newmeriarpg.client.CompanionTextures;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolemRenderer.class)
public abstract class IronGolemRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void newmeriarpg$captureCompanionTexture(IronGolem entity, IronGolemRenderState state, float partialTicks, CallbackInfo ci) {
        CompanionTextures.capture(entity, state);
    }

    @ModifyReturnValue(method = "getTextureLocation", at = @At("RETURN"))
    private Identifier newmeriarpg$companionTexture(Identifier original, IronGolemRenderState state) {
        return CompanionTextures.resolve(state, original);
    }
}
