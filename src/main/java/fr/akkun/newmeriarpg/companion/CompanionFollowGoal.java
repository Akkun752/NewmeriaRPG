package fr.akkun.newmeriarpg.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Generic "follow the owner, teleport over if left too far behind" goal - mirrors vanilla's own
 * {@code FollowOwnerGoal} (same follow/teleport distances and teleport-spot search), but works for
 * any companion form instead of requiring a {@link net.minecraft.world.entity.TamableAnimal} (the
 * Zombie and Iron Golem forms aren't). The owner always comes from the companion's own {@link
 * CompanionData} attachment, not each form's native ownership mechanism, so behaviour is identical
 * regardless of form.
 */
public class CompanionFollowGoal extends Goal {
    private static final float START_DISTANCE = 2.0F;
    private static final float STOP_DISTANCE = 1.0F;
    private static final double TELEPORT_DISTANCE_SQR = 144.0;

    private final Mob companion;
    private final double speedModifier;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public CompanionFollowGoal(Mob companion, double speedModifier) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.navigation = companion.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private LivingEntity findOwner() {
        UUID ownerId = companion.getExistingData(CompanionAttachments.COMPANION_DATA)
                .map(CompanionData::ownerId).orElse(null);
        return ownerId != null ? companion.level().getPlayerByUUID(ownerId) : null;
    }

    /**
     * A companion with a target is left alone to fight it: this goal claims movement at a higher
     * priority than some forms' own attack goal (the wolf's), which would otherwise never get to
     * run more than 2 blocks away from the owner. Past teleport range the owner still wins, so a
     * fight can't strand the companion far behind.
     */
    private boolean isBusyFighting(LivingEntity owner) {
        return companion.getTarget() != null && companion.distanceToSqr(owner) < TELEPORT_DISTANCE_SQR;
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = findOwner();
        if (owner == null || companion.isVehicle()) {
            return false;
        }
        if (isBusyFighting(owner)) {
            return false;
        }
        if (companion.distanceToSqr(owner) < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        this.owner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        if (isBusyFighting(owner)) {
            return false;
        }
        return companion.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        navigation.stop();
    }

    @Override
    public void tick() {
        boolean tooFar = companion.distanceToSqr(owner) >= TELEPORT_DISTANCE_SQR;
        if (!tooFar) {
            companion.getLookControl().setLookAt(owner, 10.0F, companion.getMaxHeadXRot());
        }
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            if (tooFar) {
                teleportToAroundOwner();
            } else {
                navigation.moveTo(owner, speedModifier);
            }
        }
    }

    private void teleportToAroundOwner() {
        BlockPos ownerPos = owner.blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            int xd = companion.getRandom().nextIntBetweenInclusive(-3, 3);
            int zd = companion.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(xd) >= 2 || Math.abs(zd) >= 2) {
                int yd = companion.getRandom().nextIntBetweenInclusive(-1, 1);
                if (maybeTeleportTo(ownerPos.getX() + xd, ownerPos.getY() + yd, ownerPos.getZ() + zd)) {
                    return;
                }
            }
        }
    }

    private boolean maybeTeleportTo(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!canTeleportTo(pos)) {
            return false;
        }
        companion.snapTo(x + 0.5, y, z + 0.5, companion.getYRot(), companion.getXRot());
        navigation.stop();
        return true;
    }

    private boolean canTeleportTo(BlockPos pos) {
        if (WalkNodeEvaluator.getPathTypeStatic(companion, pos) != PathType.WALKABLE) {
            return false;
        }
        BlockState below = companion.level().getBlockState(pos.below());
        if (below.getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos delta = pos.subtract(companion.blockPosition());
        return companion.level().noCollision(companion, companion.getBoundingBox().move(delta));
    }
}
