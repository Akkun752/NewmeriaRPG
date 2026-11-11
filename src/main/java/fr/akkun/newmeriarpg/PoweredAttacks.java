package fr.akkun.newmeriarpg;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lightning bolts and fireballs three times as strong as vanilla's: what the Lightning and Fireball
 * spells cast (see {@link SpellCasting}), and what the bosses of NewmeriaBosses (Saphira) use too.
 */
public final class PoweredAttacks {
    // Damage/blast size multiplier over vanilla lightning/ghast-fireball defaults.
    public static final float POWER_MULTIPLIER = 3.0F;

    private PoweredAttacks() {
    }

    /** Vanilla lightning with its damage tripled, anywhere. {@code cause} is the player behind it, if any. */
    public static void strikeLightning(ServerLevel level, Vec3 target, @Nullable ServerPlayer cause) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }
        bolt.setPos(target.x, target.y, target.z);
        bolt.setCause(cause);
        bolt.setDamage(bolt.getDamage() * POWER_MULTIPLIER);
        level.addFreshEntity(bolt);
    }

    /** A ghast fireball with tripled blast and damage, launched from anywhere. */
    public static void launchFireball(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 direction) {
        // Vanilla ghast fireballs: explosion power 1, 6.0F direct-hit damage.
        int explosionPower = Math.round(1 * POWER_MULTIPLIER);
        float directHitDamage = 6.0F * POWER_MULTIPLIER;
        PoweredLargeFireball fireball = new PoweredLargeFireball(level, owner, direction, explosionPower, directHitDamage);
        fireball.setPos(from.x, from.y, from.z);
        level.addFreshEntity(fireball);
    }
}
