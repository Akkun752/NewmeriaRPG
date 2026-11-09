package fr.akkun.newmeriarpg.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Pushes the companion up toward the surface of whatever fluid it's standing in - water, lava, or
 * the mod's own oil - checked directly against the world's raw fluid state rather than the vanilla
 * {@code isInWater()}/{@code isInLava()} (which only ever recognize the water/lava tags, never a
 * modded fluid like oil). Not added to the Nautilus form, which is happy staying submerged.
 */
public class CompanionFloatGoal extends Goal {
    private final Mob mob;

    public CompanionFloatGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return !mob.level().getFluidState(BlockPos.containing(mob.getX(), mob.getY(), mob.getZ())).isEmpty()
                || !mob.level().getFluidState(BlockPos.containing(mob.getEyePosition())).isEmpty();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 movement = mob.getDeltaMovement();
        mob.setDeltaMovement(movement.x, Math.max(movement.y + 0.04, 0.08), movement.z);
    }
}
