package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Handles the bare "/rpg" command entirely client-side (same effect as the {@link RpgKeybinds#OPEN_STATS}
 * key). "/rpg add ..." isn't defined here, so it falls through and gets sent to the server, where
 * {@code RpgCommand} handles it.
 */
@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID, value = Dist.CLIENT)
public class RpgClientCommands {
    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rpg")
                .executes(ctx -> {
                    if (Minecraft.getInstance().gui.screen() == null) {
                        Minecraft.getInstance().gui.setScreen(new StatScreen());
                    }
                    return 1;
                }));
    }
}
