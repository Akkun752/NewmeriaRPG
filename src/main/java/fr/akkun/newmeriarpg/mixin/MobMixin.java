package fr.akkun.newmeriarpg.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fr.akkun.newmeriarpg.companion.CompanionAttachments;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ink Friend companions never catch fire from daylight, whatever their form (in practice only the
 * Zombie form is in the {@code burn_in_daylight} entity type tag). Other fire sources still apply.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
    @ModifyReturnValue(method = "isSunBurnTick", at = @At("RETURN"))
    private boolean newmeriarpg$companionNeverSunBurns(boolean original) {
        return original && ((Mob) (Object) this).getExistingData(CompanionAttachments.COMPANION_DATA).isEmpty();
    }
}
