package fr.akkun.newmeriarpg;

import fr.akkun.newmeriacore.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Set;

/**
 * Each season totem doubles RPG points gained by its own owner while held in either hand - but only
 * for that specific player, identified by exact login username (not display name).
 */
public class TotemBonuses {
    // Only ever built well after mod registration finishes, so eagerly calling .get() here is safe.
    private static final Map<Item, Set<String>> OWNERS = Map.of(
            ModItems.AKKUN_S1_TOTEM.get(), Set.of("Akkun_7", "Dev"), // "Dev" is the local singleplayer username, for debugging.
            ModItems.FALNIX_S1_TOTEM.get(), Set.of("Falnix"),
            ModItems.BATS_S1_TOTEM.get(), Set.of("bats01"),
            ModItems.RAPHAAILE_S1_TOTEM.get(), Set.of("RaphaAile"),
            ModItems.WOOHTYTI_S1_TOTEM.get(), Set.of("woohtyti")
    );

    public static boolean hasMatchingTotem(ServerPlayer player) {
        String username = player.getGameProfile().name();
        return matches(player.getMainHandItem().getItem(), username) || matches(player.getOffhandItem().getItem(), username);
    }

    private static boolean matches(Item item, String username) {
        Set<String> owners = OWNERS.get(item);
        return owners != null && owners.contains(username);
    }
}
