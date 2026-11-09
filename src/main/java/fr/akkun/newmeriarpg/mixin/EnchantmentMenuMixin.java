package fr.akkun.newmeriarpg.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fr.akkun.newmeriarpg.LapisBypassSlot;
import fr.akkun.newmeriarpg.RpgAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Magic 2+ players get a permanently simulated, always-full stack of Lapis Lazuli: the real slot
 * stops accepting new items entirely ({@link LapisBypassSlot} - meant to be paired with a custom GUI
 * texture that no longer draws a slot there), while both the enchant-click currency check and the
 * displayed Lapis count behave exactly as if a permanent stack of 64 were sitting in it - the real
 * slot's actual contents are never read for either purpose once bypassed.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow
    @Final
    private Container enchantSlots;

    @Unique
    private Player newmeriarpg$player;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void newmeriarpg$capturePlayer(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.newmeriarpg$player = inventory.player;
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/EnchantmentMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot newmeriarpg$replaceLapisSlot(Slot original, @Local Inventory inventory) {
        return new LapisBypassSlot(this.enchantSlots, 1, 35, 47, inventory.player);
    }

    @Redirect(method = "clickMenuButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;", ordinal = 1))
    private ItemStack newmeriarpg$simulatedLapisForClick(Container enchantSlots, int slot) {
        if (this.newmeriarpg$player != null && this.newmeriarpg$player.getData(RpgAttachments.RPG_DATA).magicLevel() >= 2) {
            return new ItemStack(Items.LAPIS_LAZULI, 64);
        }
        return enchantSlots.getItem(slot);
    }

    @Inject(method = "getGoldCount", at = @At("HEAD"), cancellable = true)
    private void newmeriarpg$simulatedLapisForDisplay(CallbackInfoReturnable<Integer> cir) {
        if (this.newmeriarpg$player != null && this.newmeriarpg$player.getData(RpgAttachments.RPG_DATA).magicLevel() >= 2) {
            cir.setReturnValue(64);
        }
    }
}
