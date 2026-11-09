package fr.akkun.newmeriarpg.companion;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * The 5 possible Ink Friend companion shapes. Each reuses the real vanilla entity type and model
 * unchanged, just with a special texture (see {@link #texture()}) - {@code CompanionManager} applies
 * the shared companion rules (health, ownership, targeting) on top, regardless of form.
 *
 * <p>Required special-texture pixel dimensions (must match the vanilla model's own UV layout):
 * WOLF 64x32, ZOMBIE 64x64, HORSE 64x64, IRON_GOLEM 128x128, NAUTILUS 128x128.
 */
public enum CompanionForm {
    WOLF(() -> EntityTypes.WOLF, "rpg.newmeriarpg.companion.wolf", 40.0F, true),
    ZOMBIE(() -> EntityTypes.ZOMBIE, "rpg.newmeriarpg.companion.zombie", 20.0F, true),
    HORSE(() -> EntityTypes.HORSE, "rpg.newmeriarpg.companion.horse", 30.0F, false),
    IRON_GOLEM(() -> EntityTypes.IRON_GOLEM, "rpg.newmeriarpg.companion.iron_golem", 100.0F, true),
    NAUTILUS(() -> EntityTypes.NAUTILUS, "rpg.newmeriarpg.companion.nautilus", 15.0F, false);

    private final Supplier<EntityType<?>> entityType;
    private final String translationKey;
    private final float vanillaMaxHealth;
    private final boolean fighter;
    private final Identifier icon;
    private final Identifier texture;

    /**
     * @param vanillaMaxHealth max health of the vanilla mob this form copies: a tamed wolf (40, a wild one
     *                         only has 8), and the best a horse can roll (15-30)
     * @param fighter          whether this form attacks hostile mobs on its own (the others have no attack)
     */
    CompanionForm(Supplier<EntityType<?>> entityType, String translationKey, float vanillaMaxHealth, boolean fighter) {
        this.entityType = entityType;
        this.translationKey = translationKey;
        this.vanillaMaxHealth = vanillaMaxHealth;
        this.fighter = fighter;
        String name = name().toLowerCase(Locale.ROOT);
        this.icon = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "textures/gui/companion/" + name + ".png");
        this.texture = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "textures/entity/companion/" + name + ".png");
    }

    public EntityType<?> entityType() {
        return entityType.get();
    }

    /** A companion always has 1.5x the health of the vanilla mob it is shaped after. */
    public float maxHealth() {
        return vanillaMaxHealth * 1.5F;
    }

    public boolean isFighter() {
        return fighter;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    /** 16x16 icon, expected at {@code assets/newmeriarpg/textures/gui/companion/<name>.png}. */
    public Identifier icon() {
        return icon;
    }

    /** Full mob skin reusing the vanilla model's UV layout - see the class javadoc for the required
     *  pixel dimensions per form. Expected at {@code assets/newmeriarpg/textures/entity/companion/<name>.png}. */
    public Identifier texture() {
        return texture;
    }
}
