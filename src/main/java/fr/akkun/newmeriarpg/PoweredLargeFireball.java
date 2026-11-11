package fr.akkun.newmeriarpg;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Same as vanilla's LargeFireball, except both the explosion power and the direct-hit damage are
 * configurable - vanilla hardcodes the direct-hit damage to 6.0F with no override hook, so this
 * extends {@link Fireball} directly (which, unlike LargeFireball, doesn't touch onHit/onHitEntity)
 * instead of subclassing LargeFireball itself.
 */
public class PoweredLargeFireball extends Fireball {
    private final int explosionPower;
    private final float directHitDamage;

    public PoweredLargeFireball(Level level, LivingEntity mob, Vec3 direction, int explosionPower, float directHitDamage) {
        super(EntityTypes.FIREBALL, mob, direction, level);
        this.explosionPower = explosionPower;
        this.directHitDamage = directHitDamage;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            boolean grief = net.neoforged.neoforge.event.EventHooks.canEntityGrief(serverLevel, this.getOwner());
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionPower, grief, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity hitEntity = hitResult.getEntity();
            Entity owner = this.getOwner();
            DamageSource damageSource = this.damageSources().fireball(this, owner);
            hitEntity.hurtServer(serverLevel, damageSource, this.directHitDamage);
            EnchantmentHelper.doPostAttackEffects(serverLevel, hitEntity, damageSource);
        }
    }
}
