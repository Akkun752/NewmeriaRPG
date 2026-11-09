package fr.akkun.newmeriarpg;

import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Recomputes and applies the {@link AttributeModifier}s driven by RPG stats.
 * Called on login, on respawn, and right after a stat point is spent.
 */
public class RpgAttributeModifiers {
    private static final Identifier FORCE_ATTACK_DAMAGE = Identifier.fromNamespaceAndPath(NewmeriaRPG.LEGACY_NAMESPACE, "rpg_force_attack_damage");
    private static final Identifier RESISTANCE_MAX_HEALTH = Identifier.fromNamespaceAndPath(NewmeriaRPG.LEGACY_NAMESPACE, "rpg_resistance_max_health");
    private static final Identifier SPEED_MOVEMENT = Identifier.fromNamespaceAndPath(NewmeriaRPG.LEGACY_NAMESPACE, "rpg_speed_movement");
    private static final Identifier SPEED_WATER_EFFICIENCY = Identifier.fromNamespaceAndPath(NewmeriaRPG.LEGACY_NAMESPACE, "rpg_speed_water_efficiency");

    public static void apply(ServerPlayer player, RpgData data) {
        applyModifier(player, Attributes.ATTACK_DAMAGE, FORCE_ATTACK_DAMAGE,
                data.forceLevel(), AttributeModifier.Operation.ADD_VALUE);

        applyModifier(player, Attributes.MAX_HEALTH, RESISTANCE_MAX_HEALTH,
                data.resistanceLevel() * 4.0, AttributeModifier.Operation.ADD_VALUE);

        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOVEMENT,
                data.speedLevel() * 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        applyModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, SPEED_WATER_EFFICIENCY,
                waterEfficiencyBonus(data.speedLevel()), AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * There is no literal "swim speed" attribute in this version; WATER_MOVEMENT_EFFICIENCY
     * (0..1, same lever as Depth Strider) is the closest analog to reduce water drag.
     */
    private static double waterEfficiencyBonus(int speedLevel) {
        if (speedLevel >= 4) {
            return 0.65;
        }
        if (speedLevel >= 2) {
            return 0.35;
        }
        return 0.0;
    }

    public static double miningSpeedMultiplier(int forceLevel) {
        if (forceLevel >= 5) {
            return 1.5;
        }
        if (forceLevel >= 4) {
            return 1.4;
        }
        if (forceLevel >= 3) {
            return 1.3;
        }
        if (forceLevel >= 2) {
            return 1.2;
        }
        if (forceLevel >= 1) {
            return 1.1;
        }
        return 1.0;
    }

    public static double xpMultiplier(int magicLevel) {
        if (magicLevel >= 5) {
            return 2.0;
        }
        if (magicLevel >= 3) {
            return 1.5;
        }
        return 1.0;
    }

    private static void applyModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id,
                                       double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0) {
            instance.removeModifier(id);
        } else {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
