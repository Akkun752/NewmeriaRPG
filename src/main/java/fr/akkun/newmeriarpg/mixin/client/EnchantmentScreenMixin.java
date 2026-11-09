package fr.akkun.newmeriarpg.mixin.client;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import fr.akkun.newmeriarpg.RpgAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * For Magic 2+ players, swaps the whole enchanting table background for a version with no lapis
 * slot drawn on it (the slot itself already stops accepting items at that point - see {@code
 * EnchantmentMenuMixin}/{@code LapisBypassSlot}). Below Magic 2, the real vanilla background (with
 * the slot) is left untouched, since those players still need to see where to place lapis.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin {
    /** 256x256, same as vanilla's own enchanting_table.png - expected at
     *  {@code assets/newmeriarpg/textures/gui/container/enchanting_table_no_lapis.png}. */
    private static final Identifier NO_LAPIS_BACKGROUND =
            Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "textures/gui/container/enchanting_table_no_lapis.png");

    @ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private Identifier newmeriarpg$conditionalBackground(Identifier original) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getData(RpgAttachments.RPG_DATA).magicLevel() >= 2) {
            return NO_LAPIS_BACKGROUND;
        }
        return original;
    }
}
