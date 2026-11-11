package fr.akkun.newmeriarpg;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Every spell of the game. None of them is tied to a Magic level any more: each Magic level gives a
 * spell slot (see {@link SpellBook}), and players pick which spells fill them in the Spellbook. The order
 * here is the order of the Spellbook's list.
 *
 * <p>Adding a spell: an entry here (with its 16x16 icon, see {@link #icon()}, and its name in the language
 * files), plus what it does in {@link SpellCasting#cast}.
 *
 * <p>A spell can be restricted to some players: give their exact login usernames (not display names) as
 * last argument. Everyone else neither sees it in the Spellbook nor can learn or cast it. No usernames =
 * open to everyone.
 */
public enum RpgSpell {
    FRIENDSHIP("rpg.newmeriarpg.spell.friendship"),
    LIGHTNING("rpg.newmeriarpg.spell.lightning"),
    FIREBALL("rpg.newmeriarpg.spell.fireball"),
    // "Dev" is the local singleplayer username, for testing.
    INK_FRIEND("rpg.newmeriarpg.spell.ink_friend", "RaphaAile", "Dev"),
    TELEPORT("rpg.newmeriarpg.spell.teleport"),
    HEALING("rpg.newmeriarpg.spell.healing");

    private final String translationKey;
    private final Set<String> allowedPlayers;
    private final Identifier icon;

    RpgSpell(String translationKey, String... allowedPlayers) {
        this.translationKey = translationKey;
        this.allowedPlayers = Set.of(allowedPlayers);
        this.icon = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "textures/gui/spell/" + this.id() + ".png");
    }

    /** What saves and the network know the spell by: never change it once players have learned the spell. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<RpgSpell> byId(String id) {
        return Arrays.stream(values()).filter(spell -> spell.id().equals(id)).findFirst();
    }

    /** @return whether that player may see, learn and cast this spell */
    public boolean isAvailableTo(Player player) {
        return allowedPlayers.isEmpty() || allowedPlayers.contains(player.getGameProfile().name());
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    /** 16x16 icon, expected at {@code assets/newmeriarpg/textures/gui/spell/<id>.png}. */
    public Identifier icon() {
        return icon;
    }
}
