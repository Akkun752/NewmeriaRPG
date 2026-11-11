package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriacore.item.ModItems;
import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-clicking with the Spellbook (an item of NewmeriaCore) opens it. */
@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID, value = Dist.CLIENT)
public class SpellbookClientEvents {
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.SPELLBOOK.get())) {
            return;
        }
        Minecraft.getInstance().gui.setScreen(new SpellbookScreen());
    }
}
