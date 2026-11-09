package fr.akkun.newmeriarpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID, value = Dist.CLIENT)
public class RpgKeybinds {
    // Still named after newmeriacore, where these keys were first added: the game saves custom bindings
    // under these names, so renaming them would reset the keys players have chosen.
    public static final KeyMapping OPEN_STATS = new KeyMapping("key.newmeriacore.open_stats",
            InputConstants.KEY_K, KeyMapping.Category.MISC);
    // Spells are now cast by right-clicking a Wand; M only opens the spell selection menu.
    // Key codes are physical (US/QWERTY) positions - on an AZERTY keyboard the letter "M" is
    // physically where QWERTY has ";", so KEY_SEMICOLON is the key that actually shows "M".
    public static final KeyMapping OPEN_MAGIC = new KeyMapping("key.newmeriacore.open_magic",
            InputConstants.KEY_SEMICOLON, KeyMapping.Category.MISC);

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS);
        event.register(OPEN_MAGIC);
    }

    // Tracked manually (rather than via consumeClick(), which is edge-triggered for taps) so the
    // wheel can open on press and close-and-confirm on release, i.e. a "hold" gesture.
    private static boolean magicKeyWasDown = false;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_STATS.consumeClick()) {
            if (Minecraft.getInstance().gui.screen() == null) {
                Minecraft.getInstance().gui.setScreen(new StatScreen());
            }
        }

        boolean isDown = isMagicKeyDown();
        if (isDown && !magicKeyWasDown) {
            if (Minecraft.getInstance().gui.screen() == null) {
                Minecraft.getInstance().gui.setScreen(new MagicWheelScreen());
            }
        } else if (!isDown && magicKeyWasDown) {
            if (Minecraft.getInstance().gui.screen() instanceof MagicWheelScreen wheel) {
                wheel.closeAndConfirm();
            }
        }
        magicKeyWasDown = isDown;
    }

    /**
     * Polls the raw hardware key state instead of {@code OPEN_MAGIC.isDown()}: opening the wheel
     * calls {@code Gui#setScreen}, which always calls {@code KeyMapping.releaseAll()} - that would
     * make {@code isDown()} report false on the very next tick even while the key is still
     * physically held, which caused the wheel to open and immediately close in a loop.
     */
    private static boolean isMagicKeyDown() {
        InputConstants.Key key = OPEN_MAGIC.getKey();
        if (key.getType() != InputConstants.Type.KEYBOARD) {
            return OPEN_MAGIC.isDown();
        }
        return InputConstants.isKeyDown(key.getValue());
    }
}
