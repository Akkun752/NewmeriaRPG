package fr.akkun.newmeriarpg;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Order matters: this is both the unlock order shown in the Magic menu (by required Magic level)
 *  and the slice order in the spell wheel (see {@code MagicWheelScreen}) - adding a spell here (with
 *  a matching 16x16 icon, see {@link #icon()}) automatically adds it as a wheel slice, no other
 *  wiring needed. Cooldown is not per-spell anymore - see {@link SpellCasting} for the general
 *  Magic-level cooldown. */
public enum RpgSpell {
    FRIENDSHIP(1, "rpg.newmeriarpg.spell.friendship"),
    LIGHTNING(2, "rpg.newmeriarpg.spell.lightning"),
    FIREBALL(3, "rpg.newmeriarpg.spell.fireball"),
    INK_FRIEND(4, "rpg.newmeriarpg.spell.ink_friend"),
    TELEPORT(5, "rpg.newmeriarpg.spell.teleport");

    private final int requiredMagicLevel;
    private final String translationKey;
    private final Identifier icon;

    RpgSpell(int requiredMagicLevel, String translationKey) {
        this.requiredMagicLevel = requiredMagicLevel;
        this.translationKey = translationKey;
        this.icon = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID,
                "textures/gui/spell/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public int requiredMagicLevel() {
        return requiredMagicLevel;
    }

    public boolean isUnlocked(int magicLevel) {
        return magicLevel >= requiredMagicLevel;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    /** 16x16 icon, expected at {@code assets/newmeriarpg/textures/gui/spell/<name>.png}. */
    public Identifier icon() {
        return icon;
    }
}
