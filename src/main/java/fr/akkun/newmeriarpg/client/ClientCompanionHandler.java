package fr.akkun.newmeriarpg.client;

import net.minecraft.client.Minecraft;

/** Client-side reaction to {@code OpenCompanionMenuPayload}, kept out of the common network class
 *  so nothing there references client-only types like {@code Minecraft}/{@code Screen}. */
public class ClientCompanionHandler {
    public static void openMenu() {
        if (Minecraft.getInstance().gui.screen() == null) {
            Minecraft.getInstance().gui.setScreen(new CompanionScreen());
        }
    }
}
